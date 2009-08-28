#!/bin/bash

PATH_TO_THIS_DIR=`dirname $0`

pushd $PATH_TO_THIS_DIR
./run_vendetta.sh --config-path configs/haggletestbed/
popd
