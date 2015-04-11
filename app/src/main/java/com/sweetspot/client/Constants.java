package com.sweetspot.client;

import com.dropbox.client2.session.Session;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.os.Environment;

import com.dropbox.client2.session.Session.AccessType;

/**
 * Created by James on 4/11/2015.
 */
public class Constants {

    public static final String OVERRIDEMSG = "File name with this name already exists.Do you want to replace this file?";
    // For Use with DropBox
    // Replace this with your app key and secret assigned by Dropbox.
    // Note that this is a really insecure way to do this, and you shouldn't
    // ship code which contains your key & secret in such an obvious way.
    // Obfuscation is good.
    final static public String DROPBOX_APP_KEY = "fd2ss18720tu0ds";
    final static public String DROPBOX_APP_SECRET = "769gk29vul1jek5";
    public static boolean mLoggedIn = false;


    final static public Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    final static public String ACCOUNT_PREFS_NAME = "prefs";
    final static public String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static public String ACCESS_SECRET_NAME = "ACCESS_SECRET";

}
