#!/usr/bin/env python3
"""Mirror stdin to stdout while retaining a line-safe bounded log file."""

import argparse
import sys
from pathlib import Path


TRUNCATION_MARKER = b"[renderer diagnostics] console log byte budget reached; retention stopped.\n"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log_file", type=Path)
    parser.add_argument("max_bytes", type=int)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.max_bytes < len(TRUNCATION_MARKER):
        raise SystemExit("max_bytes must be large enough for the truncation marker")

    args.log_file.parent.mkdir(parents=True, exist_ok=True)
    retained = 0
    truncated = False
    with args.log_file.open("wb") as log:
        try:
            while True:
                line = sys.stdin.buffer.readline()
                if not line:
                    break
                sys.stdout.buffer.write(line)
                sys.stdout.buffer.flush()
                if truncated:
                    continue
                if retained + len(line) + len(TRUNCATION_MARKER) <= args.max_bytes:
                    log.write(line)
                    log.flush()
                    retained += len(line)
                else:
                    log.write(TRUNCATION_MARKER)
                    log.flush()
                    truncated = True
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    main()
