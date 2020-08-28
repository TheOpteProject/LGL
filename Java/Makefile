
## Simple and stupid makefile

jarfiles: src/*/*.class
	jar uf jar/ImageMaker.jar -C src .
	jar uf jar/LGLLib.jar src/Viewer2D/*.class src/ImageMaker/*.class 
	jar uf jar/LGLView.jar src/Viewer2D/*.class src/ImageMaker/*.class

%.class : %.java
	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar src/Viewer2D/*.java src/ImageMaker/*.java	

classfiles: src/*/*.java
	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar src/Viewer2D/*.java src/ImageMaker/*.java


.PHONY: classfiles
