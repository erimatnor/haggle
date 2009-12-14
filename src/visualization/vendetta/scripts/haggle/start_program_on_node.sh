#!/bin/sh

. node.conf

USAGE() {
    echo USAGE:
    echo "$0 <node name> <application> [<parameters>]"
    exit 1
}

if [ $# -lt 2 ];then 
    USAGE
fi

node_name=$1
application=$2
parameters=$3

if ! ssh $NODE_USERNAME@$node_name screen -dmS $application $application $parameters; then
	echo "exit 1"
	exit 1
fi
