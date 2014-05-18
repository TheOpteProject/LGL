//  
//  Copyright (c) 2003 Alex Adai, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  

package Viewer2D;

import java.util.*;
import java.io.*;
import java.util.HashMap;

public class FileInputHandler
{
    private String text;
    private String delimeters;
    private Vector tokens;
    private StringTokenizer tokenizer;
    private String filename;
    private BufferedReader bufferedreader;
    private FileReader filereader;

    private final String DELIMETERS = " \t";

    // CONSTRUCTORS
    public FileInputHandler( String filename , String delimeters ) throws
	FileNotFoundException
    {
	this(filename);
	this.delimeters = delimeters;
    }

    public FileInputHandler( String filename ) throws 
	FileNotFoundException
    {
	this.filename = filename;
	this.delimeters = DELIMETERS;
	tokens = new Vector();
	filereader = new FileReader(filename);
	bufferedreader = new BufferedReader(filereader);
    }	

    // MUTATORS
    public void setDelimeters( String delimiters ) {
	this.delimeters = delimeters;
    }

    // ACCESSORS
    public int getTokenCount() { return tokens.size(); }

    public Vector getAllTokens() { return tokens; }

    public String getToken( int i ) { 
	return (String) tokens.elementAt(i); 
    }

    public boolean readNextLine() throws IOException {
	text = bufferedreader.readLine();
	if ( text == null ) { return false; }
	performSplit();
	return true;
    }

    public double getTokenAsDouble( int i ) {
	return Double.parseDouble( (String) tokens.elementAt(i) );
    }

    public int getTokenAsInt( int i ) {
	return Integer.parseInt( (String) tokens.elementAt(i) );
    }

    public void print() {
	System.out.println(text);
    }

    // INTERNAL METHODS
    private void performSplit() {
	tokens.removeAllElements();
	if ( text == null ) { return; }
	tokenizer = new StringTokenizer( text , delimeters );
	while ( tokenizer.hasMoreTokens() ) {
	    tokens.addElement( tokenizer.nextElement() );
	}
    }

}
