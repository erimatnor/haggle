.PHONY: test testmetadata

HAGGLE_KERNEL_DIR=$(top_srcdir)/src/hagglekernel/
UTILS_DIR=$(top_srcdir)/src/utils/
LIBCPPHAGGLE_DIR=$(top_srcdir)/src/libcpphaggle/
AM_CPPFLAGS = -I$(HAGGLE_KERNEL_DIR) -I$(UTILS_DIR) -I$(LIBCPPHAGGLE_DIR)include/ -I.. $(XML_CPPFLAGS)
AM_LDFLAGS = $(XML_LIBS) -lcrypto

if OS_LINUX
AM_LDFLAGS += -lpthread
endif

bin_PROGRAMS=certificate

STDDEPS=$(HAGGLE_KERNEL_DIR)libhagglekernel.a
STDDEPS+=$(UTILS_DIR)libhaggleutils.a
STDDEPS+=../libtesthlp.a

certificate_SOURCES=certificate.cpp
certificate_DEPENDENCIES=$(STDDEPS)

LDADD=$(HAGGLE_KERNEL_DIR)libhagglekernel.a 
LDADD+=$(UTILS_DIR)libhaggleutils.a
LDADD+=$(LIBCPPHAGGLE_DIR)libcpphaggle.a
LDADD+=../libtesthlp.a

if OS_MACOSX
AM_LDFLAGS += -framework IOKit -framework CoreFoundation -framework CoreServices
endif

test: testcertificate

testcertificate: certificate
	@./certificate && echo "Passed!" || echo "Failed!"

all-local:

clean-local:
	rm -f *~ *.o
