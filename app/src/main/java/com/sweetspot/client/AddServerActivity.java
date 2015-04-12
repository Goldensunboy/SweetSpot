package com.sweetspot.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sweetspot.shared.Definitions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.List;
import java.util.Scanner;
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

                    final ServerEntryData serverEntry = SweetSpotMain.sweetspot_server_list.get(serverList.get(position));

                    // Create the dialog for viewing a server
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddServerActivity.this);
                    LayoutInflater inflater = AddServerActivity.this.getLayoutInflater();
                    final View dialogView = inflater.inflate(R.layout.fragment_modify_server_dialog, null);
                    builder.setView(dialogView)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                            .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    removeServer(serverEntry);
                                    Toast.makeText(getBaseContext(), "Removed server \"" + serverEntry.name + "\"", Toast.LENGTH_LONG).show();
                                }
                            })
                            .setNeutralButton(serverEntry.enabled ? "Disable" : "Enable", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    toggleServer(serverEntry);
                                    Toast.makeText(getBaseContext(), "Toggled server \"" + serverEntry.name + "\" to " + (serverEntry.enabled ? "enabled" : "disabled"), Toast.LENGTH_LONG).show();
                                }
                            });

                    // Show information about the server
                    TextView serverInfo = ((TextView) dialogView.findViewById(R.id.serverInfoTextView));
                    String info = "Name: " + serverEntry.name + "\nEnabled: " + serverEntry.enabled + "\n";
                    switch(serverEntry.type) {
                        case SWEETSPOT:
                            info += "Type: SweetSpot\nURL: " + serverEntry.url + "\nPort: " + serverEntry.port;
                            break;
                        case DROPBOX:
                            info += "Type: DropBox\nUsername: " + serverEntry.username + "\nPassword: " + serverEntry.password;
                            break;
                        default:
                            info += "Type: Unknown";
                    }
                    serverInfo.setText(info);

                    // Create the modify server dialog
                    builder.create().show();
                } else {

                    // Create the dialog for adding a new server
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
        runOnUiThread(new AddSingleServerTask(newServer.name));

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
        } catch (FileNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("Backing file not found: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (IOException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("File IO error: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    /**
     * Remove a server from the data structures used by SweetSpot
     * @param serverEntry The metadata for the server to remove
     */
    private void removeServer(ServerEntryData serverEntry) {

        // Remove server from current instance of SweetSpot
        SweetSpotMain.sweetspot_server_list.remove(serverEntry.name);
        runOnUiThread(new RemoveSingleServerTask(serverEntry.name));

        // Remove server from backing file
        try {

            // Read existing data into an ArrayList, except removal server
            FileInputStream fis = openFileInput(Definitions.CLIENT_DATA_FILE);
            ArrayList<String> servers = new ArrayList<String>();
            Scanner sc = new Scanner(fis);
            while(sc.hasNext()) {
                String line = sc.nextLine();
                String[] element = line.split(",");
                if(!serverEntry.name.equals(element[0])) {
                    servers.add(line);
                }
            }
            sc.close();
            fis.close();

            // Write data back to the file
            FileOutputStream fos = openFileOutput(Definitions.CLIENT_DATA_FILE, MODE_PRIVATE);
            PrintWriter pw = new PrintWriter(fos);
            for(String s : servers) {
                pw.printf("%s\n", s);
            }
            pw.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("Backing file not found: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (IOException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("File IO error: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    /**
     * Toggle a server in the data structures used by SweetSpot
     * @param serverEntry The metadata for the server to toggle
     */
    private void toggleServer(ServerEntryData serverEntry) {

        // Toggle server in current instance of SweetSpot
        boolean newState = !serverEntry.enabled;
        SweetSpotMain.sweetspot_server_list.get(serverEntry.name).enabled = newState;

        // Update server from backing file
        try {

            // Read existing data into an ArrayList, except removal server
            FileInputStream fis = openFileInput(Definitions.CLIENT_DATA_FILE);
            ArrayList<String> servers = new ArrayList<String>();
            Scanner sc = new Scanner(fis);
            while(sc.hasNext()) {
                String line = sc.nextLine();
                String[] element = line.split(",");
                if(!serverEntry.name.equals(element[0])) {
                    servers.add(line);
                } else {
                    // Toggle the enabled entry in the CSV data
                    String newLine = "";
                    for(int i = 0; i < element.length; ++i) {
                        if(i != 0) {
                            newLine += ',';
                        }
                        if(i != 2) {
                            newLine += element[i];
                        } else {
                            newLine += newState;
                        }
                    }
                    servers.add(newLine);
                }
            }
            sc.close();
            fis.close();

            // Write data back to the file
            FileOutputStream fos = openFileOutput(Definitions.CLIENT_DATA_FILE, MODE_PRIVATE);
            PrintWriter pw = new PrintWriter(fos);
            for(String s : servers) {
                pw.printf("%s\n", s);
            }
            pw.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Internal error")
                    .setMessage("Backing file not found: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (IOException e) {
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
    private class AddSingleServerTask implements Runnable {
        private String name;
        public AddSingleServerTask(String name) {
            this.name = name;
        }
        public void run() {
            serverList.add(0, name);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Remove a server from the server list on the UI thread
     */
    private class RemoveSingleServerTask implements Runnable {
        private String name;
        public RemoveSingleServerTask(String name) {
            this.name = name;
        }
        public void run() {
            serverList.remove(name);
            adapter.notifyDataSetChanged();
        }
    }
}
