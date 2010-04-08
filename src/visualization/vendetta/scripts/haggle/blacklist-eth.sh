#!/bin/bash

# Script to blacklist mac addresses on Haggle nodes.

function usage() {
    echo "Usage: $0 IP [ACTION] TYPE MAC [TYPE MAC [...]]"
    echo "   IP is the IP address of the target node," 
    echo "   ACTION is either 'up' or 'down', (no action means toggle),"
    echo "   TYPE is the type of interface (only ethernet accepted)"
    echo "   MAC is the mac address to blacklist."
    exit;
}

if [ ${#@} -lt 3 ]; then
    usage;
fi

cd $(dirname $0)

if [ "$2" == "up" ] ; then
	ONOFF=' action="remove"'
	j=2
elif [ "$2" == "down" ] ; then
	ONOFF=' action="add"'
	j=2
else 
	ONOFF=''
	j=1
fi

TIME=$(date +%s)
k=0
l=
for i in $@ ; do
	if [ $j -ne "0" ] ; then
		j=$(echo $j-1 | bc)
	else
		if [ $k -ne "1" ] ; then
			l=$i
			k=1
		else
			k=0
			if [ "$l" == "ethernet" ] ; then
				(
					echo "<Haggle persistent=\"no\" create_time=\""$TIME".000000\">";
					echo "	<Attr name=\"Connectivity\">Blacklist</Attr>";
					echo "	<ConnectivityManager>";
					echo "		<Blacklist type=\""$l"\""$ONOFF">"$i"</Blacklist>";
					echo "	</ConnectivityManager>";
					echo "</Haggle>"
				) | ./do.sh $1 9697
			fi
		fi
	fi
done
