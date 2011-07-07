#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <node name> <DO file name>"
    exit 1
}

if [ $# -lt 1 ];then 
    USAGE
fi

if [ -z $2 ] ; then
    echo "Must specify file"
    exit 1
elif [ -a $2 ] ; then
    cat $2 | telnet $1 9697
fi
