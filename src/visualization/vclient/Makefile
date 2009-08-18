COMPILE_INFO=`id -un`@`hostname` `date`
CFLAGS+=-Wall -std=gnu99 -pedantic -Os
LDFLAGS+=

OBJS = configparser.o logevent.o logparser.o main.o network.o position.o control.o util.o log.o readln.o


all: vclient fake_monitor

vclient: $(OBJS)
	$(CC) $(OBJS) -o vclient 

%.o: %.c
	$(CC) $(CFLAGS) -DCOMPILE_INFO="\"$(COMPILE_INFO)\"" -c $<

doxygen: *.c
	doxygen docs.cfg

fake_monitor: test/fake_monitor.c
	$(CC) $(CFLAGS) test/fake_monitor.c -o fake_monitor

test: vclient
	cd test && ../vclient

debug: vclient
	cd test && gdb ../vclient

clean:
	rm -f $(OBJS) *.o *~ vclient fake_monitor
	rm -f docs/*
