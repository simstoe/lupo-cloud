#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

jar="$(ls -t build/libs/*-all.jar 2>/dev/null | head -n1)"
if [ -z "$jar" ]; then
    echo "No shadow jar found in build/libs — run ./gradlew shadowJar first." >&2
    exit 1
fi

# --sun-misc-unsafe-memory-access=allow: grpc-netty-shaded's bundled Netty still probes
# sun.misc.Unsafe at startup; on current JDKs that prints a terminally-deprecated warning.
# This flag silences it without changing behavior (default is already "allow").
exec java --sun-misc-unsafe-memory-access=allow -jar "$jar" "$@"
