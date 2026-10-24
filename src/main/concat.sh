#!/usr/bin/env bash

# Usage:
#   ./concat.sh <directory> <output-file> [extension]
#
# Examples:
#   ./concat.sh ./src all.txt
#   ./concat.sh ./src all-java.txt java
#   ./concat.sh . project.txt

set -euo pipefail

DIR="${1:-.}"
OUTPUT="${2:-combined.txt}"
EXTENSION="${3:-}"

# Empty/create output file
: > "$OUTPUT"

if [[ -n "$EXTENSION" ]]; then
    find "$DIR" -type f -name "*.${EXTENSION}" -print0 |
    while IFS= read -r -d '' file; do
        echo "===== FILE: $file =====" >> "$OUTPUT"
        cat "$file" >> "$OUTPUT"
        printf '\n\n' >> "$OUTPUT"
    done
else
    find "$DIR" -type f -not -path "$OUTPUT" -print0 |
    while IFS= read -r -d '' file; do
        echo "===== FILE: $file =====" >> "$OUTPUT"
        cat "$file" >> "$OUTPUT"
        printf '\n\n' >> "$OUTPUT"
    done
fi

echo "Created: $OUTPUT"
