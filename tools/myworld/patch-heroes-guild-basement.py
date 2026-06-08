#!/usr/bin/env python3
"""Expand the Heroes' Guild basement cage room in Custom_Landscape.orsc."""

from __future__ import annotations

import argparse
import hashlib
import os
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path


SECTOR_NAME = "h3x55y46"
SECTOR_ORIGIN_X = 336
SECTOR_ORIGIN_Y = 3264
REGION_SIZE = 48
TILE_SIZE = 10
BASE_SECTOR_SHA256 = "1749036f6c1e59633e319c520996b4abad157dc10e581759d700d60ee6b5781f"
ALPHA_68_SECTOR_SHA256 = "493506c65c737bba1c3d77ba95a55fc88504c34712c54ebd40758c52013ed43e"
TRIMMED_SECTOR_SHA256 = "0a42c3af6ee61225cce7d110172de7a119ea647a5bc0451a55416258b5594491"
SUPPORTED_SOURCE_HASHES = {
    BASE_SECTOR_SHA256,
    ALPHA_68_SECTOR_SHA256,
    TRIMMED_SECTOR_SHA256,
}

TARGETS = (
    Path("server/conf/server/data/Custom_Landscape.orsc"),
    Path("Client_Base/Cache/video/Custom_Landscape.orsc"),
)


def tile_offset(x: int, y: int) -> int:
    local_x = x - SECTOR_ORIGIN_X
    local_y = y - SECTOR_ORIGIN_Y
    if not (0 <= local_x < REGION_SIZE and 0 <= local_y < REGION_SIZE):
        raise ValueError(f"Tile ({x}, {y}) is outside {SECTOR_NAME}")
    return (local_x * REGION_SIZE + local_y) * TILE_SIZE


def set_tile(
    sector: bytearray,
    x: int,
    y: int,
    *,
    elevation: int | None = None,
    texture: int | None = None,
    overlay: int | None = None,
    roof: int | None = None,
    horizontal_wall: int | None = None,
    vertical_wall: int | None = None,
    diagonal_wall: int | None = None,
) -> None:
    offset = tile_offset(x, y)
    values = list(struct.unpack_from(">BBBBBBI", sector, offset))
    replacements = (
        elevation,
        texture,
        overlay,
        roof,
        horizontal_wall,
        vertical_wall,
        diagonal_wall,
    )
    for index, value in enumerate(replacements):
        if value is not None:
            values[index] = value
    struct.pack_into(">BBBBBBI", sector, offset, *values)


def build_patched_sector(source: bytes) -> bytes:
    sector = bytearray(source)

    # The chamber ends at x=376 and y=3275. The east wall is stored on the
    # adjacent void column, while y=3276 belongs to the dirt approach.
    for x in range(365, 377):
        for y in range(3264, 3276):
            set_tile(
                sector,
                x,
                y,
                elevation=128,
                texture=70,
                overlay=3,
                roof=0,
                horizontal_wall=0,
                vertical_wall=0,
                diagonal_wall=0,
            )

    # Stone outer walls. The south side remains open into the ore approach.
    for y in range(3264, 3276):
        set_tile(sector, 365, y, horizontal_wall=1)
        set_tile(sector, 377, y, horizontal_wall=1)
    for x in range(365, 377):
        set_tile(sector, x, 3264, vertical_wall=1)

    # Keep x=377 as void while retaining the horizontal wall required by the
    # east cages. The north-edge wall at 377,3264 is deliberately absent.
    for y in range(3264, 3277):
        set_tile(
            sector,
            377,
            y,
            overlay=8,
            roof=0,
            horizontal_wall=1 if y < 3276 else 0,
            vertical_wall=0,
            diagonal_wall=0,
        )

    # Railings split the four equal cages.
    for x in (*range(365, 370), *range(373, 377)):
        set_tile(sector, x, 3270, vertical_wall=6)

    # Two-tile gaps in each aisle-facing railing are occupied by gate objects.
    gate_tiles = {3266, 3267, 3272, 3273}
    for x in (369, 373):
        for y in range(3264, 3276):
            if y not in gate_tiles and not (x == 369 and y in (3269, 3275)):
                set_tile(sector, x, y, horizontal_wall=6)

    # Remove the old south railing and extend the dirt approach westward into
    # the ore room without flattening the existing slope.
    for x in range(365, 377):
        set_tile(
            sector,
            x,
            3276,
            overlay=8,
            roof=0,
            horizontal_wall=0,
            vertical_wall=0,
            diagonal_wall=0,
        )
    for x in range(365, 369):
        for y in range(3276, 3281):
            set_tile(
                sector,
                x,
                y,
                overlay=8,
                roof=0,
                horizontal_wall=0,
                vertical_wall=0,
                diagonal_wall=0,
            )

    # Bound the east side of the widened dirt approach so it connects the
    # monster room to the ore room without opening onto the surrounding void.
    for y in range(3276, 3281):
        set_tile(sector, 368, y, horizontal_wall=1)

    return bytes(sector)


def rewrite_archive(path: Path, patched_sector: bytes) -> None:
    path = path.resolve()
    source_mode = path.stat().st_mode
    source_bytes = path.read_bytes()
    with zipfile.ZipFile(path, "r") as archive:
        entries = archive.infolist()
        central_offset = archive.start_dir

    target_entry = next(entry for entry in entries if entry.filename == SECTOR_NAME)
    compressor = zlib.compressobj(level=9, wbits=-15)
    compressed_sector = compressor.compress(patched_sector) + compressor.flush()

    local_chunks: list[bytes] = []
    local_offsets: dict[str, int] = {}
    output_offset = 0
    for index, entry in enumerate(entries):
        start = entry.header_offset
        end = entries[index + 1].header_offset if index + 1 < len(entries) else central_offset
        chunk = source_bytes[start:end]
        local_offsets[entry.filename] = output_offset

        if entry.filename == SECTOR_NAME:
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
            descriptor_signature = b"PK\x07\x08"
            if not descriptor.startswith(descriptor_signature):
                raise SystemExit(f"{path}: missing ZIP data descriptor")
            crc = zlib.crc32(patched_sector)
            descriptor = descriptor_signature + struct.pack(
                "<III", crc, len(compressed_sector), len(patched_sector)
            )
            chunk = chunk[:header_length] + compressed_sector + descriptor

        local_chunks.append(chunk)
        output_offset += len(chunk)

    central_chunks: list[bytes] = []
    cursor = central_offset
    target_crc = zlib.crc32(patched_sector)
    for entry in entries:
        if struct.unpack_from("<I", source_bytes, cursor)[0] != 0x02014B50:
            raise SystemExit(f"{path}: invalid ZIP central directory")
        name_length, extra_length, comment_length = struct.unpack_from(
            "<HHH", source_bytes, cursor + 28
        )
        length = 46 + name_length + extra_length + comment_length
        central_entry = bytearray(source_bytes[cursor : cursor + length])
        struct.pack_into("<I", central_entry, 42, local_offsets[entry.filename])
        if entry.filename == SECTOR_NAME:
            struct.pack_into("<I", central_entry, 16, target_crc)
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

    fd, temp_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    os.close(fd)
    temp_path = Path(temp_name)
    try:
        temp_path.write_bytes(replacement)
        with zipfile.ZipFile(temp_path) as archive:
            if archive.testzip() is not None or archive.read(SECTOR_NAME) != patched_sector:
                raise SystemExit(f"{path}: rewritten landscape archive failed validation")
        os.chmod(temp_path, source_mode)
        os.replace(temp_path, path)
    finally:
        temp_path.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="verify that every target already contains the patched sector",
    )
    args = parser.parse_args()

    expected_patched: bytes | None = None
    for path in TARGETS:
        with zipfile.ZipFile(path) as archive:
            source_sector = archive.read(SECTOR_NAME)

        source_hash = hashlib.sha256(source_sector).hexdigest()
        candidate = build_patched_sector(source_sector)
        if expected_patched is None:
            if source_hash not in SUPPORTED_SOURCE_HASHES and candidate != source_sector:
                raise SystemExit(
                    f"{path}: unexpected source sector {source_hash}; "
                    "refusing to layer this patch over unknown terrain"
                )
            expected_patched = candidate
        elif source_sector not in (expected_patched,):
            if source_hash not in SUPPORTED_SOURCE_HASHES:
                raise SystemExit(f"{path}: client/server landscape sectors differ")
            candidate = build_patched_sector(source_sector)

        if source_sector == candidate:
            print(f"{path}: Heroes' Guild basement sector is current")
            continue
        if args.check:
            raise SystemExit(f"{path}: Heroes' Guild basement sector is out of date")

        rewrite_archive(path, candidate)
        print(f"{path}: patched {SECTOR_NAME}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
