#!/bin/sh

USAGE() {
    echo USAGE:
    echo "$0 <node name>"
    exit 1
}

if [ $# -lt 1 ];then 
    USAGE
fi

cd $(dirname $0)
rm *.log
cd ../../
rm nodelogs/$1/*
