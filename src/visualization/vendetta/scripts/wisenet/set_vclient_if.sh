#!/bin/sh

if [ $# -le 2 ]
then
    echo "Usage: $0 MONITOR_ADDRESS NETWORK_IF HOST1 HOST2 HOST3 ..."
    exit 1
fi

MONITOR_ADDRESS=$1
NETWORK_IF=$2

HOSTS=$*
HOSTS=`echo $HOSTS | sed 's/^[^ ]* [^ ]*//'`

echo "Updating vclient.config on hosts $HOSTS"

for HOST in $HOSTS
do
    echo "Updating $HOST "
	ssh root@$HOST "cd /var/wisenet/nodes && for NODE in *
	                do
	                    cd \$NODE
                        /opt/nodemgr/tools/generate_vclient_config $MONITOR_ADDRESS $NETWORK_IF
                        if [ -f run/vclient.pid ]
                        then
    	                    cat run/vclient.pid | xargs kill -SIGTERM 
                        fi
                        vclient --daemon
	                    cd ..
	                done"
done
