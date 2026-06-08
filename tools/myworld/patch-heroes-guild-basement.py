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
ALPHA_69_SECTOR_SHA256 = "93fe6421608137d5925bf267d37ab2e59e77770a3c71ffb59ec8fb810bb530eb"
ALPHA_70_SECTOR_SHA256 = "33353a42556f7bf3d3283102930e9612169103700ad58a00e46ffbcfb11bae6a"
ALPHA_71_SECTOR_SHA256 = "37422057a1618507520e6b1032d5eb36f9d09bd9d82245d9861000ebb5ee1d2f"
EXPANDED_CAVERN_SECTOR_SHA256 = "3091372d06193f64ddb27442be6ef9e5dd9a93044f67555930c8ebd0d0d24f84"
ALPHA_72_SECTOR_SHA256 = "823355f02c279495ec370f2f10e99ec498c4650593b1d71dc720c3e5196e0620"
ALPHA_73_SECTOR_SHA256 = "0e61ff1e45b8fe8835b2a9ad2e193a44a0e9bd47585ff2eeb389c730b0376efc"
DIAGONAL_CAVERN_SECTOR_SHA256 = "ca88243e9ba202490ba43b9ecfd361eaf99c7e19ecfd7d4076943a87e7c3d6dc"
SUPPORTED_SOURCE_HASHES = {
    BASE_SECTOR_SHA256,
    ALPHA_68_SECTOR_SHA256,
    TRIMMED_SECTOR_SHA256,
    ALPHA_69_SECTOR_SHA256,
    ALPHA_70_SECTOR_SHA256,
    ALPHA_71_SECTOR_SHA256,
    EXPANDED_CAVERN_SECTOR_SHA256,
    ALPHA_72_SECTOR_SHA256,
    ALPHA_73_SECTOR_SHA256,
    DIAGONAL_CAVERN_SECTOR_SHA256,
}

TARGETS = (
    Path("server/conf/server/data/Custom_Landscape.orsc"),
    Path("Client_Base/Cache/video/Custom_Landscape.orsc"),
)

# Irregular row spans form a narrow mine entrance that opens into a broad
# cavern without approaching the separate dungeon rooms farther south/east.
CAVERN_ROWS = {
    3276: (356, 376),
    3277: (352, 377),
    3278: (350, 378),
    3279: (349, 379),
    3280: (348, 380),
    3281: (347, 380),
    3282: (346, 381),
    3283: (345, 381),
    3284: (344, 382),
    3285: (343, 382),
    3286: (342, 381),
    3287: (341, 381),
    3288: (341, 380),
    3289: (342, 380),
    3290: (342, 379),
    3291: (343, 379),
    3292: (344, 378),
    3293: (345, 378),
    3294: (347, 377),
    3295: (350, 375),
}

ORIGINAL_ORE_ROWS = {
    3276: ((356, 356), (364, 376)),
    3277: ((353, 357), (365, 376)),
    3278: ((352, 360), (365, 376)),
    3279: ((352, 376),),
    3280: ((353, 375),),
    3281: ((354, 374),),
    3282: ((357, 367),),
    3283: ((359, 366),),
}

DIAGONAL_BOUNDARY_WALLS = {
    (351, 3277): 1,
    (349, 3278): 1,
    (347, 3280): 1,
    (345, 3282): 1,
    (343, 3284): 1,
    (340, 3287): 1,
    (341, 3290): 12001,
    (343, 3292): 12001,
    (346, 3294): 12001,
}


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


def is_original_ore_tile(x: int, y: int) -> bool:
    return any(
        minimum_x <= x <= maximum_x
        for minimum_x, maximum_x in ORIGINAL_ORE_ROWS.get(y, ())
    )


def expanded_cavern_elevation(x: int, y: int) -> int:
    # A broad bowl centered south-west of the old ore stalk. It keeps the new
    # cavern visually closer to the dipped mine floor while rising at the wall.
    dx = abs(x - 363)
    dy = abs(y - 3287)
    elevation = 18 + (dx * 4) + (dy * 9)
    if x < 352:
        elevation += (352 - x) * 5
    if x > 374:
        elevation += (x - 374) * 5
    if y > 3291:
        elevation += (y - 3291) * 7
    elevation += ((x * 3 + y * 5) % 9) - 4
    return max(14, min(124, elevation))


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
            horizontal_wall=1,
            vertical_wall=0,
            diagonal_wall=0,
        )

    # Railings split the four equal cages.
    for x in (*range(365, 369), *range(373, 377)):
        set_tile(sector, x, 3270, vertical_wall=6)

    # Two-tile gaps in each aisle-facing railing are occupied by gate objects.
    gate_tiles = {3266, 3267, 3272, 3273}
    for x in (369, 373):
        for y in range(3264, 3276):
            if y not in gate_tiles and not (x == 369 and y in (3270,)):
                set_tile(sector, x, y, horizontal_wall=6)

    cavern_tiles = {
        (x, y)
        for y, (minimum_x, maximum_x) in CAVERN_ROWS.items()
        for x in range(minimum_x, maximum_x + 1)
    }
    chamber_tiles = {
        (x, y)
        for x in range(365, 377)
        for y in range(3264, 3276)
    }
    occupied_tiles = cavern_tiles | chamber_tiles

    # Clear the old peninsula walls and cliff diagonals before laying out the
    # new floor. This prevents previous boundaries from crossing the cavern.
    for x in range(340, 384):
        for y in range(3276, 3297):
            set_tile(
                sector,
                x,
                y,
                horizontal_wall=0,
                vertical_wall=0,
                diagonal_wall=0,
            )

    # Use the existing brown underground floor and retain the terrain's
    # underlying elevation variation for a natural cavern surface.
    for x, y in cavern_tiles:
        elevation = None if is_original_ore_tile(x, y) else expanded_cavern_elevation(x, y)
        set_tile(
            sector,
            x,
            y,
            elevation=elevation,
            texture=176,
            overlay=0,
            roof=0,
            horizontal_wall=0,
            vertical_wall=0,
            diagonal_wall=0,
        )

    # These former entrance spurs are outside the new boundary.
    for x in (354, 355):
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

    # Generate a complete stone perimeter from the floor footprint. Horizontal
    # walls occupy the west edge of their tile; vertical walls occupy its north
    # edge, so east/south boundaries are stored on the adjacent void tile.
    for x, y in cavern_tiles:
        if (x - 1, y) not in occupied_tiles:
            set_tile(sector, x, y, horizontal_wall=1)
        if (x + 1, y) not in occupied_tiles:
            set_tile(sector, x + 1, y, horizontal_wall=1, diagonal_wall=0)
        if (x, y - 1) not in occupied_tiles:
            set_tile(sector, x, y, vertical_wall=1)
        if (x, y + 1) not in occupied_tiles:
            set_tile(sector, x, y + 1, vertical_wall=1, diagonal_wall=0)

    for (x, y), diagonal_wall in DIAGONAL_BOUNDARY_WALLS.items():
        set_tile(sector, x, y, diagonal_wall=diagonal_wall)
        if diagonal_wall < 12000:
            set_tile(sector, x + 1, y, horizontal_wall=0)
            set_tile(sector, x, y + 1, vertical_wall=0)
        else:
            set_tile(sector, x + 1, y, horizontal_wall=0)
            set_tile(sector, x, y, vertical_wall=0)

    # Close the southern sides of both lower cages while keeping the central
    # aisle open toward the ore room.
    for x in (*range(365, 369), *range(373, 377)):
        set_tile(sector, x, 3276, vertical_wall=6)

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
