#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0 <nodename> <nodename>"
    exit 1
}

if [ $# -lt 2 ];then 
    USAGE
fi

if [ -e node_list ]; then
	NODE_A=$(grep -w $1 node_list | awk -F' ' '{print $2}')
else
	NODE_A=$(sudo xm list | grep -w $1 | awk -F' ' '{print $2}')
fi
if [ -e node_list ]; then
	NODE_B=$(grep -w $2 node_list | awk -F' ' '{print $2}')
else
	NODE_B=$(sudo xm list | grep -w $2 | awk -F' ' '{print $2}')
fi

if [ $NODE_A != "" ] && [ $NODE_B != "" ]; then
	OUTPUT=$(sudo iptables -n --list FORWARD | grep "\--physdev-in vif${NODE_A}.0 --physdev-out vif${NODE_B}.0")
	if [ "$OUTPUT" = "" ]; then
		exit 1;
	fi
else
    USAGE
	exit 1;
fi
