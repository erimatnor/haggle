#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <path>"
    echo $0 ./scenarios
    exit 1
}

if [ $# -lt 1 ]; then 
    USAGE
fi

cd $(dirname $0)
if [ ! -a openfile.class ] ; then
	javac openfile.java
fi
java openfile $1
