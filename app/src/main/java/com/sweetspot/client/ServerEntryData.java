package com.sweetspot.client;

/**
 * Created by andrew on 3/17/15.
 */
public class ServerEntryData {

    // Enumeration of the types of servers available
    public static enum ServerType {
        SWEETSPOT {public String toString() {return "SweetSpot";}},
        DROPBOX   {public String toString() {return "DropBox";}}
    };

    // Name of the server
    public String name = null;

    // Type fo thr server
    public ServerType type;

    // Whether or not we should receive files from this server
    public boolean enabled;

    // URL of the server
    public String url = null;

    // Port number used to connect
    public int port = -1;

    // DropBox username
    public String username = null;

    // DropBox password
    public String password = null;

    // Create a new server data object (SweetSpot)
    public ServerEntryData(String name, String url, int port) {
        this.name = name;
        this.url = url;
        this.port = port;
        type = ServerType.SWEETSPOT;
        enabled = true;
    }

    // Create a new server data object (DropBox)
    public ServerEntryData(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
        type = ServerType.DROPBOX;
        enabled = true;
    }
}
