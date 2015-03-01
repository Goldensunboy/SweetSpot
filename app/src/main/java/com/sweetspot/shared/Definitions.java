package com.sweetspot.shared;

import java.io.Serializable;

/**
 * Created by andrew on 3/1/15.
 */
public class Definitions {

    /** Default variables */
    public static final int DEFAULT_PORT = 30012;
    public static final String MUSICFILE_REGEX = ".*\\.(mp3|ogg|wav|flac)";

    /** Enumeration of transaction types */
    public enum TransactionType implements Serializable {
        GET_METADATA,
        GET_SONGFILE,
        CLIENT_DISCONNECT
    };
}
