#!/bin/bash

JAVA=$(which java)
BASE_DIR=$(dirname $0)
cd $BASE_DIR
JDBGEN=$(find . -name jdbgen-*.jar -print)
$JAVA -jar $JDBGEN
