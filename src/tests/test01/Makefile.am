bin_PROGRAMS=test1
test1_SOURCES=test1.cpp

AM_CPPFLAGS = \
	-I$(top_builddir)/src/libhaggle/include \
	-I$(top_builddir)/src/utils \
	-I$(top_builddir)/src/libcpphaggle 

AM_LDFLAGS = \
	-lhaggle \
	-L$(top_builddir)/src/libhaggle/ \
	-lhaggleutils \
	-L$(top_builddir)/src/utils/ \
	-lcpphaggle \
	-L$(top_builddir)/src/libcpphaggle \
	-lxml2

all-local:

clean-local:
	rm -f *~
