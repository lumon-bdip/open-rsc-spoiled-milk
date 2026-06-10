#!/usr/bin/env python3
"""Replace the Cosmic Altar grass approach with an invisible L-shaped path."""

from __future__ import annotations

import argparse
import hashlib
import os
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path


REGION_SIZE = 48
TILE_SIZE = 10
WALKABLE_INVISIBLE_OVERLAY = 26
VOID_OVERLAY = 8
ALTAR_ELEVATION = 96
ALTAR_TEXTURE = 100
SUPPORTED_SECTOR_HASHES = {
    "h3x50y51": {
        "39b24288bd45917f43ec1e6c1ba565c166686e64d1bf00f8e0432b184b4b6001",
        "ea9ac5476144f56ebe94b8647a82cd0ffacad34a5d0e089cb2b49da12c20e6a5",
        "540c6bd0dabc601d597bbe7de241dfcfa629c8547cf6dbe66df2898dab0f155f",
        "36948c1db93e790c60fe1b4b192cd87b10ea27b1b1b49480f56c9a771432e40c",
    },
    "h3x50y52": {
        "ecdf819282e9f8b0e9b8e469d552413c34d3f7de8bd5623b678374faab1d3e23",
        "19e78cbb53d798f27d791c1a3fb865586cc5dd491d75505725b06fcf6e07964f",
        "c69bf776a0ee37a322a25a9331209c7674d7b258f303919b56238e2ed3cf9130",
    },
    "h3x51y51": {
        "d1f9f93a5aa1eddad8717fea194f83684ca0e5a1104df7464e49fd0e8b696105",
        "0a15241e107999d219c7a43dd851db154b029c0b82ddda0ec53a14aba05ae821",
        "f175da27a07f73d00cab976271659bc03b5338a8e159310d16acd575197e3bc5",
        "ff3837986a1f7cad3894699d8b07c82b0ab53d4cf84ea0cb1a394a4dc3ef4958",
    },
    "h3x51y52": {
        "5f57fb5d85c33f91eae2a69bfaf1bdc22d6ccaa67249699b877fbaf78095308c",
        "7e4b00a38e005b7a18e4c70b46ce3e65112673c15d1fb094ba92146a048b3722",
        "56aa2903284826a662f997033c99efe7f61b46b95dd7100d4d56bc39100c2cf2",
    },
}
TARGETS = (
    Path("server/conf/server/data/Custom_Landscape.orsc"),
    Path("Client_Base/Cache/video/Custom_Landscape.orsc"),
)


def sector_name(x: int, y: int) -> str:
    height = y // 944
    y_in_height = y - height * 944
    return f"h{height}x{x // REGION_SIZE + 48}y{y_in_height // REGION_SIZE + 37}"


def sector_origin(name: str) -> tuple[int, int]:
    height = int(name[1])
    section_x = int(name[3:5])
    section_y = int(name[6:8])
    return (section_x - 48) * REGION_SIZE, height * 944 + (section_y - 37) * REGION_SIZE


def tile_offset(sector: str, x: int, y: int) -> int:
    origin_x, origin_y = sector_origin(sector)
    local_x = x - origin_x
    local_y = y - origin_y
    if not (0 <= local_x < REGION_SIZE and 0 <= local_y < REGION_SIZE):
        raise ValueError(f"Tile ({x}, {y}) is outside {sector}")
    return (local_x * REGION_SIZE + local_y) * TILE_SIZE


def set_tile(
    sector_data: bytearray,
    sector: str,
    x: int,
    y: int,
    *,
    elevation: int,
    texture: int,
    overlay: int,
) -> None:
    offset = tile_offset(sector, x, y)
    struct.pack_into(
        ">BBBBBBI",
        sector_data,
        offset,
        elevation,
        texture,
        overlay,
        0,
        0,
        0,
        0,
    )


def old_grass_tiles() -> set[tuple[int, int]]:
    # Original Cosmic Altar floating grass/road footprint. The torch gap is
    # left in place below, and the new path/support tiles are rewritten after.
    rows = {
        3541: ((150, 150),),
        3542: ((148, 151),),
        3543: ((148, 151),),
        3544: ((148, 151),),
        3545: ((148, 151),),
        3546: ((148, 151),),
        3547: ((148, 151),),
        3548: ((148, 151),),
        3549: ((148, 151),),
        3550: ((148, 151),),
        3551: ((148, 151),),
        3552: ((147, 152),),
        3553: ((147, 152),),
        3554: ((97, 144), (147, 152)),
        3555: ((97, 144), (147, 152)),
        3556: ((97, 151),),
        3557: ((97, 150),),
        3558: ((97, 101), (138, 149)),
        3559: ((97, 101), (139, 148)),
        3560: ((97, 101),),
        3561: ((97, 101),),
        3562: ((97, 102),),
        3563: ((97, 103),),
        3564: ((97, 111),),
        3565: ((97, 111),),
        3566: ((98, 111),),
        3567: ((99, 111),),
        3568: ((100, 111),),
        3569: ((101, 111),),
    }
    return {
        (x, y)
        for y, spans in rows.items()
        for start_x, end_x in spans
        for x in range(start_x, end_x + 1)
    }


def preserved_torch_gap() -> set[tuple[int, int]]:
    return {(x, y) for x in (149, 150) for y in (3541, 3542)}


def invisible_path_tiles() -> set[tuple[int, int]]:
    tiles: set[tuple[int, int]] = set()
    for x in range(106, 149):
        for y in (3541, 3542):
            tiles.add((x, y))
    for y in range(3541, 3567):
        for x in (106, 107):
            tiles.add((x, y))
    return tiles


def object_support_tiles() -> set[tuple[int, int]]:
    return {
        (106, 3565),
        (104, 3564),
        (109, 3564),
        (104, 3568),
        (109, 3568),
    }


def affected_sectors() -> set[str]:
    tiles = old_grass_tiles() | invisible_path_tiles() | object_support_tiles()
    return {sector_name(x, y) for x, y in tiles}


def build_patched_sectors(source_sectors: dict[str, bytes]) -> dict[str, bytes]:
    sectors = {name: bytearray(data) for name, data in source_sectors.items()}
    for x, y in old_grass_tiles() - preserved_torch_gap():
        name = sector_name(x, y)
        if name in sectors:
            set_tile(sectors[name], name, x, y, elevation=0, texture=0, overlay=VOID_OVERLAY)

    for x, y in invisible_path_tiles() | object_support_tiles():
        name = sector_name(x, y)
        set_tile(
            sectors[name],
            name,
            x,
            y,
            elevation=ALTAR_ELEVATION,
            texture=ALTAR_TEXTURE,
            overlay=WALKABLE_INVISIBLE_OVERLAY,
        )

    return {name: bytes(data) for name, data in sectors.items()}


def rewrite_archive(path: Path, patched_sectors: dict[str, bytes], *, dry_run: bool) -> None:
    source_mode = path.stat().st_mode
    source_bytes = path.read_bytes()
    with zipfile.ZipFile(path, "r") as archive:
        entries = archive.infolist()
        central_offset = archive.start_dir
        for sector in patched_sectors:
            if archive.getinfo(sector).file_size != REGION_SIZE * REGION_SIZE * TILE_SIZE:
                raise SystemExit(f"{path}: {sector} has unexpected sector size")

    local_chunks: list[bytes] = []
    local_offsets: dict[str, int] = {}
    output_offset = 0
    for index, entry in enumerate(entries):
        start = entry.header_offset
        end = entries[index + 1].header_offset if index + 1 < len(entries) else central_offset
        chunk = source_bytes[start:end]
        local_offsets[entry.filename] = output_offset

        if entry.filename in patched_sectors:
            patched_sector = patched_sectors[entry.filename]
            compressor = zlib.compressobj(level=9, wbits=-15)
            compressed_sector = compressor.compress(patched_sector) + compressor.flush()
            (
                signature,
                _version,
                flags,
                _compression,
                _time,
                _date,
                _crc,
                _compressed_size,
                _size,
                name_length,
                extra_length,
            ) = struct.unpack_from("<IHHHHHIIIHH", chunk)
            if signature != 0x04034B50 or not flags & 0x08:
                raise SystemExit(f"{path}: unsupported landscape ZIP entry format")

            header_length = 30 + name_length + extra_length
            descriptor = chunk[header_length + entry.compress_size :]
            if not descriptor.startswith(b"PK\x07\x08"):
                raise SystemExit(f"{path}: missing ZIP data descriptor")
            descriptor = b"PK\x07\x08" + struct.pack(
                "<III", zlib.crc32(patched_sector), len(compressed_sector), len(patched_sector)
            )
            chunk = chunk[:header_length] + compressed_sector + descriptor

        local_chunks.append(chunk)
        output_offset += len(chunk)

    central_chunks: list[bytes] = []
    cursor = central_offset
    for entry in entries:
        if struct.unpack_from("<I", source_bytes, cursor)[0] != 0x02014B50:
            raise SystemExit(f"{path}: invalid ZIP central directory")
        name_length, extra_length, comment_length = struct.unpack_from("<HHH", source_bytes, cursor + 28)
        length = 46 + name_length + extra_length + comment_length
        central_entry = bytearray(source_bytes[cursor : cursor + length])
        struct.pack_into("<I", central_entry, 42, local_offsets[entry.filename])
        if entry.filename in patched_sectors:
            patched_sector = patched_sectors[entry.filename]
            compressor = zlib.compressobj(level=9, wbits=-15)
            compressed_sector = compressor.compress(patched_sector) + compressor.flush()
            struct.pack_into("<I", central_entry, 16, zlib.crc32(patched_sector))
            struct.pack_into("<I", central_entry, 20, len(compressed_sector))
            struct.pack_into("<I", central_entry, 24, len(patched_sector))
        central_chunks.append(bytes(central_entry))
        cursor += length

    if source_bytes[cursor : cursor + 4] != b"PK\x05\x06":
        raise SystemExit(f"{path}: ZIP64 or trailing central records are unsupported")
    end_record = bytearray(source_bytes[cursor:])
    central_data = b"".join(central_chunks)
    struct.pack_into("<I", end_record, 12, len(central_data))
    struct.pack_into("<I", end_record, 16, output_offset)
    replacement = b"".join(local_chunks) + central_data + bytes(end_record)

    if dry_run:
        print(f"{path}: would patch {', '.join(sorted(patched_sectors))}")
        return

    fd, temp_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    os.close(fd)
    temp_path = Path(temp_name)
    try:
        temp_path.write_bytes(replacement)
        with zipfile.ZipFile(temp_path) as archive:
            if archive.testzip() is not None:
                raise SystemExit(f"{path}: rewritten landscape archive failed validation")
            for sector, patched_sector in patched_sectors.items():
                if archive.read(sector) != patched_sector:
                    raise SystemExit(f"{path}: rewritten {sector} failed validation")
        os.chmod(temp_path, source_mode)
        os.replace(temp_path, path)
    finally:
        temp_path.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    sectors = affected_sectors()
    if sectors != set(SUPPORTED_SECTOR_HASHES):
        raise SystemExit(f"Unexpected affected sectors: {sorted(sectors)}")

    with zipfile.ZipFile(TARGETS[0]) as archive:
        source_sectors = {sector: archive.read(sector) for sector in sectors}

    for sector, expected_hashes in SUPPORTED_SECTOR_HASHES.items():
        actual_hash = hashlib.sha256(source_sectors[sector]).hexdigest()
        if actual_hash not in expected_hashes:
            raise SystemExit(f"{sector}: unsupported source hash {actual_hash}")

    patched_sectors = build_patched_sectors(source_sectors)
    for target in TARGETS:
        with zipfile.ZipFile(target) as archive:
            current = {sector: archive.read(sector) for sector in sectors}
        if target != TARGETS[0] and current != source_sectors:
            raise SystemExit(f"{target}: client/server source sectors differ")
        rewrite_archive(target, patched_sectors, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
