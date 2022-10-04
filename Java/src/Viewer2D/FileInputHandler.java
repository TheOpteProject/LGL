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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

public class FileInputHandler {
	private String text;
	private String delimeters;
	@SuppressWarnings("rawtypes")
	private Vector tokens;
	private StringTokenizer tokenizer;
	// SESS: DELETEME
	// private String filename;
	private BufferedReader bufferedreader;
	private FileReader filereader;

	private final String DELIMETERS = " \t";

	// CONSTRUCTORS
	public FileInputHandler(String filename, String delimeters)
			throws FileNotFoundException {
		this(filename);
		this.delimeters = delimeters;
	}

	public FileInputHandler(String filename) throws FileNotFoundException {
		// SESS: DELETEME
		// this.filename = filename;
		this.delimeters = DELIMETERS;
		tokens = new Vector();
		filereader = new FileReader(filename);
		bufferedreader = new BufferedReader(filereader);
	}

	// MUTATORS
	public void setDelimeters(String delimiters_) {
		this.delimeters = delimiters_;
	}

	// ACCESSORS
	public int getTokenCount() {
		return tokens.size();
	}

	public Vector getAllTokens() {
		return tokens;
	}

	public String getToken(int i) {
		return (String) tokens.elementAt(i);
	}

	public boolean readNextLine() throws IOException {
		text = bufferedreader.readLine();
		if (text == null) {
			return false;
		}
		performSplit();
		return true;
	}

	public Double getTokenAsDouble(int i) {
		try
		{
			return Double.parseDouble((String) tokens.elementAt(i));
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}

	public Integer getTokenAsInt(int i) {
		try
		{
		return Integer.parseInt((String) tokens.elementAt(i));
		}
	    catch (NumberFormatException nfe) {
			return null;
		}
	}

	public void print() {
		System.out.println(text);
	}

	// INTERNAL METHODS
	private void performSplit() {
		tokens.removeAllElements();
		if (text == null) {
			return;
		}
		tokenizer = new StringTokenizer(text, delimeters,true);

		//String[] stringArr ;
		//tokens = new Vector( Arrays.asList(text.split(delimeters)));
		boolean lastissep = false;
		while (tokenizer.hasMoreTokens()) {
			Object o = tokenizer.nextElement();
			if (((String)(o)).equals(delimeters))
			{
				tokens.addElement("");
				lastissep = true;
			}
			else
			{
				tokens.addElement(o);
				lastissep = false;
				if (tokenizer.hasMoreTokens()) 
					tokenizer.nextElement();
			}	
			  
		}
		if (lastissep)
			tokens.addElement("");
	}

}
