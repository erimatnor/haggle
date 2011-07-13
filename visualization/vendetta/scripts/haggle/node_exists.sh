#!/bin/bash

# If the file does not exist:
if ! [ -a node_list ] ; then
	# Try to create it:
	if ! sudo xm list > node_list; then
		# Terminal failure:
		exit 1
	fi
fi

# Check for nonexistence
if [ "`grep -w $1 node_list`" == "" ]; then
	# Node does not exist, fail.
	exit 1;
fi
# Node must exist, succeed.
