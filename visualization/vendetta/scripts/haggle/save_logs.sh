#!/bin/sh

USAGE() {
    echo USAGE:
    echo "$0 <number of nodes> <scenario>"
    exit 1
}

if [ $# -lt 2 ]; then 
    USAGE
fi

cd $(dirname $0)

nodeCount=$1
scenario=$2
tarball=$scenario"-"$(date +"%s")".tar"
cmd="tar -rf "$tarball
#Add execution time log

tarcmd=$cmd" *.log"
$tarcmd

cmd=$cmd" -C ../../nodelogs/"
for((i = 0; $i < $nodeCount; i++)); do
	$cmd "node-"$i
done
gzip $tarball
mv *.tar.gz ../../logs/

