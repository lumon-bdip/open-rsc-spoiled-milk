package com.openrsc.server.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Verified two-copy materialization for an authoritative terrain draft. */
public final class WorldEditorTerrainSaveFiles {
	private static final int REGION_SIZE=48,RECORD_SIZE=10,SECTOR_BYTES=REGION_SIZE*REGION_SIZE*RECORD_SIZE;
	private static final DateTimeFormatter BACKUP_TIME=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
	private WorldEditorTerrainSaveFiles(){}

	public static SaveResult save(Path serverArchive,Path clientArchive,Path backupDirectory,String expectedBaseSha256,
		Collection<TileRecord> requestedTiles) throws IOException {
		if(serverArchive==null||clientArchive==null||backupDirectory==null)throw new IllegalArgumentException("Terrain save paths are required.");
		serverArchive=serverArchive.toAbsolutePath().normalize();clientArchive=clientArchive.toAbsolutePath().normalize();
		if(serverArchive.equals(clientArchive))throw new IllegalArgumentException("Server and client terrain archives must be separate files.");
		if(!Files.isRegularFile(serverArchive)||!Files.isRegularFile(clientArchive))throw new IOException("Both server and client landscape archives must exist.");
		List<TileRecord> tiles=normalizeTiles(requestedTiles);if(tiles.isEmpty())throw new IllegalArgumentException("Terrain draft is empty.");
		String serverBase=sha256(serverArchive),clientBase=sha256(clientArchive);
		if(expectedBaseSha256==null||!serverBase.equals(expectedBaseSha256))throw new IOException("Server landscape changed after the terrain draft was opened.");
		if(!serverBase.equals(clientBase))throw new IOException("Server and client landscape archives are not byte-identical before save.");
		Path serverTemp=Files.createTempFile(serverArchive.getParent(),".world-editor-terrain-",".orsc.tmp");
		Path clientTemp=Files.createTempFile(clientArchive.getParent(),".world-editor-terrain-",".orsc.tmp");Path backup=null;
		try{
			materialize(serverArchive,serverTemp,tiles);verifyMaterialization(serverArchive,serverTemp,tiles);
			Files.copy(serverTemp,clientTemp,StandardCopyOption.REPLACE_EXISTING);String resultSha=sha256(serverTemp);
			if(!resultSha.equals(sha256(clientTemp)))throw new IOException("Prepared server and client landscape hashes differ.");
			Files.createDirectories(backupDirectory);backup=uniqueBackupPath(backupDirectory,serverBase);
			Files.copy(serverArchive,backup,StandardCopyOption.COPY_ATTRIBUTES);
			boolean serverReplaced=false,clientReplaced=false;
			try{
				moveReplace(serverTemp,serverArchive);serverReplaced=true;moveReplace(clientTemp,clientArchive);clientReplaced=true;
				if(!resultSha.equals(sha256(serverArchive))||!resultSha.equals(sha256(clientArchive)))throw new IOException("Saved landscape hash verification failed.");
			}catch(Exception failure){
				IOException rollbackFailure=null;
				try{if(serverReplaced)restore(backup,serverArchive);}catch(IOException e){rollbackFailure=e;}
				try{if(clientReplaced||serverReplaced)restore(backup,clientArchive);}catch(IOException e){if(rollbackFailure==null)rollbackFailure=e;else rollbackFailure.addSuppressed(e);}
				IOException wrapped=failure instanceof IOException?(IOException)failure:new IOException(failure);
				if(rollbackFailure!=null)wrapped.addSuppressed(rollbackFailure);throw wrapped;
			}
			Set<String> sectors=new HashSet<String>();for(TileRecord tile:tiles)sectors.add(tile.sectorName());
			return new SaveResult(tiles.size(),sectors.size(),serverBase,resultSha,serverArchive,clientArchive,backup);
		}finally{Files.deleteIfExists(serverTemp);Files.deleteIfExists(clientTemp);}
	}

	public static String sha256(Path path) throws IOException {
		try{MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];
			try(InputStream in=Files.newInputStream(path)){int count;while((count=in.read(buffer))>=0)if(count>0)digest.update(buffer,0,count);}
			StringBuilder hex=new StringBuilder(64);for(byte value:digest.digest())hex.append(String.format("%02x",value&0xff));return hex.toString();
		}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
	}

	private static List<TileRecord> normalizeTiles(Collection<TileRecord> requested){
		if(requested==null)return Collections.emptyList();ArrayList<TileRecord> tiles=new ArrayList<TileRecord>(requested);Set<String> keys=new HashSet<String>();
		for(TileRecord tile:tiles){if(tile==null||!keys.add(tile.key()))throw new IllegalArgumentException("Terrain draft contains a null or duplicate tile.");}
		Collections.sort(tiles,Comparator.comparingInt((TileRecord t)->t.plane).thenComparingInt(t->t.worldX).thenComparingInt(t->t.worldY));return tiles;
	}

	private static void materialize(Path source,Path output,List<TileRecord> tiles) throws IOException {
		Map<String,List<TileRecord>> bySector=bySector(tiles);Set<String> found=new HashSet<String>();
		try(ZipFile input=new ZipFile(source.toFile());OutputStream raw=Files.newOutputStream(output);ZipOutputStream zip=new ZipOutputStream(raw)){
			if(input.getComment()!=null)zip.setComment(input.getComment());Enumeration<? extends ZipEntry> entries=input.entries();
			while(entries.hasMoreElements()){
				ZipEntry entry=entries.nextElement();byte[] data=readEntry(input,entry);List<TileRecord> patches=bySector.get(entry.getName());
				if(patches!=null){if(data.length!=SECTOR_BYTES)throw new IOException("Invalid terrain sector size: "+entry.getName());for(TileRecord tile:patches)tile.writeTo(data);found.add(entry.getName());}
				ZipEntry replacement=copyEntryMetadata(entry);zip.putNextEntry(replacement);if(!entry.isDirectory())zip.write(data);zip.closeEntry();
			}
		}
		if(!found.equals(bySector.keySet())){Set<String> missing=new HashSet<String>(bySector.keySet());missing.removeAll(found);throw new IOException("Landscape sectors not found: "+missing);}
	}

	private static void verifyMaterialization(Path source,Path output,List<TileRecord> tiles) throws IOException {
		Map<String,List<TileRecord>> bySector=bySector(tiles);
		try(ZipFile before=new ZipFile(source.toFile());ZipFile after=new ZipFile(output.toFile())){
			Enumeration<? extends ZipEntry> entries=before.entries();int count=0;
			while(entries.hasMoreElements()){
				ZipEntry entry=entries.nextElement(),saved=after.getEntry(entry.getName());if(saved==null)throw new IOException("Saved landscape omitted entry "+entry.getName());
				byte[] expected=readEntry(before,entry);List<TileRecord> patches=bySector.get(entry.getName());if(patches!=null)for(TileRecord tile:patches)tile.writeTo(expected);
				if(!Arrays.equals(expected,readEntry(after,saved)))throw new IOException("Saved landscape entry verification failed: "+entry.getName());count++;
			}
			if(count!=after.size())throw new IOException("Saved landscape entry count changed.");
		}
	}

	private static Map<String,List<TileRecord>> bySector(List<TileRecord> tiles){LinkedHashMap<String,List<TileRecord>> map=new LinkedHashMap<String,List<TileRecord>>();
		for(TileRecord tile:tiles){List<TileRecord> sector=map.get(tile.sectorName());if(sector==null){sector=new ArrayList<TileRecord>();map.put(tile.sectorName(),sector);}sector.add(tile);}return map;}
	private static byte[] readEntry(ZipFile zip,ZipEntry entry) throws IOException {try(InputStream in=zip.getInputStream(entry);ByteArrayOutputStream out=new ByteArrayOutputStream((int)Math.max(0,entry.getSize()))){byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)if(n>0)out.write(b,0,n);return out.toByteArray();}}
	private static ZipEntry copyEntryMetadata(ZipEntry source){ZipEntry copy=new ZipEntry(source.getName());copy.setComment(source.getComment());copy.setTime(source.getTime());if(source.getExtra()!=null)copy.setExtra(source.getExtra());return copy;}
	private static Path uniqueBackupPath(Path directory,String hash) throws IOException {String stem="Custom_Landscape-"+BACKUP_TIME.format(Instant.now())+"-"+hash.substring(0,12);Path path=directory.resolve(stem+".orsc");int suffix=1;while(Files.exists(path))path=directory.resolve(stem+"-"+(suffix++)+".orsc");return path;}
	private static void moveReplace(Path from,Path to) throws IOException {try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}}
	private static void restore(Path backup,Path destination) throws IOException {Path temp=Files.createTempFile(destination.getParent(),".world-editor-rollback-",".tmp");try{Files.copy(backup,temp,StandardCopyOption.REPLACE_EXISTING);moveReplace(temp,destination);}finally{Files.deleteIfExists(temp);}}

	public static final class TileRecord {
		public final int worldX,worldY,plane,elevation,groundTexture,groundOverlay,roofTexture,horizontalWall,verticalWall,diagonal;
		private final int sectorX,sectorY,localX,localY;
		private TileRecord(int x,int y,int p,int e,int texture,int overlay,int roof,int horizontal,int vertical,int diagonal){
			if(p<0||p>3||Math.floorDiv(y,944)!=p)throw new IllegalArgumentException("Terrain tile coordinate and plane disagree.");
			if(!rawByte(e)||!rawByte(texture)||!rawByte(overlay)||!rawByte(roof)||!rawByte(horizontal)||!rawByte(vertical))throw new IllegalArgumentException("Terrain byte values must be 0..255.");
			worldX=x;worldY=y;plane=p;elevation=e;groundTexture=texture;groundOverlay=overlay;roofTexture=roof;horizontalWall=horizontal;verticalWall=vertical;this.diagonal=diagonal;
			int baseY=y-p*944;sectorX=Math.floorDiv(x+2304,REGION_SIZE);sectorY=Math.floorDiv(baseY+1776,REGION_SIZE);localX=Math.floorMod(x,REGION_SIZE);localY=Math.floorMod(baseY,REGION_SIZE);
		}
		public static TileRecord of(int x,int y,int plane,int elevation,int texture,int overlay,int roof,int horizontal,int vertical,int diagonal){return new TileRecord(x,y,plane,elevation,texture,overlay,roof,horizontal,vertical,diagonal);}
		private String key(){return plane+":"+worldX+":"+worldY;}private String sectorName(){return "h"+plane+"x"+sectorX+"y"+sectorY;}
		private void writeTo(byte[] sector){int offset=(localX*REGION_SIZE+localY)*RECORD_SIZE;ByteBuffer out=ByteBuffer.wrap(sector,offset,RECORD_SIZE);out.put((byte)elevation).put((byte)groundTexture).put((byte)groundOverlay).put((byte)roofTexture).put((byte)horizontalWall).put((byte)verticalWall).putInt(diagonal);}
		private static boolean rawByte(int value){return value>=0&&value<=255;}
	}
	public static final class SaveResult {
		public final int tilesSaved,sectorsChanged;public final String baseSha256,resultSha256;public final Path serverArchive,clientArchive,backupArchive;
		private SaveResult(int tiles,int sectors,String base,String result,Path server,Path client,Path backup){tilesSaved=tiles;sectorsChanged=sectors;baseSha256=base;resultSha256=result;serverArchive=server;clientArchive=client;backupArchive=backup;}
	}
}
