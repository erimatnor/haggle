#!/usr/bin/env python
# usage: python gen_ring_trace.py <number-of-nodes> <run-time [s]> <output-file>

import sys
import datetime
import os
import operator

if len(sys.argv) != 4:
	print "usage: python " + sys.argv[0] + " <number-of-nodes> <run-time [s]> <output-file>"
	sys.exit(1)

number_of_nodes = int(sys.argv[1])
run_time = int(sys.argv[2])
out = file(sys.argv[3], "w")

first_node_index = 1
last_node_index = first_node_index + number_of_nodes - 1

for i in range(first_node_index, last_node_index + 1):
	if (i == first_node_index):
		out.write(str(i) + "\t" + str(last_node_index) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		out.write(str(i) + "\t" + str(i + 1) + "\t" + str(0) + "\t" + str(run_time) + "\n")
	elif (i == last_node_index):
		out.write(str(i) + "\t" + str(i - 1) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		out.write(str(i) + "\t" + str(first_node_index) + "\t" + str(0) + "\t" + str(run_time) + "\n")
	else:
		out.write(str(i) + "\t" + str(i - 1) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		out.write(str(i) + "\t" + str(i + 1) + "\t" + str(0) + "\t" + str(run_time) + "\n")

out.close()
