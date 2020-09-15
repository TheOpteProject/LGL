JARS: 
	make -C Java jarfiles

CBINS: 
	make -C src install
	# also call the suspect perl script
	# Writing log to install_perl.log
	./setup.pl -i &> install_perl.log

all: JARS
	make -C src all

install: all CBINS

.PHONY: all install JARS CLASS CBINS