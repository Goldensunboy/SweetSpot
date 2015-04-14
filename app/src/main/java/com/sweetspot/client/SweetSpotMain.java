package com.sweetspot.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.exception.DropboxException;
import com.sweetspot.shared.Metadata;
import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Definitions.TransactionType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.SocketFactory;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

public class SweetSpotMain extends ActionBarActivity {

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    public static SweetSpotMain main_instance;
    public static MediaPlayer mp = new MediaPlayer();
    private static boolean init = false;

    // Mapping of song files onto their metadata
    private HashMap<String, Metadata> file_map = null;

    // List of songs that backs the main ListView
    private static List<String> songList = null;
    private static List<String> songListTitles = null;
    HashMap<String, String> songListMap = null;
    private static ListView songListView = null;

    // Mapping of song files onto which server they are from
    private HashMap<String, ServerEntryData> which_server = null;

    // List of available servers
    public static HashMap<String, ServerEntryData> sweetspot_server_list = null;

    // In the class declaration section:
    public static DropboxAPI<AndroidAuthSession> mDBApi;
//    private Button mSubmit;
//    private String NEXT_DBOX_REQUEST = "Add DropBox";
//    public String access_Token, saved_Token, default_Token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweet_spot_main);
        mTitle = getTitle();
        main_instance = this;

        if(!init) {
            init = true;
//            Log.d("Main1", "Started  successfully1.");
//            if (!Constants.mLoggedIn) {
//                Log.d("Main1", "Current Status: Logged Out of DropBox");
//            } else {
//                Log.d("Main1", "Current Status: Logged In to DropBox");
//            }

            // Initialize DropBox KeyPair
            SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
            String authToken = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
            AppKeyPair appKeys = new AppKeyPair(Constants.DROPBOX_APP_KEY, Constants.DROPBOX_APP_SECRET);
            AndroidAuthSession session;
            if (authToken != null) {
                // Restore session
                session = new AndroidAuthSession(appKeys, authToken);
            } else {
                // No token saved
                session = new AndroidAuthSession(appKeys);
            }
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);

            // Populate the available server list
            sweetspot_server_list = new HashMap<>();
            try {
                FileInputStream fis = openFileInput(Definitions.CLIENT_DATA_FILE);
                Scanner sc = new Scanner(fis);
                while (sc.hasNext()) {
                    String line = sc.nextLine();
                    String[] element = line.split(",");
                    ServerEntryData entry = new ServerEntryData(element[0], element[1], Integer.parseInt(element[2]));
                    entry.enabled = Boolean.parseBoolean(element[3]);
                    sweetspot_server_list.put(element[0], entry);
                }
            } catch (FileNotFoundException e) {
                // If this is the very first time running SweetSpot, create the server list file
                new AlertDialog.Builder(this)
                        .setTitle("No servers")
                        .setMessage("It looks like you don't have any servers yet. Add some under options!")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openServerListOptions();
                            }
                        })
                        .show();
            }

            // Populate song list from available servers
            new PopulateSongListTask().execute();
        }
    }

    // Open the server list options
    public void openServerListOptions() {
        Intent intent = new Intent(getApplicationContext(), AddServerActivity.class);
        startActivity(intent);
    }

    // Open new page to display Dropbox contents
    public void openDropboxFileList() {
        //Intent intent = new Intent(SweetSpotMain.this, DropboxFileDisplay.class);
        //SweetSpotMain.this.startActivity(intent);
        Log.d("Main1", "OPEN DROPBOX FILE LIST CALLED.");
        setContentView(R.layout.activity_dropbox_file_display);
        startActivity(new Intent(SweetSpotMain.this,DropboxFileDisplay.class));
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // If "Server List" selected ...
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), AddServerActivity.class);
            startActivity(intent);

        // Else if "Refresh" selected ...
        } else if (id == R.id.start_player) {
            Intent intent = new Intent(getApplicationContext(), SweetSpotPlayer.class);
            new PopulateSongListTask().execute();

        // Else if "Add Drop Box" selected ...
        } else if (id == R.id.connect_dropbox) {
            if (Constants.mLoggedIn) {
                // Insert what to do if selected when logged in
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle("Manage Dropbox Connection");
                builder1.setMessage("You are already connected to Dropbox.");
                builder1.setCancelable(true);
                builder1.setPositiveButton("Unlink from Dropbox",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Change this to actions for logging into DropBox...
                                //mDBApi.getSession().startOAuth2Authentication(SweetSpotMain.this);
                                //dialog.cancel();
                                logOut();
                            }
                        });
                builder1.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert1 = builder1.create();
                alert1.show();
            } else {
                // Insert what to do if selected when not logged in already
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle("Connect to Dropbox");
                builder2.setMessage("Please log in and allow access via Dropbox.");
                builder2.setCancelable(true);
                builder2.setPositiveButton("Continue",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Change this to actions for logging into DropBox...
                                mDBApi.getSession().startOAuth2Authentication(SweetSpotMain.this);
                                //dialog.cancel();
                            }
                        });
                builder2.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert2 = builder2.create();
                alert2.show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_sweet_spot_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((SweetSpotMain) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    // This class represents the action of populating the song list from available servers
    public void refreshSonglist() {new PopulateSongListTask().execute();}
    public class PopulateSongListTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void ... params) {

            // Populate the song list map
            populateSongListMap();

            // Update the list of songs to play in the main screen
            songList = new ArrayList<String>(file_map.keySet());
            songListMap = new HashMap<String, String>(); // Map song titles onto file names
            for(String s : file_map.keySet()) {
                songListMap.put(file_map.get(s).title, s);
            }
            songListTitles = new ArrayList<String>(songListMap.keySet());
            Collections.sort(songListTitles, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return s1.toUpperCase().compareTo(s2.toUpperCase());
                }
            });
            songListView = (ListView) findViewById(R.id.songListView);
            runOnUiThread(new Runnable() {
                public void run() {
                    songListView.setAdapter(new ArrayAdapter<String>(SweetSpotMain.this, android.R.layout.simple_list_item_1, songListTitles));
                }
            });
            songListView.setFastScrollEnabled(true);
            songListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> a, View v,int position, long id)
                {

                    // This is what happens when you click on a song entry
                    String filepath = songListMap.get(songListTitles.get(position));
                    Metadata meta = file_map.get(filepath);
                    ServerEntryData entry = which_server.get(filepath);

                    Intent intent = new Intent(getApplicationContext(), SweetSpotPlayer.class);
                    intent.putExtra("filepath", filepath);
                    intent.putExtra("title", meta.title);
                    intent.putExtra("artist", meta.artist);
                    intent.putExtra("album", meta.album);
                    intent.putExtra("filename", meta.filename);
                    intent.putExtra("length", meta.length);
                    intent.putExtra("samplerate", meta.samplerate);
                    intent.putExtra("filesize", meta.filesize);
                    intent.putExtra("name", entry.name);
                    intent.putExtra("url", entry.url);
                    intent.putExtra("port", entry.port);
                    startActivity(intent);
                }
            });

            return null;
        }
    }

    // Populate song list
    private void populateSongListMap() {

        // Reset all the metadata structures
        file_map = new HashMap<String, Metadata>();
        which_server = new HashMap<String, ServerEntryData>();

        // Process all enabled servers in the server list
        for(ServerEntryData entry : sweetspot_server_list.values()) {

            // Do nothing for servers that are disabled
            if(!entry.enabled) {
                continue;
            }

            // Get metadata from a single server
            HashMap<String, Metadata> map = retrieveMetadataSweetSpot(entry);

            // Consolidate results into main map
            if(map != null) {
                file_map.putAll(map);
                for(String s : map.keySet()) {
                    which_server.put(s, entry);
                }
            } else {
                Log.w("metadata", "Empty metadata HashMap gotten: " + entry.url);
            }
        }

        // Process files from DropBox
        HashMap<String, Metadata> map = retrieveMetadataDropBox();
        if(map != null) {
            file_map.putAll(map);
        }
    }

    // Get metadata from a SweetSpot server
    private HashMap<String, Metadata> retrieveMetadataSweetSpot(ServerEntryData entry) {
        HashMap<String, Metadata> map = null;
        try {

            // Establish a communication socket
            Socket sock = SocketFactory.getDefault().createSocket(entry.url, entry.port);
            ObjectInputStream objin = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream objout = new ObjectOutputStream(sock.getOutputStream());

            // Initiate a get metadata transaction
            objout.writeObject(TransactionType.GET_METADATA);
            objout.flush();
            map = (HashMap<String, Metadata>) objin.readObject();

            // Close communication socket
            objout.writeObject(TransactionType.CLIENT_DISCONNECT);
            sock.close();

        } catch (IOException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        }
        return map;
    }

    // Get metadata from a DropBox server
    private HashMap<String, Metadata> retrieveMetadataDropBox() {

        HashMap<String, Metadata> map = new HashMap<String, Metadata>();

        // Is the user logged in?
        dropboxRecurse(map, "/");

        return map;
    }

    // Recurse the Dropbox folders to find MP3s
    private void dropboxRecurse(HashMap<String, Metadata> map, String dir) {
        try {
            DropboxAPI.Entry ent = mDBApi.metadata(dir, 0, null, true, null);
            for (DropboxAPI.Entry e : ent.contents) {
                if (e.isDir) {
                    // Is a folder
                    dropboxRecurse(map, e.path);
                } else {
                    // If a file
                    if (Pattern.matches(".*\\.mp3", e.path)) {
                        String[] parts = e.path.split("/");
                        Metadata m = new Metadata();
                        m.title = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 4);
                        m.filename = e.path;
                        m.filesize = e.bytes;
                        map.put(e.path, m);
                        ServerEntryData sed = new ServerEntryData(null, null, -1);
                        which_server.put(e.path, sed);
                        //Log.e("DropboxOKAY", m.title);
                    }
                }
            }
        } catch (Exception e) {
            String s = e.toString();
            for(StackTraceElement st : e.getStackTrace()) {
                s += "\n\t" + st.toString();
            }
            //Log.e("DropboxERROR", e.getMessage() + s);
        }
    }

    // Added for DropBox
    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(mDBApi.getSession());
                setLoggedIn(true);

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        Log.d("Main1", "Store Auth.");

        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Constants.ACCESS_KEY_NAME, "oauth2:");
            edit.putString(Constants.ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
   }
    private void logOut() {
        // Remove credentials from the session
        mDBApi.getSession().unlink();

        // Clear stored keys
        clearKeys();

        // Change UI state to display logged out version
        setLoggedIn(false);

        // Change layout to SweetSpotMain
        setContentView(R.layout.activity_sweet_spot_main);
    }

    /**
     * Convenience function to take actions upon login and logout
     */
    private void setLoggedIn(boolean loggedIn) {
        Constants.mLoggedIn = loggedIn;
        if (loggedIn) {
            // Enter Actions to Take upon login
            // Insert what to do if selected when not logged in already
            AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
            builder3.setTitle("Connected to Dropbox");
            builder3.setMessage("You were connected successfully.");

            Log.d("Main1", "LOGGED IN NOW.");
            if (Constants.mLoggedIn) {
                Log.d("Main1", "Confirmed: Logged In Saved");
            }

            builder3.setCancelable(false);
            builder3.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            //openDropboxFileList();
                        }
                    });
            AlertDialog alert3 = builder3.create();
            alert3.show();

        } else {
            // Enter Actions to Take upon logout
            // Enter Actions to Take upon login
            // Insert what to do if selected when not logged in already
            AlertDialog.Builder builder4 = new AlertDialog.Builder(this);
            builder4.setTitle("Unlinked from Dropbox");
            builder4.setMessage("You were disconnected successfully.");

            Log.d("Main1", "LOGGED _OUT_ NOW.");
            if (!Constants.mLoggedIn) {
                Log.d("Main1", "Confirmed: Logged Out Saved");
            }


            builder4.setCancelable(false);
            builder4.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert4 = builder4.create();
            alert4.show();
        }
    }
    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
        Log.d("Main1", "Keys Cleared.");
    }
}
