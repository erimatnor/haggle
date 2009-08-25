#!/bin/bash

cd $(dirname $0)
if [ "$2" == "up" ] ; then
	ONOFF=' action="remove"'
elif [ "$2" == "down" ] ; then
	ONOFF=' action="add"'
else 
	ONOFF=''
fi

TIME=$(date +%s)
j=2
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
			(
			echo "<Haggle persistent=\"no\" create_time=\""$TIME".000000\">";
			echo "	<Attr name=\"Connectivity\">Blacklist</Attr>";
			echo "	<Connectivity>";
			echo "		<Blacklist type=\""$l"\""$ONOFF">"$i"</Blacklist>";
			echo "	</Connectivity>";
			echo "</Haggle>"
			) | ./do.sh $1 9697
		fi
	fi
done
