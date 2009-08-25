#!/bin/sh

HOST=$1
NODE=$2

scp demo/contiki/ibc-energest.ch25.ihex root@$HOST:/var/wisenet/nodes/$NODE/imgs/
ssh root@$HOST "cd /var/wisenet/nodes/$NODE/ && ./program imgs/ibc-energest.ch25.ihex"
