bin_PROGRAMS=vclient fake_monitor
vclient_SOURCES=configparser.c logevent.c logparser.c main.c network.c position.c control.c  util.c log.c readln.c
fake_monitor_SOURCES=test/fake_monitor.c

EXTRA_DIST=configparser.h control.h globals.h log.h logevent.h logparser.h network.h position.h readln.h  util.h

CFLAGS +=-Wall -std=gnu99 -pedantic -Os

all-local:

clean-local:
	rm -f *~
