#!/bin/bash

export MY_LC_ALL=$LC_ALL
export LC_ALL=C

transmission_events_cdf_plot() {
	for i in testresult/node* ; do
		cat $i/trace.log | grep "EVENT_TYPE_DATAOBJECT_RECEIVED" | awk -F: '{print $1}' >> testresult/tmp.dat
	done
	cat testresult/tmp.dat | sort -n | awk 'BEGIN{i=1;j="-"}{if(j=="-") j = $0 ; print $0-j,"	",i;i++}' > testresult/recv.dat
	rm testresult/tmp.dat
	
	for i in testresult/node* ; do
		cat $i/trace.log | grep "EVENT_TYPE_DATAOBJECT_SEND" | grep -v "EVENT_TYPE_DATAOBJECT_SEND_" | awk -F: '{print $1}' >> testresult/tmp.dat
	done
	cat testresult/tmp.dat | sort -n | awk 'BEGIN{i=1;j="-"}{if(j=="-") j = $0 ; print $0-j,"	",i;i++}' > testresult/send.dat
	rm testresult/tmp.dat
	
	for i in testresult/node* ; do
		cat $i/trace.log | grep "EVENT_TYPE_DATAOBJECT_SEND_SUCCESSFUL" | awk -F: '{print $1}' >> testresult/tmp.dat
	done
	cat testresult/tmp.dat | sort -n | awk 'BEGIN{i=1;j="-"}{if(j=="-") j = $0 ; print $0-j,"	",i;i++}' > testresult/sendsucc.dat
	rm testresult/tmp.dat
	
	for i in testresult/node* ; do
		cat $i/trace.log | grep "EVENT_TYPE_DATAOBJECT_SEND_FAILURE" | awk -F: '{print $1}' >> testresult/tmp.dat
	done
	cat testresult/tmp.dat | sort -n | awk 'BEGIN{i=1;j="-"}{if(j=="-") j = $0 ; print $0-j,"	",i;i++}' > testresult/sendfail.dat
	rm testresult/tmp.dat
	
	echo "set autoscale;" >> testresult/plot.p
	echo "set terminal pdf;" >> testresult/plot.p
	echo "set output \"report/"$transmission_events_cdf_plot_name".pdf\";" >> testresult/plot.p
	echo "set xlabel \"Time (s)\";" >> testresult/plot.p
	echo "set ylabel \"Number of events\";" >> testresult/plot.p
	echo "plot \"testresult/recv.dat\" using 1:2 title 'Receive events' with lines,\\" >> testresult/plot.p
	echo " \"testresult/send.dat\" using 1:2 title 'Send events' with lines,\\" >> testresult/plot.p
	echo " \"testresult/sendsucc.dat\" using 1:2 title 'Successful send events' with lines,\\" >> testresult/plot.p
	echo " \"testresult/sendfail.dat\" using 1:2 title 'Failed send events' with lines;" >> testresult/plot.p
	gnuplot -e "load \"testresult/plot.p\""
	
	echo "\begin{figure}[!h]"
	echo "\begin{center}"
	echo "\caption{"$caption"}"
	echo "\includegraphics[scale=0.8]{"$transmission_events_cdf_plot_name".pdf}"
	echo "\label{CDFtrans"$transmission_events_cdf_plot_name"}"
	echo "\end{center}"
	echo "\end{figure}"
}

case $1 in
	1	)
		echo "\section{2 nodes node description exchange}"
		echo "\label{2nde}"
		
		echo "This test sets up two nodes and connects them for 30 seconds."
		echo
		echo "No data objects are inserted, no program is running"
		echo "and no interests are added to either node."
		
		transmission_events_cdf_plot_name="1-1"
		caption="CDF plot of tranmission events for test \ref{2nde}"
		transmission_events_cdf_plot
	;;
	2	)
		echo "\section{3 nodes node description exchange test}"
		echo "\label{3nde}"
		
		echo "This test sets up three nodes and connects node 1 to node 2 and "
		echo "node 2 to node 3 for 30 seconds."
		echo
		echo "No data objects are inserted, no program is running"
		echo "and no interests are added to either node."
		
		transmission_events_cdf_plot_name="2-1"
		caption="CDF plot of tranmission events for test \ref{3nde}"
		transmission_events_cdf_plot
	;;
	3	)
		echo "\section{Basic 3-node forwarding test}"
		echo "\label{3nf}"
		
		echo "This test sets up three nodes and connects node 1 to node 2 and "
		echo "node 2 to node 3 for 30 seconds."
		echo
		echo "No program is running on either node, but on nodes 1 and 3, an "
		echo "interest is added, and on the third node a data object with an "
		echo "attribute matching that interest is added."
		echo
		echo "The test should result in the data object being transferred to "
		echo "the first node."
		
		transmission_events_cdf_plot_name="3-1"
		caption="CDF plot of tranmission events for test \ref{3nf}"
		transmission_events_cdf_plot
	;;
	4	)
		echo "\section{Fully connected nodes}"
		echo "\label{FCN}"
		
		echo "This test sets up 25 nodes and connects all nodes to all other "
		echo "nodes."
		echo
		echo "No data objects are inserted on, no program is running on"
		echo "and no interests are added to, any node."
		
		transmission_events_cdf_plot_name="4-1"
		caption="CDF plot of tranmission events for test \ref{FCN}"
		transmission_events_cdf_plot
	;;
	5	)
		echo "\section{Fully connected nodes 2}"
		echo "\label{FCN2}"
		
		echo "This test sets up 26 nodes and connects nodes 1 through 25 to "
		echo "each other. It then waits 1800 seconds and connects node 26 to "
		echo "nodes 1 through 25."
		echo
		echo "No data objects are inserted on, no program is running on"
		echo "and no interests are added to, any node."
		
		transmission_events_cdf_plot_name="5-1"
		caption="CDF plot of tranmission events for test \ref{FCN2}"
		transmission_events_cdf_plot
	;;
	6	)
		echo "\section{Fully connected nodes 3}"
		echo "\label{FCN3}"
		
		echo "This test sets up 25 nodes. Every 100 seconds, a new node is "
		echo "connected to all previously connected nodes. (I.e. at 0 seconds, "
		echo "no nodes are connected, at 100 seconds node 1 is connected to 0, "
		echo "at 200 seconds 2 is connected to 1 and 0, etc.)."
		echo
		echo "No data objects are inserted on, no program is running on"
		echo "and no interests are added to, any node."
		
		transmission_events_cdf_plot_name="6-1"
		caption="CDF plot of tranmission events for test \ref{FCN3}"
		transmission_events_cdf_plot
	;;
	*	)
		echo "\section{UNRECOGNIZED TEST}"
	;;
esac

export LC_ALL=$MY_LC_ALL
