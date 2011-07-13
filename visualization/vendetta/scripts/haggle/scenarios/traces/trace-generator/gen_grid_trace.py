#!/usr/bin/env python
# usage: python gen_grid_trace.py <number-of-nodes-x> <number-of-nodes-y> <run-time [s]> <output-file>

import sys
import datetime
import os
import operator

if len(sys.argv) != 5:
	print "usage: python " + sys.argv[0] + " <number-of-nodes-x> <number-of-nodes-y> <run-time [s]> <output-file>"
	sys.exit(1)

number_of_nodes_x = int(sys.argv[1])
number_of_nodes_y = int(sys.argv[2])
run_time = int(sys.argv[3])
out = file(sys.argv[4], "w")

first_node_index_x = 1
last_node_index_x = first_node_index_x + number_of_nodes_x - 1

for j in range(0, number_of_nodes_y):

	adder = (j * number_of_nodes_x)
	
	for i in range(first_node_index_x, last_node_index_x + 1):
		# First, we do the connectivity in the x direction
		if (i == first_node_index_x):
			out.write(str(i + adder) + "\t" + str(i + 1 + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		elif (i == last_node_index_x):
			out.write(str(i + adder) + "\t" + str(i - 1 + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		else:
			out.write(str(i + adder) + "\t" + str(i - 1 + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
			out.write(str(i + adder) + "\t" + str(i + 1 + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")

		# Then, we take the y direction
		if (j == 0):
			out.write(str(i + adder) + "\t" + str(i + number_of_nodes_x + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		elif (j == (number_of_nodes_y - 1)):
			out.write(str(i + adder) + "\t" + str(i - number_of_nodes_x + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
		else:
			out.write(str(i + adder) + "\t" + str(i - number_of_nodes_x + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")
			out.write(str(i + adder) + "\t" + str(i + number_of_nodes_x + adder) + "\t" + str(0) + "\t" + str(run_time) + "\n")

out.close()
