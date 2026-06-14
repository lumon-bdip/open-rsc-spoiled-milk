#!/usr/bin/env python3
"""Seed the Rangers Guild basement stairs with a small walkable cave floor."""

from __future__ import annotations

import argparse
import hashlib
import os
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path


SECTOR_NAME = "h3x58y46"
SECTOR_ORIGIN_X = 480
SECTOR_ORIGIN_Y = 3264
REGION_SIZE = 48
TILE_SIZE = 10
SUPPORTED_SOURCE_HASHES = {
    # Blank underground void sector before the Rangers Guild basement seed.
    "56aa2903284826a662f997033c99efe7f61b46b95dd7100d4d56bc39100c2cf2",
    # Idempotent rerun after this patch has already been applied.
    "47a39037dcc5fca49311aa986d798758197f22c4c8327c735ba0bab1ec9d6671",
}
TARGETS = (
    Path("server/conf/server/data/Custom_Landscape.orsc"),
    Path("Client_Base/Cache/video/Custom_Landscape.orsc"),
)

FLOOR_TILES = {
    (x, y)
    for x in range(496, 504)
    for y in range(3294, 3302)
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
    elevation: int,
    texture: int,
    overlay: int,
) -> None:
    struct.pack_into(
        ">BBBBBBI",
        sector,
        tile_offset(x, y),
        elevation,
        texture,
        overlay,
        0,
        0,
        0,
        0,
    )


def build_patched_sector(source: bytes) -> bytes:
    sector = bytearray(source)
    for x, y in FLOOR_TILES:
        set_tile(sector, x, y, elevation=128, texture=70, overlay=16)
    return bytes(sector)


def patch_archive(path: Path, *, allow_unknown_source: bool) -> None:
    with zipfile.ZipFile(path, "r") as archive:
        source_sector = archive.read(SECTOR_NAME)
        source_hash = hashlib.sha256(source_sector).hexdigest()
        if source_hash not in SUPPORTED_SOURCE_HASHES and not allow_unknown_source:
            raise SystemExit(
                f"{path}: unexpected {SECTOR_NAME} hash {source_hash}; "
                "rerun with --allow-unknown-source after reviewing the sector"
            )
        patched_sector = build_patched_sector(source_sector)

        fd, temp_name = tempfile.mkstemp(
            prefix=path.name + ".", suffix=".tmp", dir=str(path.parent)
        )
        os.close(fd)
        temp_path = Path(temp_name)
        try:
            with zipfile.ZipFile(temp_path, "w") as output:
                for info in archive.infolist():
                    data = patched_sector if info.filename == SECTOR_NAME else archive.read(info.filename)
                    compress_type = info.compress_type
                    if compress_type == zipfile.ZIP_DEFLATED and zlib is None:
                        compress_type = zipfile.ZIP_STORED
                    output.writestr(info, data, compress_type=compress_type)
            temp_path.replace(path)
        finally:
            if temp_path.exists():
                temp_path.unlink()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--allow-unknown-source",
        action="store_true",
        help="Patch even when the source sector hash is not in the reviewed set.",
    )
    args = parser.parse_args()

    for target in TARGETS:
        patch_archive(target, allow_unknown_source=args.allow_unknown_source)

    server_hash = hashlib.sha256(TARGETS[0].read_bytes()).hexdigest()
    client_hash = hashlib.sha256(TARGETS[1].read_bytes()).hexdigest()
    if server_hash != client_hash:
        raise SystemExit("Server and client Custom_Landscape.orsc hashes do not match")

    print("Patched Rangers Guild basement terrain seed")


if __name__ == "__main__":
    main()
