#!/usr/bin/env bash
# Prints the cache key for Robolectric's Android images (~/.m2/repository/org/robolectric), shared by ci.yml (which
# saves them) and release.yml: the Robolectric version and the SDK levels the tests ask for (@Config, and the sdk= line
# of robolectric.properties). Finding none of either is not an error: the key is always printed.
set -euo pipefail
version=$(sed -n 's/^robolectric = "\(.*\)"/\1/p' gradle/libs.versions.toml)
sdks=$({
    grep -rhoE 'sdk = \[[0-9, ]+\]' app/src/test || true
    grep -hE '^sdk=' app/src/test/resources/robolectric.properties 2>/dev/null || true
} | sort -u | md5sum | cut -c1-8)
echo "robolectric-$version-$sdks"
