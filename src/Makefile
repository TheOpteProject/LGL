CPP=c++

#CPP_FLAGS=-Wall -ftemplate-depth-30
CPP_FLAGS=-Wall -O3 -ftemplate-depth-30

SOURCES= lglayout.C calcFuncs.C
OBJECTS= lglayout.o calcFuncs.o

LIBS=-lm -lpthread

INCPATH=-I../include -I../boost-1.30.2
#LIBPATH=-L../lib

all: lglayout lglbreakup lglrebuild

lglayout: $(OBJECTS)
	$(CPP) -o lglayout $(OBJECTS) $(LIBPATH) $(LIBS)

lglbreakup: lglbreakup.o
	$(CPP) -o lglbreakup lglbreakup.o $(LIBPATH) $(LIBS)

lglrebuild: lglrebuild.o
	$(CPP) -o lglrebuild lglrebuild.o $(LIBPATH) $(LIBS)

install:
	mv -f lglrebuild lglbreakup lglayout ../bin/.

.C.o:
	$(CPP) $(INCPATH) -c $(CPP_FLAGS) $<

clean: 
	rm -f *.o

tidy:
	rm -f *o core lglayout lglbreakup lglrebuild
