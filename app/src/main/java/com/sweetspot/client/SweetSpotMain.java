package com.sweetspot.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.SocketFactory;

public class SweetSpotMain extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // Vars used for communication
    private HashMap<String, Metadata> file_map = null;
    private Socket sock = null;
    private ObjectOutputStream objout;
    private ObjectInputStream objin;
    SweetSpotMain this_view = this;

//    private void show_error(String error_msg) {
//        AlertDialog.Builder b = new AlertDialog.Builder(this);
//        b.setMessage(error_msg);
//        b.setCancelable(true);
//        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        AlertDialog a = b.create();
//        a.show();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweet_spot_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Create socket for communication
        new InitiateConnectionTask().execute("trixie.no-ip.info", "" + Definitions.DEFAULT_PORT);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.songListContainer, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section_dropbox) + " Music";
                break;
            case 2:
                mTitle = getString(R.string.title_section_gplay) + " Music";
                break;
            default:
                mTitle = getString(R.string.title_section_addserver);
                new GetMetadataTask().execute();
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
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.sweet_spot_main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
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

    // This class represents an initiation of a socket connection
    private class InitiateConnectionTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                sock = SocketFactory.getDefault().createSocket(params[0], Integer.parseInt(params[1]));
                objout = new ObjectOutputStream(sock.getOutputStream());
                objin = new ObjectInputStream(sock.getInputStream());
            } catch(Exception e) {
                Log.e("", e.getMessage(), e);
            }
            return null;
        }
    }

    // This class represents a transaction of receiving file metadata from the server
    private class GetMetadataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void ... v) {
            try {
                Log.wtf("1", "2");
                objout.writeObject(TransactionType.GET_METADATA);
                objout.flush();
                file_map = (HashMap<String, Metadata>) objin.readObject();
                Log.wtf("3", "4");
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
            list.setAdapter(new ArrayAdapter<String>(findViewById(R.id.drawer_layout).getContext(), R.layout.activity_sweet_spot_main, song_list));
//            new AlertDialog.Builder(this_view)
//                    .setTitle("Result")
//                    .setMessage("Songs: " + file_map.size())
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                        }
//                    })
//                    .show();
        }
    }
}
