######################################

All files distributed with LGL fall under the terms of the
GNU General Public License, and are copyright (c) 2002,2003 Alex Adai.

Updated 2014 Barrett Lyon and The Opte Project Team 

LGL on the web at: http://bioinformatics.icmb.utexas.edu/lgl

Much thanks to the Marcotte lab for testing.

######################################
# Table of contents

  0 Before compiling!!
  I Setup and Installation

######################################
# 0

You must have javac and jar in your path to compile JAVA programs. If you don't the
compiles will fail. Examine the build.sh file in this directory, and modify the variable
$JAVABINPATH. This variable is only important for the compilation process.

######################################
# I

Change in to the same directory that this README is in and run ./build.sh

This should produce 3 .jar files

LGLLib.jar - The full viewer library that can produce images or do the GUI 
viewer. 


For backwards compatibility the older files are also produced:


LGLView.jar - The 2D viewer to look at layouts

ImageMaker.jar - Generates high resolution images of layouts. Run without 
arguments to get the usage. This program is very crude, and will probably 
be expanded as soon as time permits. 

The source code is there so feel free to dive in!

Numerous warnings will be produced, but it should compile.

######################################
