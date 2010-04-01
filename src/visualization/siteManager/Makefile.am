noinst_JAVA = \
	src/Forwarding/Forward.java \
	src/Forwarding/HandleVclient.java \
	src/Forwarding/Main.java \
	src/Forwarding/Monitor.java \
	src/Forwarding/MonitorManager.java \
	src/Forwarding/Packet.java \
	src/Forwarding/TCPDown.java \
	src/Forwarding/UDPDown.java \
	src/Forwarding/UDPForward.java \
	src/Forwarding/VDown.java \
	src/Forwarding/handleMonitor.java

EXTRA_DIST= $(noinst_JAVA)

JAVAROOT=classes

AM_JAVACFLAGS=

clean-local:
	rm -rf classes/*
	rm -f *~
	rm -f *.stamp
