
## Simple and stupid makefile

jarfiles: src/*/*.class
	jar uf jar/ImageMaker.jar -C src .
	jar uf jar/LGLLib.jar -C src . 
	jar uf jar/LGLView.jar -C src . 

%.class : %.java
	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar src/Viewer2D/*.java src/ImageMaker/*.java	

classfiles: src/*/*.java
	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar src/Viewer2D/*.java src/ImageMaker/*.java


.PHONY: classfiles
