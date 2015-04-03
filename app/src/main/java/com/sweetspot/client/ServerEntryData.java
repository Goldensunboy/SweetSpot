package com.sweetspot.client;

/**
 * Created by andrew on 3/17/15.
 */
public class ServerEntryData {

    // Name of the server
    public String name;

    // URL of the server
    public String url;

    // Port number used to connect
    public int port;

    // Whether or not we should receive files from this server
    public boolean enabled;

    // Create a new server data object
    public ServerEntryData(String name, String url, int port) {
        this.name = name;
        this.url = url;
        this.port = port;
        enabled = true;
    }
}
