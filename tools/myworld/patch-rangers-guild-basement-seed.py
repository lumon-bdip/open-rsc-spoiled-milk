#!/usr/bin/env python3
"""Seed the Rangers Guild basement stairs and ground-floor stair opening."""

from __future__ import annotations

import argparse
import hashlib
import os
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path


BASEMENT_SECTOR_NAME = "h3x58y46"
BASEMENT_SECTOR_ORIGIN_X = 480
BASEMENT_SECTOR_ORIGIN_Y = 3264
GROUND_SECTOR_NAME = "h0x58y46"
GROUND_SECTOR_ORIGIN_X = 480
GROUND_SECTOR_ORIGIN_Y = 432
REGION_SIZE = 48
TILE_SIZE = 10
SUPPORTED_SECTOR_HASHES = {
    BASEMENT_SECTOR_NAME: {
        # Blank underground void sector before the Rangers Guild basement seed.
        "56aa2903284826a662f997033c99efe7f61b46b95dd7100d4d56bc39100c2cf2",
        # Idempotent rerun after this patch has already been applied.
        "47a39037dcc5fca49311aa986d798758197f22c4c8327c735ba0bab1ec9d6671",
    },
    GROUND_SECTOR_NAME: {
        # Rangers Guild first-pass floor before carving the down-stair opening.
        "c206f8de59341a27918b787b4599bc1c800a3763e7c6f6eb14e43d118a03d602",
        # Idempotent rerun after this patch has already been applied.
        "46c424f93a490bc5c4ae2cf99d966696fd6aabdb6665da2d610da1cbd610a1ee",
    },
}
TARGETS = (
    Path("server/conf/server/data/Custom_Landscape.orsc"),
    Path("Client_Base/Cache/video/Custom_Landscape.orsc"),
)

BASEMENT_FLOOR_TILES = {
    (x, y)
    for x in range(496, 504)
    for y in range(3294, 3302)
}
GROUND_DOWN_STAIR_TILES = {
    (x, y)
    for x in range(499, 501)
    for y in range(469, 472)
}


def tile_offset(x: int, y: int, *, origin_x: int, origin_y: int, sector_name: str) -> int:
    local_x = x - origin_x
    local_y = y - origin_y
    if not (0 <= local_x < REGION_SIZE and 0 <= local_y < REGION_SIZE):
        raise ValueError(f"Tile ({x}, {y}) is outside {sector_name}")
    return (local_x * REGION_SIZE + local_y) * TILE_SIZE


def set_tile(
    sector: bytearray,
    x: int,
    y: int,
    *,
    elevation: int,
    texture: int,
    overlay: int,
    origin_x: int,
    origin_y: int,
    sector_name: str,
) -> None:
    struct.pack_into(
        ">BBBBBBI",
        sector,
        tile_offset(x, y, origin_x=origin_x, origin_y=origin_y, sector_name=sector_name),
        elevation,
        texture,
        overlay,
        0,
        0,
        0,
        0,
    )


def set_overlay(
    sector: bytearray,
    x: int,
    y: int,
    *,
    overlay: int,
    origin_x: int,
    origin_y: int,
    sector_name: str,
) -> None:
    offset = tile_offset(x, y, origin_x=origin_x, origin_y=origin_y, sector_name=sector_name)
    elevation, texture, _, roof, east_wall, north_wall, diagonal_wall = struct.unpack_from(
        ">BBBBBBI",
        sector,
        offset,
    )
    struct.pack_into(
        ">BBBBBBI",
        sector,
        offset,
        elevation,
        texture,
        overlay,
        roof,
        east_wall,
        north_wall,
        diagonal_wall,
    )


def build_patched_sector(sector_name: str, source: bytes) -> bytes:
    sector = bytearray(source)
    if sector_name == BASEMENT_SECTOR_NAME:
        for x, y in BASEMENT_FLOOR_TILES:
            set_tile(
                sector,
                x,
                y,
                elevation=128,
                texture=70,
                overlay=16,
                origin_x=BASEMENT_SECTOR_ORIGIN_X,
                origin_y=BASEMENT_SECTOR_ORIGIN_Y,
                sector_name=BASEMENT_SECTOR_NAME,
            )
    elif sector_name == GROUND_SECTOR_NAME:
        for x, y in GROUND_DOWN_STAIR_TILES:
            set_overlay(
                sector,
                x,
                y,
                overlay=8,
                origin_x=GROUND_SECTOR_ORIGIN_X,
                origin_y=GROUND_SECTOR_ORIGIN_Y,
                sector_name=GROUND_SECTOR_NAME,
            )
    else:
        raise ValueError(f"Unsupported sector {sector_name}")
    return bytes(sector)


def patch_archive(path: Path, *, allow_unknown_source: bool) -> None:
    with zipfile.ZipFile(path, "r") as archive:
        patched_sectors = {}
        for sector_name, supported_hashes in SUPPORTED_SECTOR_HASHES.items():
            source_sector = archive.read(sector_name)
            source_hash = hashlib.sha256(source_sector).hexdigest()
            if source_hash not in supported_hashes and not allow_unknown_source:
                raise SystemExit(
                    f"{path}: unexpected {sector_name} hash {source_hash}; "
                    "rerun with --allow-unknown-source after reviewing the sector"
                )
            patched_sectors[sector_name] = build_patched_sector(sector_name, source_sector)

        fd, temp_name = tempfile.mkstemp(
            prefix=path.name + ".", suffix=".tmp", dir=str(path.parent)
        )
        os.close(fd)
        temp_path = Path(temp_name)
        try:
            with zipfile.ZipFile(temp_path, "w") as output:
                for info in archive.infolist():
                    data = patched_sectors.get(info.filename)
                    if data is None:
                        data = archive.read(info.filename)
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

    print("Patched Rangers Guild basement terrain seed and stair opening")


if __name__ == "__main__":
    main()
