#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <node name>"
    exit 1
}

if [ $# -lt 0 ];then 
    USAGE
fi

cd $(dirname $0)

node_name=$1
echo Shutting down $node_name

while [[ ! `./manage_node.sh shutdown $node_name` ]];do
	echo Shutting down $node_name
done

sudo xm list > node_list
