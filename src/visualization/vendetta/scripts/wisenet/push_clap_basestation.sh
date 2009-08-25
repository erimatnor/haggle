#!/bin/sh

HOST=$1
NODE=$2
NODE_ID=$3

scripts/wisenet/set_symbols_and_copy $NODE_ID demo/tinyos/basestation.ch25.ihex root@$HOST:/var/wisenet/nodes/$NODE/imgs/
ssh root@$HOST "cd /var/wisenet/nodes/$NODE/ && ./program imgs/basestation.ch25.ihex"
