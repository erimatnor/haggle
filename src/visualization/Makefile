# Makefile to make things easier...

.PHONY: all

all:
	make -C siteManager/src/Forwarding
	make -C vclient
#	make -C middleware
	cd vendetta && ./compile_vendetta.sh && cd -
