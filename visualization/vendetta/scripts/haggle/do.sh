#!/bin/bash

cd $(dirname $0)
TELNET_DIR=../../../do_telnet
if [ "$1" == "gdb" ] ; then
	cd $TELNET_DIR && make && cd - > /dev/null && gdb $TELNET_DIR/do_telnet $@
else
	cd $TELNET_DIR && make && cd - > /dev/null && $TELNET_DIR/do_telnet $@
fi
