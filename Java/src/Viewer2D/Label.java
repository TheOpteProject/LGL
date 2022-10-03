package Viewer2D;

import Jama.Matrix;

import java.awt.Color;

public class Label {
    
    
    //private Vertex id;
    public  String shape ;
    public int shapesize ;
    public int shapeborderwidth ;
    public Color shapebordercolor;

    public Color shapefillcolor ;

    public double shapefillopacity ;
    public int linesize ;
    public int linelength ;
    public double lineangle; 
    public Color linecolor;
    
    public String toptextttf;
    public int toptextsize ;
    public Color toptextcolor ;
    public Color topbgfillcolor ;

    public String bottomtextttf ;
    public int bottomtextsize ;
    public Color bottomtextcolor ;
    public Color bottombgfillcolor ;

    public String toptext;
    public String bottomtext;
    Label(
     String shape_ ,
    int shapesize_ ,
    int shapeborderwidth_ ,
    Color shapebordercolor_,

    Color shapefillcolor_ ,

    double shapefillopacity_ ,
    int linesize_ ,
    int linelength_ ,
    double lineangle_,
    Color linecolor_,
    
    String toptextttf_,
    int toptextsize_ ,
    Color toptextcolor_ ,
    Color topbgfillcolor_ ,

    String bottomtextttf_ ,
    int bottomtextsize_ ,
    Color bottomtextcolor_ ,
    Color bottombgfillcolor_ ,

    String toptext_,
    String bottomtext_)
    {
        //id=id_;
        shape = shape_;
        shapesize = shapesize_;
        shapeborderwidth = shapeborderwidth_;
        shapebordercolor = shapebordercolor_;
        shapefillcolor = shapefillcolor_;
        shapefillopacity = shapefillopacity_;
        linesize = linesize_;
        linelength = linelength_;
        lineangle = lineangle_;
        linecolor = linecolor_;
        toptextttf = toptextttf_;
        toptextsize = toptextsize_;
        toptextcolor = toptextcolor_;
        topbgfillcolor = topbgfillcolor_;
        bottomtextttf = bottomtextttf_;
        bottomtextsize = bottomtextsize_;
        bottomtextcolor = bottomtextcolor_;
        bottombgfillcolor = bottombgfillcolor_;
        toptext = toptext_;
        bottomtext = bottomtext_;

    }





}