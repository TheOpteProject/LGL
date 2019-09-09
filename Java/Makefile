
## Simple and stupid makefile

jarfiles: src/*/*.class
	jar uf jar/ImageMaker.jar src/Viewer2D/*.class src/ImageMaker/*.class
	jar uf jar/LGLLib.jar src/Viewer2D/*.class src/ImageMaker/*.class
	jar uf jar/LGLView.jar src/Viewer2D/*.class src/ImageMaker/*.class

classfiles: src/*/*.java
	javac -cp lib/Jama-1.0.3.jar src/Viewer2D/*.java src/ImageMaker/*.java


.PHONY: classfiles
