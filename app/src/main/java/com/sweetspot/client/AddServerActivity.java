package com.sweetspot.client;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.sweetspot.shared.Definitions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class AddServerActivity extends Activity {

    private static List<String> serverList;
    private static ArrayAdapter<String> adapter;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_server_dialog);

        serverList = new ArrayList<String>();
        for(String s : SweetSpotMain.sweetspot_server_list.keySet()) serverList.add(s);
        serverList.add("Add server...");
        adapter = new ArrayAdapter<String>(this, R.layout.add_server_textview, serverList);

        setContentView(R.layout.add_server);
        ListView lv = (ListView)findViewById(R.id.serverlist);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> a, View v,int position, long id)
            {
                if(position < serverList.size() - 1) {
                    Toast.makeText(getBaseContext(), serverList.get(position), Toast.LENGTH_LONG).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddServerActivity.this);
                    LayoutInflater inflater = AddServerActivity.this.getLayoutInflater();
                    final View dialogView = inflater.inflate(R.layout.fragment_add_server_dialog, null);
                    builder.setView(dialogView)
                            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    // Does this server already exist?
                                    String errorStr = null;
                                    String name = ((EditText) dialogView.findViewById(R.id.serverNameEditText)).getText().toString();

                                    // Validate name
                                    if ("".equals(name)) {
                                        errorStr = "Must enter a server name!";
                                    } else if (SweetSpotMain.sweetspot_server_list.keySet().contains(name)) {
                                        errorStr = "Server named " + name + " already exists!";
                                    } else if (Pattern.matches(".*((,).*)+", name)) {
                                        errorStr = "Server name has an illegal character!";
                                    }

                                    // Is this a SweetSpot server or DropBox server?
                                    ServerEntryData newServer = null;
                                    if (((RadioButton) dialogView.findViewById(R.id.sweetSpotButton)).isChecked() && errorStr == null) {

                                        // SweetSpot server
                                        String url = ((EditText) dialogView.findViewById(R.id.urlEditText)).getText().toString();
                                        String port = ((EditText) dialogView.findViewById(R.id.portEditText)).getText().toString();

                                        // Validate input
                                        int portVal = -1;
                                        if ("".equals(url)) {
                                            errorStr = "Must enter a server URL!";
                                        } else if (!Pattern.matches("\\d+", port)) {
                                            errorStr = "Invalid port!";
                                        } else {
                                            portVal = Integer.parseInt(port);
                                            if (portVal > 0xFFFF) {
                                                errorStr = "Port out of range!";
                                            }
                                        }

                                        // Create server
                                        newServer = new ServerEntryData(name, url, portVal);

                                    } else if (errorStr == null) {

                                        // DropBox server
                                        String username = ((EditText) dialogView.findViewById(R.id.usernameEditText)).getText().toString();
                                        String password = ((EditText) dialogView.findViewById(R.id.passwordEditText)).getText().toString();

                                        // Validate input
                                        if ("".equals(username)) {
                                            errorStr = "Must enter a DropBox username!";
                                        } else if ("".equals(password)) {
                                            errorStr = "Must enter a DropBox password!";
                                        }

                                        // Create server
                                        newServer = new ServerEntryData(name, username, password);
                                    }

                                    // Add server to our server list if there wasn't an error
                                    if (errorStr == null) {
                                        addNewServer(newServer);
                                    } else {
                                        new AlertDialog.Builder(AddServerActivity.this)
                                                .setTitle("Error creating server")
                                                .setMessage(errorStr)
                                                .setPositiveButton(android.R.string.yes, null)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });

                    // Create the add server dialog
                    builder.create().show();
                }
            }
        });
    }

    /**
     * Add a new server to the data structures used by SweetSpot
     * @param newServer The metadata for the new server to add
     */
    private void addNewServer(ServerEntryData newServer) {

        // Add server to current running instance of SweetSpot
        SweetSpotMain.sweetspot_server_list.put(newServer.name, newServer);
        runOnUiThread(new UpdateViewTask(newServer.name));

        // Add server to backing file
        try {
            FileOutputStream fos = openFileOutput(Definitions.CLIENT_DATA_FILE, MODE_APPEND);
            PrintWriter pw = new PrintWriter(fos);
            pw.printf("%s,%s,%b,%s,%d,%s,%s\n",
                    newServer.name,
                    newServer.type,
                    newServer.enabled,
                    newServer.url,
                    newServer.port,
                    newServer.username,
                    newServer.password);
            pw.flush();
            fos.close();
        } catch(FileNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("Backing file not found: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } catch(IOException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("File IO error: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    /**
     * Adds a server to the server list on the UI thread
     */
    private class UpdateViewTask implements Runnable {
        private String name;
        public UpdateViewTask(String name) {
            this.name = name;
        }
        public void run() {
            serverList.add(0, name);
            adapter.notifyDataSetChanged();
        }
    }
}
