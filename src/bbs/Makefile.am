bin_PROGRAMS=hagglebbs
hagglebbs_SOURCES=hagglebbs.cpp httpd.cpp databuf.cpp mini_base64.c

hagglebbs_CPPFLAGS = \
	-I$(top_builddir)/src/libhaggle/include \
	-I$(top_builddir)/src/utils 

hagglebbs_LDFLAGS = \
	-lhaggle \
	-L$(top_builddir)/src/libhaggle/ \
	-lhaggleutils \
	-L$(top_builddir)/src/utils/ \
	-lxml2

hagglebbs_CFLAGS = \
	-std=gnu99

EXTRA_DIST = databuf.h httpd.h mini_base64.h

all-local:

clean-local:
	rm -f *~
