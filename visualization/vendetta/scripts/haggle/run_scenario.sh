#!/bin/bash
cd $(dirname $0)
if [ ! -a scenariorunner.class ] || [ scenariorunner.java -nt scenariorunner.class ] ; then
	if ! javac scenariorunner.java ; then
		exit 1
	fi
fi
usage() {
	echo "usage:"
	echo $(basename $0)" [--help] [--nofork] [--run-with-controller <controller address>] [<scenario file>]"
	echo
	echo "--help                      Displays this help information."
	echo "--nofork                    Runs scenario, and waits for it to finish."
	echo "<scenario file>             The scenario file to use. Please use a full path."
	echo "                            If no file is specified, a dialog box will be shown "
	echo "                            to allow you to choose which scenario file to use."
	echo "--run-with-controller       "
	echo "<controller address>        "
	exit 1
}
FORK=1
CONTROLLER_IP=
OPTFILE=
next_is_controller_ip=0
for i in $@; do
	if [ $next_is_controller_ip -eq 0 ]; then
		if [ "$i" == "--help" ]; then
			usage
		elif [ "$i" == "--nofork" ] ; then
			FORK=0
		elif [ "$i" == "--run-with-controller" ] ; then
			next_is_controller_ip=1
		else
			OPTFILE=$i
		fi
	else
		CONTROLLER_IP="--run-with-controller $i"
		next_is_controller_ip=0
	fi
done

echo $CONTROLLER_IP
echo $OPTFILE
echo $FORK

if [ $next_is_controller_ip -eq 1 ] ; then
	usage
fi

if [ ! -z $OPTFILE ] && [ -a $OPTFILE ] ; then
	FILE=$OPTFILE
else
	FILE=$(./get_file.sh ./scenarios)
fi
if [ -z $FILE ] ; then
	echo "Bleh!"
	exit 1
elif [ -a $FILE ] ; then
	if [ $FORK -eq 1 ] ; then
		echo "Forking!"
		java scenariorunner $FILE $CONTROLLER_IP &
	else
		echo "Do not fork!"
		java scenariorunner $FILE $CONTROLLER_IP
	fi
fi
