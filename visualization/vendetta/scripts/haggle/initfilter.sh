#!/bin/sh
# This script initializes the filtering before a testrun.
USAGE() {
    echo USAGE:
    echo "$0"
    exit 1
}

if [ $# -lt 0 ];then 
    USAGE
fi

# Get IP and netmask of xenbr0.
IFNAME=xenbr0
IP=`/sbin/ifconfig $IFNAME | grep "inet addr"`
NM=`/sbin/ifconfig $IFNAME | grep "Mask"`
IP=${IP##*inet addr:}
IP=${IP%%  B*}
NM=${NM##*:}

# Flush the table.
sudo iptables -F
# Allow traffic from host (gateway) to anywhere.
sudo iptables -A FORWARD --source $IP -j ACCEPT
# Allow traffic to the host (gateway) from anywhere.
sudo iptables -A FORWARD --destination $IP -j ACCEPT
# Drop all traffic going between virtual nodes.
sudo iptables -A FORWARD --source $IP/$NM --destination $IP/$NM -j DROP
