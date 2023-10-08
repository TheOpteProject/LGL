package ImageMaker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import Viewer2D.EdgesPanel;
import Viewer2D.FormatVertex;
import Viewer2D.ViewerIO;

public class GenerateImages {
    
    public static boolean GENERATE_IN_SEPARATE_THREAD = true;
    
    /**
     * Entry point.
     * 
     * @param args
     */
    public static void main(String[] args) {
        ParseArguments pa = new ParseArguments(false);
        pa.parse(args);
        
        printStats(pa);
        
        GENERATE_IN_SEPARATE_THREAD = true;
        
        generate(pa, "dark with labels", "dark_withlabels", Color.BLACK, 
                true /*scale*/, true /*printLabels*/, true /*useAlignmentCenterArg*/);
        generate(pa, "dark without labels", "dark_nolabels", Color.BLACK,
                true /*scale*/, false /*printLabels*/, true /*useAlignmentCenterArg*/);
        
        generate(pa, "dark without scale without labels", "dark_withoutscale_withoutlabels", Color.BLACK,
                false /*scale*/, false /*printLabels*/, false /*useAlignmentCenterArg*/);
        generate(pa, "dark without scale with labels", "dark_withoutscale_withlabels", Color.BLACK,
                false /*scale*/, true /*printLabels*/, false /*useAlignmentCenterArg*/);
        
        generate(pa, "light without scale without labels", "light_withoutscale_withoutlabels", Color.WHITE,
                false /*scale*/, false /*printLabels*/, false /*useAlignmentCenterArg*/);
        generate(pa, "transparent without scale without labels", "transparent_withoutscale_withoutlabels", new Color(0f, 0f, 0f, 0f),
                false /*scale*/, false /*printLabels*/, false /*useAlignmentCenterArg*/);
    }
    
    private static void printStats(ParseArguments pa) {
        try {
            ViewerIO v = createViewerIO(pa);
            v.loadVertexCoords(new File(pa.coordFiles.get(0)));
            v.getStats().print();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static ViewerIO createViewerIO(ParseArguments pa) {
        ViewerIO verterIO = null;
        try {
            verterIO = new ViewerIO(new File(pa.edgeFile));
            verterIO.loadSHORTFile();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        verterIO.setLabelScale(pa.scaling);

        verterIO.setMinMaxXY(pa.minX, pa.minY, pa.maxX, pa.maxY);

        try {
            verterIO.loadEdgeColorFile(new File(pa.edgeColorFile));
        } catch (java.io.FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        return verterIO;
    }
    
    private static void generate(ParseArguments pa, String displayname, String name, Color background,
            boolean scale, boolean printLabels, boolean useAlignmentCenterArg) {
        
        ViewerIO verterIO = createViewerIO(pa);
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                generate(displayname, name, background, !verterIO.getEdgeColorMap().isEmpty(), pa.windowSizes, pa.coordFiles,
                        printLabels ? pa.labelFile : "", useAlignmentCenterArg ? pa.alignmentCenter : false, verterIO, scale);
            }
        };
        
        if (GENERATE_IN_SEPARATE_THREAD) {
            new Thread(task).start();
        }
        else {
            task.run();
        }
    }

    private static void generate(String displayname, String name, Color background, boolean loadedEdgeColors,
            int[] windowSizes, List<String> coordFiles, String labelFile, boolean alignmentCenter, ViewerIO verterIO, boolean scale) {
        if (!labelFile.isEmpty())
            loadLabels(labelFile, verterIO);
        else
            verterIO.clearLabels();

        for (String coordFile : coordFiles) {
            try {
                verterIO.loadVertexCoords(new File(coordFile));

                FormatVertex formatter = new FormatVertex(verterIO.getVertices(), verterIO.getLabels(),
                        verterIO.getLabelScale(), scale ? verterIO.getMinX() : 0, scale ? verterIO.getMinY() : 0,
                        scale ? verterIO.getMaxX() : 0, scale ? verterIO.getMaxY() : 0, alignmentCenter,
                        verterIO.getStats(), windowSizes, 1);

                EdgesPanel panel = new EdgesPanel(verterIO.getEdges(), verterIO.getVertices(), verterIO.getLabels(),
                        windowSizes[0], windowSizes[1]);

                if (loadedEdgeColors)
                    panel.addEdgeColors(verterIO.getEdgeColorMap());

                panel.showVertices(true);
                panel.setVisibilityTest(true);
                panel.setFormatter(formatter);
                panel.setEdgeColor(EDGE_COLOR);
                panel.setVertexColor(Color.white);
                panel.setBackgroundColor(background);

                BufferedImage bufferedImage = new BufferedImage(windowSizes[0], windowSizes[1],
                        BufferedImage.TYPE_INT_ARGB);

                // Now the image has to be fitted to the given region
                panel.fitData();
                String pngFile = MessageFormat.format("{0}_{1,number,0}x{2,number,0}_" + name + ".png", coordFile,
                        windowSizes[0], windowSizes[1]);
                panel.writeImage(pngFile, bufferedImage);
                System.out.println("Done.");
            } catch (IOException e) {
                System.out.println(MessageFormat.format("Error processing {0}:\n{1}", e.getMessage()));
            }
        }
    }

    static void loadLabels(String labelFile, ViewerIO verterIO) {
        if (labelFile.isEmpty()) return;
        System.out.println("Loading label file: " + labelFile + "...");
        try {
            verterIO.loadLabelFile(new File(labelFile));
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Labels loading complete.");
    }
    
    public static class ParseArguments {

        public String edgeFile;
        public String edgeColorFile;
        public String labelFile;
        public double scaling;
        public double minX;
        public double maxX;
        public double minY;
        public double maxY;
        int[] windowSizes;
        boolean alignmentCenter;
        public List<String> coordFiles;
        private boolean viewer2d;

        public ParseArguments(boolean viewer2d) {
            this.viewer2d = viewer2d;

        }

        void usage() {
            if (viewer2d)
                message2();
            else
                message();

        }

        public void parse(String[] args) {
            int minArguments = 3;

            if (viewer2d) {
                minArguments = 1;
                if (args.length == 0)
                    return;
            }

            if (args.length < minArguments) {
                usage();
            }
            int argno = 0;
            if (!viewer2d) {
                windowSizes = new int[2];
                windowSizes[0] = Integer.parseInt(args[0]);
                windowSizes[1] = Integer.parseInt(args[1]);
                argno += 2;
                System.out.println("Image size is " + windowSizes[0] + " x " + windowSizes[1]);
            }

            // Check params
            edgeFile = args[argno++];
            edgeColorFile = "";
            coordFiles = new ArrayList<String>();
            labelFile = "";
            boolean colorFileSwitch = false;
            boolean labelFileSwitch = false;
            boolean scaleSwitch = false;
            scaling = 1;

            boolean wasMin = false;
            boolean wasMax = false;
            boolean minSwitch = false;
            boolean maxSwitch = false;
            minX = 0;
            maxX = 0;
            minY = 0;
            maxY = 0;

            alignmentCenter = false;
            boolean alignSwitch = false;
            for (int i = argno; i < args.length; i++) {
                String arg = args[i];
                if ("-c".equals(arg)) {
                    colorFileSwitch = true;
                    continue;
                }
                if ("-l".equals(arg)) {
                    labelFileSwitch = true;
                    continue;
                }
                if ("-s".equals(arg)) {
                    scaleSwitch = true;
                    continue;
                }
                if ("-m".equals(arg) && !viewer2d) {
                    minSwitch = true;
                    continue;
                }
                if ("-a".equals(arg) && !viewer2d) {
                    alignSwitch = true;
                    continue;
                }
                if ("-M".equals(arg) && !viewer2d) {
                    maxSwitch = true;
                    continue;
                }
                if (scaleSwitch) {
                    scaleSwitch = false;
                    scaling = Double.parseDouble(arg);
                    continue;
                }
                if (minSwitch) {
                    minSwitch = false;
                    wasMin = true;
                    String[] a = arg.split(",");
                    if (a.length != 2) {
                        System.out.println("Error:-m requires exactly 2 coordinates");
                        System.exit(1);

                    }
                    minX = Double.parseDouble(a[0]);
                    minY = Double.parseDouble(a[1]);
                    continue;
                }
                if (maxSwitch) {
                    maxSwitch = false;
                    wasMax = true;
                    String[] a = arg.split(",");
                    if (a.length != 2) {
                        System.out.println("Error:-M requires exactly 2 coordinates");
                        System.exit(1);
                    }
                    maxX = Double.parseDouble(a[0]);
                    maxY = Double.parseDouble(a[1]);
                    continue;
                }
                if (alignSwitch) {
                    alignSwitch = false;
                    if (arg.equals("center"))
                        alignmentCenter = true;
                    continue;
                }
                if (labelFileSwitch) {
                    labelFile = arg;
                    labelFileSwitch = false;

                    continue;
                }
                if (colorFileSwitch) {
                    edgeColorFile = arg;
                    colorFileSwitch = false;
                    continue;
                }
                coordFiles.add(arg);
            }
            if (coordFiles.isEmpty()) {
                usage();
            }

            if (wasMax ^ wasMin) {
                System.out.println("Error:Both -m and -M need to be used at the same time, one of them is missing");
                System.exit(1);
            }
        }
    }

    public static Color EDGE_COLOR = Color.white;
    public static String EDGE_COLOR_FILE = "color_file";
    
    public static void message() {
        System.out.println("Arguments:\n\n"
                + "\t<width> <height> <edges file> <coords file1> <coords file2>... [-c <colors file> ] [-l <labels file>] [-m minx,miny -M -maxx,maxy] [-a center]\n\n"
                + "If no colors file specified program will try to load file named \"" + EDGE_COLOR_FILE + "\".\n"
                + "By default edges are white. flindeberg mod");
        System.exit(1);
    }
    
    public static void message2() {
        System.out.println("Arguments:\n\n" + "\t<edges file> <coords file1> [-c <colors file> ] [-l <labels file>]\n\n"
                + "If no colors file specified program will try to load file named \"" + EDGE_COLOR_FILE + "\".\n");
        System.exit(1);
    }
}