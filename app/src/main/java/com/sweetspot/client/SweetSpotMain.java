package com.sweetspot.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.dropbox.client2.exception.DropboxException;
import com.sweetspot.shared.Metadata;
import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Definitions.TransactionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.net.SocketFactory;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI;

public class SweetSpotMain extends ActionBarActivity {

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // Mapping of song files onto their metadata
    private HashMap<String, Metadata> file_map = null;

    // Mapping of active server URLs onto communication sockets
    private HashMap<String, Socket> sock_map = null;

    // List of available servers
    public static HashMap<String, ServerEntryData> sweetspot_server_list = null;

    // For Use with DropBox
    // Replace this with your app key and secret assigned by Dropbox.
    // Note that this is a really insecure way to do this, and you shouldn't
    // ship code which contains your key & secret in such an obvious way.
    // Obfuscation is good.
    private static final String APP_KEY = "fd2ss18720tu0ds";
    private static final String APP_SECRET = "769gk29vul1jek5";

    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private static final boolean USE_OAUTH1 = false;

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private boolean mLoggedIn = false;
    private Button mSubmit;
    private String NEXT_DBOX_REQUEST = "Add DropBox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweet_spot_main);
        mTitle = getTitle();

        // Initialize DropBox KeyPair
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        //mDBApi.getSession().startOAuth2Authentication(SweetSpotMain.this);

        // Populate the available server list
        sweetspot_server_list = new HashMap<>();
        try {
            FileInputStream fis = openFileInput(Definitions.CLIENT_DATA_FILE);
            Scanner sc = new Scanner(fis);
            while(sc.hasNext()) {
                String line = sc.nextLine();
                new AlertDialog.Builder(this)
                        .setTitle("Info")
                        .setMessage("line: " + line)
                        .setPositiveButton("OK", null)
                        .show();
                String[] element = line.split(",");
                ServerEntryData entry = null;
                switch(element[1]) {
                    case "SweetSpot":
                        entry = new ServerEntryData(element[0], element[3], Integer.parseInt(element[4]));
                        break;
                    case "DropBox":
                        entry = new ServerEntryData(element[0], element[5], element[6]);
                        break;
                    default:
                        new AlertDialog.Builder(this)
                                .setTitle("Internal error")
                                .setMessage("Error parsing " + Definitions.CLIENT_DATA_FILE + " backing file")
                                .setPositiveButton("OK", null)
                                .show();
                }
                if(entry != null) {
                    entry.enabled = Boolean.parseBoolean(element[2]);
                    sweetspot_server_list.put(element[0], entry);
                }
            }
            File f = getFilesDir();
            new AlertDialog.Builder(this)
                    .setTitle("Info")
                    .setMessage("File dir: " + f.getAbsolutePath())
                    .setPositiveButton("OK", null)
                    .show();
            new PopulateSongListTask().execute();
        } catch(FileNotFoundException e) {
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
        setContentView(R.layout.activity_dropbox_file_display);

        // Problem here with "Internet on Main" ... so this needs to go on another thread somehow
/*        String[] fnames = null;
        try {
            Entry dirent = mDBApi.metadata("/", 100, null, false, null);
            ArrayList<Entry> files = new ArrayList<Entry>();
            ArrayList<String> dir=new ArrayList<String>();
            int i=0;
            for (Entry ent: dirent.contents)
            {
                files.add(ent);// Add it to the list of thumbs we can choose from
                //dir = new ArrayList<String>();
                dir.add(new String(files.get(i++).path));
            }
            i=0;
            fnames=dir.toArray(new String[dir.size()]);

            //return fnames;
        } catch(DropboxException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        }

        final GridView gv=(GridView) findViewById(R.id.gridView1);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,fnames);
        gv.setBackgroundColor(Color.BLACK);
        gv.setNumColumns(3);
        gv.setGravity(Gravity.CENTER);
        gv.setAdapter(ad);
*/
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        // ----
        // Actions to take for each menu item selected
        // ----
        // If "Settings" selected ...
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), AddServerActivity.class);
            startActivity(intent);

        // Else if "Start Player" selected ...
        } else if (id == R.id.start_player) {
            Intent intent = new Intent(getApplicationContext(), SweetSpotPlayer.class);
            startActivity(intent);

        // Else if "Add Drop Box" selected ...
        } else if (id == R.id.connect_dropbox) {
            if (mLoggedIn) {
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
    private class PopulateSongListTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void ... params) {
            populateSongListMap();
            // TODO update UI
            return null;
        }
    }

    // Connect to a SweetSpot server
    private void initiateSweetSpotConnection(String url, int port) {
        try {
            Socket sock = SocketFactory.getDefault().createSocket(url, port);
            sock_map.put(url, sock);
        } catch(IOException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        }
    }

    // Populate song list
    private void populateSongListMap() {
        boolean b1 = true, b2 = true; if(b1 && b2) return;
        for(ServerEntryData entry : sweetspot_server_list.values()) {
            if(!entry.enabled) {
                // Do nothing for servers that are disabled
                continue;
            }
            if(!sock_map.containsKey(entry.url)) {
                // Initiate a connection to the server if the socket isn't available yet
                // TODO
            }
            // Get metadata from the server
            HashMap<String, Metadata> new_file_map = retrieveMetadata(entry);
            if(new_file_map != null) {
                file_map.putAll(new_file_map);
            } else {
                Log.w("Empty metadata HashMap gotten: " + entry.url, "");
            }
        }
    }

    // Get metadata from a server
    private HashMap<String, Metadata> retrieveMetadata(ServerEntryData entry) {
        HashMap<String, Metadata> file_map = null;
        try {
            ObjectInputStream objin = new ObjectInputStream(sock_map.get(entry.url).getInputStream());
            ObjectOutputStream objout = new ObjectOutputStream(sock_map.get(entry.url).getOutputStream());
            objout.writeObject(TransactionType.GET_METADATA);
            file_map = (HashMap<String, Metadata>) objin.readObject();
            objin.close();
            objout.close();
        } catch(IOException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        } catch(ClassNotFoundException e) {
            Log.e(e.getMessage(), e.getMessage(), e);
        }
        return file_map;
    }

    /* This class represents a transaction of receiving file metadata from the server
    private class GetMetadataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void ... v) {
            try {
                objout.writeObject(TransactionType.GET_METADATA);
                objout.flush();
                file_map = (HashMap<String, Metadata>) objin.readObject();
            } catch(Exception e) {
                Log.e("", e.getMessage(), e);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void v) {
            List<String> song_list = new ArrayList<String>();
            for(String file : file_map.keySet()) {
                Metadata m = file_map.get(file);
                song_list.add(m.title + " / " + m.artist);
            }
            ListView list = (ListView) findViewById(R.id.songListView);
            list.setAdapter(new ArrayAdapter<String>(this_view, android.R.layout.simple_list_item_1, song_list));
            list.setFastScrollEnabled(true);

            new AlertDialog.Builder(this_view)
                    .setTitle("Result")
                    .setMessage("Songs: " + file_map.size())
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }*/

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
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
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
        mLoggedIn = loggedIn;
        if (loggedIn) {
            // Enter Actions to Take upon login
            // Insert what to do if selected when not logged in already
            AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
            builder3.setTitle("Connected to Dropbox");
            builder3.setMessage("You were connected successfully.");
            builder3.setCancelable(false);
            builder3.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //dialog.cancel();
                            openDropboxFileList();
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
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    }
