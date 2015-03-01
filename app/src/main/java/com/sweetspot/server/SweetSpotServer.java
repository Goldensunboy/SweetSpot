package com.sweetspot.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ServerSocketFactory;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.sweetspot.shared.Metadata;
import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Definitions.TransactionType;

/**
 * This is the server for SweetSpot. The server will run on a computer, and
 * make music files within the folder it is executed in, visible to the
 * SweetSpot app.
 * 
 * @author Andrew Wilder, James Roberts
 * @version 1.0
 */
public class SweetSpotServer {

    /** Default variables */
    private static final String DEFAULT_DIR = ".";
	
	/** Global variables used by the server */
	private static File music_dir;
	private static HashMap<String, Metadata> file_map;
	
	/**
	 * Parse arguments and determine non-default setting for the server
	 * 
	 * @param args Arguments to the server
	 */
	public static void main(String[] args) {
		
		// Set the program defaults
		int port = Definitions.DEFAULT_PORT;
		music_dir = new File(DEFAULT_DIR);
		
		// Sanitize the input
		for(int i = 0; i < args.length; ++i) {
			switch(args[i]) {
			case "-h":
				// Print help message
				print_usage();
				System.exit(0);
			case "-p":
				// Specify a new port
				if(++i == args.length) {
					syntax_error_exit("Missing parameter for -p");
				} else if(!Pattern.matches("\\d+", args[i])) {
					syntax_error_exit("Invalid port format: " + args[i]);
				} else {
					port = Integer.parseInt(args[i]);
					if(port < 0 || port > 0xFFFF) {
						syntax_error_exit("Port out of range: " + port);
					}
				}
				break;
			case "-d":
				// Specify a new directory
				if(++i == args.length) {
					syntax_error_exit("Missing parameter for -d");
				} else {
					music_dir = new File(args[i]);
					if(!music_dir.exists()) {
						syntax_error_exit("Invalid path: " + args[i]);
					}
				}
				break;
			default:
				syntax_error_exit("Invalid option: " + args[i]);
			}
		}
		
		// Create mapping of available music onto their metadata
		file_map = new HashMap<String, Metadata>();
		Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
		enumerate_music_files(music_dir, new ArrayList<File>());
		
		if(file_map.keySet().isEmpty()) {
			System.out.println("Warning: No music files found in \"" +
					music_dir + "\"!");
		} else {
			System.out.printf("Located %d music files\n",
					file_map.keySet().size());
		}
		
		// Listen for connections from the SweetSpot app
		listen_for_connections(port);
	}
	
	/**
	 * Print program syntax for the user
	 */
	private static void print_usage() {
		System.out.println("Usage: java -jar SweetSpotServer.jar [options]");
		System.out.println("\t-h         Print this help message");
		System.out.println("\t-p <port>  The port to listen on");
		System.out.println(
				"\t-d <dir>   Specify a directory other than the current");
	}
	
	/**
	 * Enumerate music files in the chosen music directory
	 */
	private static void enumerate_music_files(File dir,
			ArrayList<File> traversed_paths) {
		// Check all files and folders in this directory
		for(File f : dir.listFiles()) {
			// If the path represents a music file, gets its metadata
			if(Pattern.matches(Definitions.MUSICFILE_REGEX, f.getName())) {
				Metadata m = generate_metadata(f);
				if(m != null) {
					file_map.put(f.getPath(), m);
				}
			// If the path represents a directory, enumerate it as well
			} else if(f.isDirectory() && !traversed_paths.contains(f)) {
				traversed_paths.add(f);
				enumerate_music_files(f, traversed_paths);
			}
		}
	}
	
	/**
	 * Generate metadata from a music file
	 * 
	 * @param f The file from which metadata will be created
	 * @return The metadata object, or null if invalid file or corrupt metadata
	 */
	private static Metadata generate_metadata(File f) {
		
		// Set params always readable from the File object
		Metadata m = new Metadata();
		m.filename = f.getName();
		m.title = m.filename;
		m.filesize = f.length();
		
		try {
			// Get audio analyzer structures
			AudioFile af = AudioFileIO.read(f);
			Tag t = af.getTag();
			AudioHeader ah = af.getAudioHeader();
			
			// Set metadata values wherever possible
			m.samplerate = ah.getSampleRateAsNumber();
			m.length = ah.getTrackLength();
			if(t != null) {
				if(!"".equals(t.getFirst(FieldKey.TITLE))) {
					m.title = t.getFirst(FieldKey.TITLE);
				}
				if(!"".equals(t.getFirst(FieldKey.ARTIST))) {
					m.artist = t.getFirst(FieldKey.ARTIST);
				}
				if(!"".equals(t.getFirst(FieldKey.ALBUM))) {
					m.album = t.getFirst(FieldKey.ALBUM);
				}
			}

		} catch(InvalidAudioFrameException e) {
			e.printStackTrace();
		} catch (CannotReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TagException e) {
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			e.printStackTrace();
		}
		return m;
	}
	
	/**
	 * Fail with an error message, due to incorrect syntax
	 * 
	 * @param msg What was wrong with the syntax
	 */
	private static void syntax_error_exit(String msg) {
		System.out.printf("Syntax error: %s\n", msg);
		print_usage();
		System.exit(1);
	}
	
	/**
	 * Listen for connections from the SweetSpot app. When a connection makes a
	 * request, dispatch a new thread to handle the client.
	 * 
	 * @param port The port on which to listen for connections
	 */
	private static void listen_for_connections(int port) {
		try {
			// Create socket for communication with clients
			ServerSocketFactory ssf = ServerSocketFactory.getDefault();
			ServerSocket ss = ssf.createServerSocket(port);
			System.out.printf("Listening on port %d for connections...\n",
					port);
			
			// Accept connections and create new threads per user
			while(true) {
				Socket s = ss.accept();
				new SweetSpotClientHandler(s).start();
				System.out.println("Client connected:");
				System.out.printf("\t%s:%d\n", s.getInetAddress(), s.getPort());
			}
		} catch(BindException e) {
			System.out.print("Unable to bind to port " + port + ". ");
			System.out.println("Perhaps the server is already running?");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This class represents a handler for a client connecting to the server
	 */
	private static class SweetSpotClientHandler extends Thread {
		
		/** Variables used by the client handler */
		Socket sock; // Communication socket
        ObjectOutputStream objout;
        ObjectInputStream objin;
		
		/**
		 * Construct a new client handler
		 * @param s The socket through which communication will travel
		 */
		public SweetSpotClientHandler(Socket s) {
			sock = s;
            try {
                objout = new ObjectOutputStream(s.getOutputStream());
                objin = new ObjectInputStream(s.getInputStream());
            } catch(IOException e) {
                e.printStackTrace();
            }
		}
		
		/**
		 * Function that will run upon creation of new client handler
		 */
		public void run() {
			TransactionType t = null;
            while(t != TransactionType.CLIENT_DISCONNECT) {

                // Get a transaction type identifier from the client
                try {
                    t = null;
                    t = (TransactionType) objin.readObject();
                } catch(ClassNotFoundException e) {
                    e.printStackTrace();
                } catch(OptionalDataException e) {
                    e.printStackTrace();
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(ClassCastException e) {
                    e.printStackTrace();
                } finally {
                    if(t == null) {
                        continue;
                    }
                }

                // Decide what to do based on the request
                try {
                    switch (t) {
                        case GET_METADATA:
                            objout.writeObject(file_map);
                            break;
                        case GET_SONGFILE:
                            // TODO
                            break;
                        case CLIENT_DISCONNECT:
                            System.out.println("Client disconnected");
                            break;
                        default:
                            System.out.println("Error: " + t);
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
		}
	}
}
