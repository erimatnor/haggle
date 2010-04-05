#!/bin/sh

. node.conf

USAGE() {
    echo USAGE:
    echo "$0 <node name> <application>"
    exit 1
}

if [ $# -lt 2 ];then 
    USAGE
fi

node_name=$1
application=$2

if ! ssh $NODE_USERNAME@$node_name "ps ax | grep SCREEN | grep $application | awk '{print \$1}' | xargs kill"; then
	exit 1
fi
