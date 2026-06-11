#!/usr/bin/env python3
import hashlib
import sys
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVER_LANDSCAPE = ROOT / "server/conf/server/data/Custom_Landscape.orsc"
CLIENT_LANDSCAPE = ROOT / "Client_Base/Cache/video/Custom_Landscape.orsc"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require_valid_zip(path: Path) -> None:
    if not path.exists():
        fail(f"Missing landscape archive: {path}")
    with zipfile.ZipFile(path) as archive:
        bad_member = archive.testzip()
        if bad_member is not None:
            fail(f"{path} contains a corrupt archive member: {bad_member}")


def main() -> None:
    require_valid_zip(SERVER_LANDSCAPE)
    require_valid_zip(CLIENT_LANDSCAPE)

    server_hash = sha256(SERVER_LANDSCAPE)
    client_hash = sha256(CLIENT_LANDSCAPE)
    if server_hash != client_hash:
        fail(
            "Client and server custom landscape archives must match exactly: "
            f"server={server_hash} client={client_hash}"
        )

    print("PASS: client and server custom landscape archives match")


if __name__ == "__main__":
    main()
