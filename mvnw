#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_VERSION="3.9.16"
MVN_DIR="$SCRIPT_DIR/.mvn/apache-maven-$MVN_VERSION"
MVN_ZIP="$SCRIPT_DIR/.mvn/apache-maven-$MVN_VERSION-bin.zip"

if [ ! -x "$MVN_DIR/bin/mvn" ]; then
  mkdir -p "$SCRIPT_DIR/.mvn"
  if [ ! -f "$MVN_ZIP" ]; then
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.zip" -o "$MVN_ZIP"
  fi
  rm -rf "$MVN_DIR"
  unzip -q "$MVN_ZIP" -d "$SCRIPT_DIR/.mvn"
fi

exec "$MVN_DIR/bin/mvn" -f "$SCRIPT_DIR/pom.xml" "$@"
