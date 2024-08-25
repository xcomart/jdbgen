#!/bin/bash

if [[ -z "$JAVA_HOME" ]]; then
    JAVA=$(which java)
else
    JAVA=$JAVA_HOME/bin/java
fi

if [[ -z "$JAVA" ]]; then
    echo "Java is required to run this program!"
    echo "You need to install JRE(Java Runtime Environment) version 11 or above."
    exit 1
fi

JAVA_VER=$(${JAVA}p -verbose java.lang.String | grep "major version" | cut -d " " -f5)
if [[ "$JAVA_VER" < "55" ]]; then
    echo "Java too old!"
    echo "You need to install JRE(Java Runtime Environment) version 11 or above."
    exit 1
fi

BASE_DIR=$(dirname $0)
cd $BASE_DIR
JDBGEN=$(find . -name jdbgen-*.jar -print)
$JAVA -jar $JDBGEN
