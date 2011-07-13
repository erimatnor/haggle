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

if ! ssh $NODE_USERNAME@$node_name "COUNT_VAR= ; while [[ \$(ps ax | grep -v sshd | grep -c haggle) > 1 ]] ; do if [[ \$COUNT_VAR = AAAAAAAAAAAA ]] ; then ps ax | grep -v sshd | grep -v grep | grep \"haggle --non-interactive\" | head -n 1 | awk '{print \$1}' | xargs kill ; COUNT_VAR= ; fi ; COUNT_VAR=A\$COUNT_VAR ; sleep 5 ; done" ; then
	exit 1
fi
