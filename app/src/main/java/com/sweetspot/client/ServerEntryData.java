package com.sweetspot.client;

import java.io.Serializable;

/**
 * Created by andrew on 3/17/15.
 */
public class ServerEntryData implements Serializable {

    // Name of the server
    public String name = null;

    // URL of the server
    public String url = null;

    // Port number used to connect
    public int port = -1;

    // Whether or not we should receive files from this server
    public boolean enabled;

    // Create a new server data object (SweetSpot)
    public ServerEntryData(String name, String url, int port) {
        this.name = name;
        this.url = url;
        this.port = port;
        enabled = true;
    }

    public boolean isDropBoxServer() {
        return port == -1;
    }
}
