#!/bin/sh
# Set this java path to your compiler
# You will probably see several warnings
# during the compilation.
export JAVABINPATH=/usr/java/jdk1.5.0_09/bin
echo "----- Compiling Imagemaker -----"
cd ImageMaker
make clean
make
cd ..
jar cmf ImageMaker/META-INF/MANIFEST.MF imageMaker.jar Viewer2D Jama ImageMaker
echo "----- Compiling LGLView -----"
cd Viewer2D
make clean
make
cd ..
jar cmf Viewer2D/META-INF/MANIFEST.MF lglview.jar Viewer2D Jama
echo "There should be 2 .jar files in this directory"
