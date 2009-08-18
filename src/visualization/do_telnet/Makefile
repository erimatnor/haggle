TELNET_SOURCES=do_telnet.cpp net.cpp
TELNET_OBJS=$(subst .cpp,.o,$(TELNET_SOURCES))

all: do_telnet

do_telnet: $(TELNET_OBJS)
	gcc -o $@ $+ -lpthread -lstdc++
 
clean:
	rm -f *.o

.cpp.o:
	gcc -c -o $@ $< -Wall -Werror