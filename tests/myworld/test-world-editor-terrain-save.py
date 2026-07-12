#!/usr/bin/env python3
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SAVE_FILES = ROOT / "server/src/com/openrsc/server/io/WorldEditorTerrainSaveFiles.java"

HARNESS = r"""
import com.openrsc.server.io.WorldEditorTerrainSaveFiles;
import com.openrsc.server.io.WorldEditorTerrainSaveFiles.TileRecord;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class WorldEditorTerrainSaveHarness {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void rejects(RunnableWithIo action, String message) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError(message);
    }
    private static byte[] bytes(int seed) {
        byte[] data = new byte[48 * 48 * 10];
        for (int i = 0; i < data.length; i++) data[i] = (byte)(seed + i * 13);
        return data;
    }
    private static void createArchive(Path path, byte[] first, byte[] second) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.setComment("fixture");
            write(zip, "h0x48y37", first);
            write(zip, "h0x48y38", second);
        }
    }
    private static void write(ZipOutputStream zip, String name, byte[] data) throws Exception {
        ZipEntry entry = new ZipEntry(name); entry.setTime(1700000000000L);
        zip.putNextEntry(entry); zip.write(data); zip.closeEntry();
    }
    private static byte[] read(ZipFile zip, String name) throws Exception {
        try (InputStream in = zip.getInputStream(zip.getEntry(name)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) >= 0) if (count > 0) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }
    public static void main(String[] args) throws Exception {
        Path root = java.nio.file.Paths.get(args[0]);
        Path server = root.resolve("server.orsc"), client = root.resolve("client.orsc"), backups = root.resolve("backups");
        byte[] first = bytes(3), second = bytes(7);
        createArchive(server, first, second); Files.copy(server, client, StandardCopyOption.REPLACE_EXISTING);
        String base = WorldEditorTerrainSaveFiles.sha256(server);
        List<TileRecord> edits = Arrays.asList(
            TileRecord.of(1, 2, 0, 11, 12, 13, 14, 15, 16, 0x01020304),
            TileRecord.of(0, 0, 0, 1, 2, 3, 4, 5, 6, 0x11223344));
        WorldEditorTerrainSaveFiles.SaveResult result = WorldEditorTerrainSaveFiles.save(server, client, backups, base, edits);
        require(result.tilesSaved == 2 && result.sectorsChanged == 1, "save summary changed");
        require(result.resultSha256.equals(WorldEditorTerrainSaveFiles.sha256(server)), "result hash mismatch");
        require(result.resultSha256.equals(WorldEditorTerrainSaveFiles.sha256(client)), "client/server outputs differ");
        require(base.equals(WorldEditorTerrainSaveFiles.sha256(result.backupArchive)), "backup does not match base");
        try (ZipFile saved = new ZipFile(server.toFile())) {
            Enumeration<? extends ZipEntry> names = saved.entries();
            require(names.nextElement().getName().equals("h0x48y37") && names.nextElement().getName().equals("h0x48y38"), "entry order changed");
            byte[] patched = read(saved, "h0x48y37");
            require((patched[0] & 255) == 1 && (patched[1] & 255) == 2 && (patched[5] & 255) == 6, "first tile bytes wrong");
            int offset = (1 * 48 + 2) * 10;
            require((patched[offset] & 255) == 11 && (patched[offset + 5] & 255) == 16, "second tile bytes wrong");
            require(Arrays.equals(second, read(saved, "h0x48y38")), "unrelated sector changed");
        }
        String savedHash = result.resultSha256;
        rejects(() -> WorldEditorTerrainSaveFiles.save(server, client, backups, base, edits), "stale base hash was accepted");
        require(savedHash.equals(WorldEditorTerrainSaveFiles.sha256(server)), "stale save changed server archive");
        Files.write(client, new byte[]{99}, java.nio.file.StandardOpenOption.APPEND);
        rejects(() -> WorldEditorTerrainSaveFiles.save(server, client, backups, savedHash, edits), "divergent client archive was accepted");
        require(savedHash.equals(WorldEditorTerrainSaveFiles.sha256(server)), "divergent save changed server archive");
        Files.copy(server, client, StandardCopyOption.REPLACE_EXISTING);
        List<TileRecord> missing = Arrays.asList(TileRecord.of(5000, 0, 0, 1, 2, 3, 4, 5, 6, 0));
        rejects(() -> WorldEditorTerrainSaveFiles.save(server, client, backups, savedHash, missing), "missing sector was accepted");
        require(savedHash.equals(WorldEditorTerrainSaveFiles.sha256(server)) && savedHash.equals(WorldEditorTerrainSaveFiles.sha256(client)), "failed save changed archives");
    }
    private interface RunnableWithIo { void run() throws Exception; }
}
"""

with tempfile.TemporaryDirectory(prefix="world-editor-terrain-save-") as temp:
    temp_path = Path(temp)
    harness = temp_path / "WorldEditorTerrainSaveHarness.java"
    harness.write_text(HARNESS, encoding="utf-8")
    subprocess.run(["javac", "-d", temp, str(SAVE_FILES), str(harness)], check=True)
    subprocess.run(["java", "-cp", temp, "WorldEditorTerrainSaveHarness", temp], check=True)

print("PASS: terrain save materialization, backup, hashes, and failure guards validated")
