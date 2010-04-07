#!/bin/bash

i=0
while [ true ]; do 
#    tail -f appin;
#    cat appin;
	i=`expr $i + 1`
	echo "foobar $i"
	echo "foobar $i, is what i said"
    sleep 0.001
done
