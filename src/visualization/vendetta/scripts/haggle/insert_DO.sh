#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <node name>"
    exit 1
}

if [ $# -lt 1 ];then 
    USAGE
fi

HERE=$(pwd)
cd $(dirname $0)
FILE=$(./get_file.sh $HERE)
if [ -z $FILE ] ; then
    echo ""
elif [ -a $FILE ] ; then
    cat $FILE | ./do.sh $1 9697
fi
