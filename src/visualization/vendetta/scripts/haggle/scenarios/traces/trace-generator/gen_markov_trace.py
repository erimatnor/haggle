#!/usr/bin/env python
# usage: python gen_markov_trace.py <number-of-nodes> <run-time [s]> <output-file>

import sys
import datetime
import os
import operator
import array
import random

if len(sys.argv) != 6:
	print "usage: python " + sys.argv[0] + " <number-of-nodes> <run-time [s]> <evolution-speed> <density> <output-file>"
	sys.exit(1)

number_of_nodes = int(sys.argv[1])
run_time = int(sys.argv[2])
evolution_speed = int(sys.argv[3])
density = int(sys.argv[4])
out = file(sys.argv[5], "w")

first_node_index = 1

q_c = (evolution_speed - 1.0)/evolution_speed
q_i = 1.0 - (1.0/(evolution_speed*density)) 

#out.write("q_i" + str(q_i) + "\n")
#out.write("q_c" + str(q_c) + "\n")

#out.write("r" + str(1/(1-q_c)) + "\n")
#out.write("l" + str((1-q_c)/(1-q_i)) + "\n")


# init linkState
linkState = []
for i in range(number_of_nodes):
	linkState.append([])
	for j in range(number_of_nodes):
		linkState[i].append(0)

# trace
for t in range(run_time):
	for i in range(number_of_nodes):
		for j in range(i):
			if (linkState[i][j] == 0):
				if (random.random() > q_i):
					linkState[i][j] = t
			else:
				if (random.random() > q_c):
					out.write(str(i+first_node_index) + "\t" + str(j+first_node_index) + "\t" + str(linkState[i][j]) + "\t" + str(t) + "\n")
					out.write(str(j+first_node_index) + "\t" + str(i+first_node_index) + "\t" + str(linkState[i][j]) + "\t" + str(t) + "\n")
					linkState[i][j] = 0

# take down all links
for i in range(number_of_nodes):
	for j in range(i):
		if (linkState[i][j] > 0):
			out.write(str(i+first_node_index) + "\t" + str(j+first_node_index) + "\t" + str(linkState[i][j]) + "\t" + str(run_time) + "\n")
			out.write(str(j+first_node_index) + "\t" + str(i+first_node_index) + "\t" + str(linkState[i][j]) + "\t" + str(run_time) + "\n")
			linkState[i][j] = 0

out.close()
