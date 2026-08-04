#!/bin/bash

if [[ -z "$JAVA_HOME" ]]; then
    JAVA=$(command -v java)
else
    JAVA="$JAVA_HOME/bin/java"
fi

if [[ -z "$JAVA" || ! -x "$JAVA" ]]; then
    echo "Java is required to run this program!"
    echo "You need to install JRE(Java Runtime Environment) version 11 or above."
    exit 1
fi

# javap is a JDK-only tool, so the version is read from java -version,
# which is available on a plain JRE as well. It is printed on stderr.
JAVA_VER=$("$JAVA" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n 1)

JAVA_MAJOR=""
if [[ "$JAVA_VER" =~ ^1\.([0-9]+) ]]; then
    # Legacy "1.x" scheme: the major version is x.
    JAVA_MAJOR="${BASH_REMATCH[1]}"
elif [[ "$JAVA_VER" =~ ^([0-9]+) ]]; then
    JAVA_MAJOR="${BASH_REMATCH[1]}"
fi

if [[ -z "$JAVA_MAJOR" ]]; then
    # Never block on an unrecognized version string, just warn.
    echo "Warning: unable to determine the Java version, running anyway."
elif (( JAVA_MAJOR < 11 )); then
    echo "Java too old!"
    echo "You need to install JRE(Java Runtime Environment) version 11 or above."
    exit 1
fi

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$BASE_DIR" || exit 1
JDBGEN=$(find "$BASE_DIR" -maxdepth 1 -name 'jdbgen-*.jar' -print | sort | head -n 1)

if [[ -z "$JDBGEN" ]]; then
    echo "Cannot find jdbgen-*.jar in $BASE_DIR!"
    exit 1
fi

"$JAVA" -jar "$JDBGEN"
