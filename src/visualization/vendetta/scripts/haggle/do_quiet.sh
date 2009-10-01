#!/bin/bash

cd $(dirname $0)
TELNET_DIR=../../../do_telnet
cd $TELNET_DIR && make && cd - > /dev/null && echo -n "" | $TELNET_DIR/do_telnet $@
