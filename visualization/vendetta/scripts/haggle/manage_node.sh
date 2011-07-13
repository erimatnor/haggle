#!/bin/sh
#  Manage Node
#  This script can create, start, stop, and remove nodes.
#  Fredrik Bjurefors <fredrik.bjurefors@it.uu.se>


USAGE() {
    echo USAGE:
    echo "$0 create|remove|start|shutdown imagename"
    echo $0 start node-3
    exit 1
}

if [ $# -lt 2 ]; then 
    USAGE
fi

create_image() 
{
	if ! ./create_xenconfig.sh $imagename; then
		exit 1
	fi
}
remove_image() 
{
	swapname=$imagename-swap.img
	echo "Removing swap file $imagename" 
	rm -rf $swappath/$swapname
	echo "Removing configuration file $imagename"
	rm -rf $confdir/$imagename
}

start_image()
{
	echo "Starting image $confdir/$imagename" 
	if [ ! -e $confdir/$imagename ]; then
		create_image
	fi
	if ! sudo /usr/sbin/xm create $confdir/$imagename &>/dev/null; then
		echo Could not start node!
		exit 1
	fi
}

shutdown_image()
{
	# Since cow is used, it is possible to use destroy without harming the filesystem.
	echo "Shutting down image $imagename" 
	if ! sudo /usr/sbin/xm destroy $imagename; then
		exit 1
	fi
}
cd $(dirname $0)
action=$1
imagename=$2
swappath="../../../../testbed/node/swap";
confdir="../../../../testbed/node/cfg";

case $1 in 
    create)
        create_image 
    ;;
    remove)
        remove_image
    ;;
    start)
        start_image
    ;;
    shutdown)
        shutdown_image
    ;;
    *)
        USAGE;
    ;;
esac

