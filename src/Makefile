CPP=c++

CPP_FLAGS=-std=c++11 -Wall -O3 -ftemplate-depth=100 -g -DBOOST_ALLOW_DEPRECATED_HEADERS

SOURCES= lglayout.C calcFuncs.C
OBJECTS= lglayout.o calcFuncs.o

LIBS=-lm -lpthread

INCPATH=-I../include

all: lglayout lglbreakup lglrebuild lglfileconvert

lglayout: $(OBJECTS)
	$(CPP) -o lglayout $(OBJECTS) $(LIBPATH) $(LIBS)

lglbreakup: lglbreakup.o
	$(CPP) -o lglbreakup lglbreakup.o $(LIBPATH) $(LIBS)

lglrebuild: lglrebuild.o calcFuncs.o
	$(CPP) -o lglrebuild lglrebuild.o calcFuncs.o $(LIBPATH) $(LIBS)

lglfileconvert: lglfileconvert.o
	$(CPP) -o lglfileconvert lglfileconvert.o $(LIBPATH) $(LIBS)

install: all
	cp -f lglrebuild lglbreakup lglayout lglfileconvert ../bin/.

.C.o:
	$(CPP) $(INCPATH) -c $(CPP_FLAGS) $<

clean: 
	rm -f *.o

tidy:
	rm -f *.o core lglayout lglbreakup lglrebuild lglfileconvert

.PHONY: all clean tidy
