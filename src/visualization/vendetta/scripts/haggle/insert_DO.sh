#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <node name>"
    exit 1
}

if [ $# -lt 1 ];then 
    USAGE
fi

cd $(dirname $0)
FILE=$(./get_file.sh)
if [ -z $FILE ] ; then
    echo ""
elif [ -a $FILE ] ; then
    cat $FILE | telnet $1 9697
fi
