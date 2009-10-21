#!/bin/sh
#  Author Fredrik Bjurefors <fredrik.bjurefors@it.uu.se>
#  This script creates the xen config file as well as a swap image
#  in the proper location

USAGE() {
    echo "USAGE:"
    echo "$0 <node name>"
    echo "format: name-1"
    echo "don't pad the number"
    exit 1
}

if [ $# -lt 1 ];then
    USAGE
fi

NODENAME=$1;
cd $(dirname $0)

plate="node-xencfg.template"
tmpaddr=$(echo $NODENAME | awk -F- '{print $2}')
addr=$(($tmpaddr + 10))
destdir="../../../../testbed/node"
confdir="$destdir/cfg"
swapimg="swap.img"
swapdir="$destdir/swap"

echo "Create $NODENAME"

echo "    Creating Config File"
cat $confdir/$plate | sed "s/\$NODENAME/$NODENAME/g" | sed "s/\$IP/$addr/g" | sed "s/\$NODENO/$tmpaddr/g" > $confdir/$NODENAME

echo "    Creating Swap File"
if [ ! -e $swapdir/$NODENAME-$swapimg ];then
	if ! dd if=/dev/zero of=$swapdir/$NODENAME-$swapimg bs=1024k count=64 &>/dev/null; then
		exit 1
	fi
	chmod 640 $swapdir/$NODENAME-$swapimg
	if ! mkswap $swapdir/$NODENAME-$swapimg &>/dev/null; then
		exit 1
	fi
fi

echo "FINISHED created $NODENAME";
