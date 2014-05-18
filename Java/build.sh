#!/bin/sh
echo "----- Compiling Imagemaker -----"
cd ImageMaker
make
cd ..
jar cmf ImageMaker/META-INF/MANIFEST.MF imageMaker.jar Viewer2D Jama ImageMaker
echo "----- Compiling LGLView -----"
cd Viewer2D
make
cd ..
jar cmf Viewer2D/META-INF/MANIFEST.MF lglview.jar Viewer2D Jama
echo "There should be 2 .jar files in this directory"
