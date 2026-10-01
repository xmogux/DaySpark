#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "Java was not found. Install JDK 17 or newer first."
  exit 1
fi

if command -v mvn >/dev/null 2>&1; then
  exec mvn spring-boot:run
fi

VERSION="3.9.9"
TOOLS="$PWD/.tools"
MAVEN_HOME="$TOOLS/apache-maven-$VERSION"
MAVEN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN" ]; then
  mkdir -p "$TOOLS"
  ARCHIVE="$TOOLS/apache-maven-$VERSION-bin.tar.gz"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$VERSION/apache-maven-$VERSION-bin.tar.gz"
  echo "Maven is not installed. Downloading Maven $VERSION once..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "curl or wget is required to download Maven."
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$TOOLS"
  rm -f "$ARCHIVE"
fi

exec "$MAVEN" spring-boot:run
