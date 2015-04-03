package com.sweetspot.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sweetspot.shared.Metadata;
import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Definitions.TransactionType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.net.SocketFactory;

public class SweetSpotMain extends ActionBarActivity {

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    public static SweetSpotMain the_main_activity;

    // Mapping of song files onto their metadata
    private HashMap<String, Metadata> file_map = null;

    // Mapping of active server URLs onto communication sockets
    private HashMap<String, Socket> sock_map = null;

    // List of available servers
    public HashMap<String, ServerEntryData> sweetspot_server_list = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweet_spot_main);
        the_main_activity = this;
        mTitle = getTitle();

        // Populate the available server list
        sweetspot_server_list = new HashMap<>();
        try {
            FileInputStream fis = openFileInput(Definitions.CLIENT_DATA_FILE);
            Scanner sc = new Scanner(fis);
            while(sc.hasNext()) {
                String line = sc.nextLine();
                String[] element = line.split(",");
                ServerEntryData entry = new ServerEntryData(element[0], element[1], Integer.parseInt(element[2]));
                entry.enabled = Boolean.parseBoolean(element[3]);
                sweetspot_server_list.put(element[0], entry);
            }
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
        Intent intent = new Intent(getApplicationContext(), ServerListActivity.class);
        startActivity(intent);
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
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), ServerListActivity.class);
            startActivity(intent);
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
}
