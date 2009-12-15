#!/bin/bash

USAGE() {
    echo USAGE:
    echo "$0"
    exit 1
}

if [ $# -lt 0 ];then 
    USAGE
fi

cd $(dirname $0)

number_of_tries=0
done=0

if ! sudo xm list > node_list; then
	exit 1
fi

for (( i=0; i<100; i++ )); do
	if [ "`grep -w "node-${i}" node_list`" == "" ]; then
		echo node-$i is not running.
		nodeno=$i
		break
	fi
done

while [ $done -eq "0" ]; do
	number_of_running_nodes=0
	number_of_started_nodes=0
	
	echo Starting node-$nodeno
	while [[ ! `./manage_node.sh start node-$nodeno` ]]; do
		echo Starting node-$nodeno
	done
	
	echo "Waiting for nodes to start."
	sleep 5
	
	if [ "`ping -c 3 node-${nodeno} | grep " 0% packet loss"`" == "" ]; then
		echo "node-$i is not answering to ping."
		if [ "$number_of_tries" -eq "3" ]; then
			echo Destroying node-$nodeno
			sudo xm destroy node-$nodeno
			number_of_tries=0
		fi		
	else
		done=1
	fi
	let number_of_tries=$number_of_tries+1
done
if ! sudo xm list > node_list; then
	exit 1
fi
echo
echo Created node-$nodeno.
