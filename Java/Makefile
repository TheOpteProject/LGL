
## Simple and stupid makefile

JAVAS=src/ImageMaker/GenerateImages.java src/Viewer2D/Edge.java src/Viewer2D/EdgesFrame.java src/Viewer2D/EdgesPanel.java src/Viewer2D/FileInputHandler.java src/Viewer2D/FindEdgesFrame.java src/Viewer2D/FindVertexFrame.java src/Viewer2D/FormatVertex.java src/Viewer2D/ManipVertexArray.java src/Viewer2D/SaveResults.java src/Viewer2D/Transformer.java src/Viewer2D/VertexDescripTable.java src/Viewer2D/VertexFitter.java src/Viewer2D/Vertex.java src/Viewer2D/VertexStats.java src/Viewer2D/Viewer2D.java src/Viewer2D/ViewerIO.java
CLASS=src/ImageMaker/GenerateImages.class src/Viewer2D/Edge.class src/Viewer2D/EdgesFrame.class src/Viewer2D/EdgesPanel.class src/Viewer2D/FileInputHandler.class src/Viewer2D/FindEdgesFrame.class src/Viewer2D/FindVertexFrame.class src/Viewer2D/FormatVertex.class src/Viewer2D/ManipVertexArray.class src/Viewer2D/SaveResults.class src/Viewer2D/Transformer.class src/Viewer2D/VertexDescripTable.class src/Viewer2D/VertexFitter.class src/Viewer2D/Vertex.class src/Viewer2D/VertexStats.class src/Viewer2D/Viewer2D.class src/Viewer2D/ViewerIO.class

jar/%.jar: $(CLASS)
	jar uf $@ -C src .

jarfiles: jar/ImageMaker.jar jar/LGLLib.jar jar/LGLView.jar

%.class : %.java
	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar -cp src src/Viewer2D/*.java src/ImageMaker/*.java	

all: $(CLASS)

.PHONY: jarfiles

# Keeping for posterity
#%classfiles: src/ImageMaker/GenerateImages.java src/Viewer2D/Edge.java src/Viewer2D/EdgesFrame.java src/Viewer2D/EdgesPanel.java src/Viewer2D/FileInputHandler.java src/Viewer2D/FindEdgesFrame.java src/Viewer2D/FindVertexFrame.java src/Viewer2D/FormatVertex.java src/Viewer2D/ManipVertexArray.java src/Viewer2D/SaveResults.java src/Viewer2D/Transformer.java src/Viewer2D/VertexDescripTable.java src/Viewer2D/VertexFitter.java src/Viewer2D/Vertex.java src/Viewer2D/VertexStats.java src/Viewer2D/Viewer2D.java src/Viewer2D/ViewerIO.java
#	javac -cp lib/Jama-1.0.3.jar:lib/VectorGraphics2D-0.13.jar src/Viewer2D/*.java src/ImageMaker/*.java
#.PHONY: classfiles jarfiles
