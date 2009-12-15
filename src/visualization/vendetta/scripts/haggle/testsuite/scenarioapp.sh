#!/bin/bash

USAGE() {
	echo
    echo "USAGE:"
    echo "$0 <scenario>"
	echo
    echo "<scenario> a number indicating which scenario (1 through 2) to run"
    exit 1
}

if [ $# -lt 1 ];then 
	echo "Too few parameters"
    USAGE
fi

# Step 1: Identify which node the script is running on.
# FIXME: identify!
NODE=$(hostname | awk -F- '{print $2}')

# Step 2: Identify which scenario the script should run.
SCENARIO=$1

# Step 3: Run the scenario for this node:
case $SCENARIO in
	1	)
		# Scenario 1: 2 nodes, no action from the program on either node.
		case $NODE in 
			0	)
			;;
			1	)
			;;
			*	)
				echo "Unrecognized node for scenario "$SCENARIO"!" > ~/.Haggle/scenarioerror.txt
				echo $0 $@ >> ~/.Haggle/scenarioerror.txt
				USAGE
			;;
		esac
	;;
	2	)
		# Scenario 2: 3 nodes, no action from the program on either node.
		case $NODE in 
			0	)
			;;
			1	)
			;;
			2	)
			;;
			*	)
				echo "Unrecognized node for scenario "$SCENARIO"!" > ~/.Haggle/scenarioerror.txt
				echo $0 $@ >> ~/.Haggle/scenarioerror.txt
				USAGE
			;;
		esac
	;;
	3	)
		# Scenario 3: 3 nodes
		case $NODE in 
			0	)
				# Node 1 has interest "Picture=banana"
				clitool add Picture=banana
			;;
			1	)
				# Node 2 has nothing
			;;
			2	)
				# Node 3 has interest "Picture=banana"...
				clitool add Picture=banana
				# ... and a data object with that attribute
				clitool new Picture=banana
			;;
			*	)
				echo "Unrecognized node for scenario "$SCENARIO"!" > ~/.Haggle/scenarioerror.txt
				echo $0 $@ >> ~/.Haggle/scenarioerror.txt
				USAGE
			;;
		esac
	;;
	*	)
		echo "Scenario ("$SCENARIO") out of bounds." > ~/.Haggle/scenarioerror.txt
		echo $0 $@ >> ~/.Haggle/scenarioerror.txt
		USAGE
	;;
esac
