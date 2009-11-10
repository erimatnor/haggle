bin_PROGRAMS=luckyMe
luckyMe_SOURCES=luckyMe.cpp

CPPFLAGS += -I$(top_builddir)/src/libhaggle/include
LDFLAGS += -lhaggle -lpthread -L$(top_builddir)/src/libhaggle/
LDFLAGS +=-lxml2

CFLAGS += -std=gnu99

all-local:

clean-local:
	rm -f *~
