package com.sweetspot.shared;

import java.io.Serializable;

/**
 * This class represents the song metadata contained in a music file.
 * 
 * @author Andrew Wilder
 */
public class Metadata implements Serializable {
	private static final long serialVersionUID = -7774261187663521895L;
	public String filename;
	public String title;
	public String artist = "Unknown";
	public String album = "Unknown";
	public int length;
	public int samplerate;
	public long filesize;
	public String toString() {
		String s = filename + ": [";
		s += "filesize:" + filesize + "bytes,";
		s += "title:" + title + ",";
		s += "artist:" + artist + ",";
		s += "album:" + album + ",";
		s += "length:" + length + "sec,";
		s += "samplerate:" + samplerate + "Hz]";
		return s;
	}
}
