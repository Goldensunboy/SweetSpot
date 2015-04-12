package com.sweetspot.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import android.widget.AdapterView.OnItemClickListener;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;

import java.util.ArrayList;


public class DropboxFileDisplay extends ActionBarActivity implements OnItemClickListener {
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private String DIR = "/";
    private ArrayList<DropboxAPI.Entry> files;
    private ArrayList<String> dir;
    private boolean isItemClicked = false;
    // , onResume = false;
    private ListView lvDropboxDownloadFilesList;
    // private Button btnDropboxDownloadDone;
    private ProgressDialog pd;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                lvDropboxDownloadFilesList.setAdapter(new DownloadFileAdapter(
                        DropboxFileDisplay.this, files));
                pd.dismiss();
            } else if (msg.what == 1) {
                Toast.makeText(DropboxFileDisplay.this,
                        "File save at " + msg.obj.toString(), Toast.LENGTH_LONG)
                        .show();
            }
        };
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DROPBOX FILE DISPLAY", "onCreate initiated.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_file_display);

        lvDropboxDownloadFilesList = (ListView) findViewById(R.id.lvDropboxDownloadFilesList);

        // btnDropboxDownloadDone = (Button)
        // findViewById(R.id.btnDropboxDownloadDone);
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        checkAppKeySetup();
        // setLoggedIn(false);
        if (!Constants.mLoggedIn)
            mDBApi.getSession().startAuthentication(DropboxFileDisplay.this);
        // else
        // setLoggedIn(mApi.getSession().isLinked());
        // click events
        // btnDropboxDownloadDone.setOnClickListener(this);
        lvDropboxDownloadFilesList.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        Entry fileSelected = files.get(arg2);
        if (fileSelected.isDir) {
            isItemClicked = true;
            DIR = dir.get(arg2);
            setLoggedIn(true);
        } else {

            downloadDropboxFile(fileSelected);
            // getIntent().getStringExtra("fileParentPath"));
        }
    }

    private void checkAppKeySetup() {
        if (Constants.DROPBOX_APP_KEY.startsWith("CHANGE")
                || Constants.DROPBOX_APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            finish();
            return;
        }
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + Constants.DROPBOX_APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's "
                    + "manifest is not set up correctly. You should have a "
                    + "com.dropbox.client2.android.AuthActivity with the "
                    + "scheme: " + scheme);
            finish();
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }


    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(Constants.DROPBOX_APP_KEY,
                Constants.DROPBOX_APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0],
                    stored[1]);
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE,
                    accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE);
        }

        return session;
    }

    public void setLoggedIn(final boolean loggedIn) {
        pd = ProgressDialog.show(DropboxFileDisplay.this, null,
                "Retrieving data...");
        new Thread(new Runnable() {

            @Override
            public void run() {
                Constants.mLoggedIn = loggedIn;
                if (loggedIn) {
                    int i = 0;
                    com.dropbox.client2.DropboxAPI.Entry dirent;
                    try {
                        dirent = mDBApi.metadata(DIR, 1000, null, true, null);
                        files = new ArrayList<com.dropbox.client2.DropboxAPI.Entry>();
                        dir = new ArrayList<String>();
                        for (com.dropbox.client2.DropboxAPI.Entry ent : dirent.contents) {
                            files.add(ent);
                            dir.add(new String(files.get(i++).path));
                        }
                        i = 0;
                        mHandler.sendEmptyMessage(0);
                    } catch (DropboxException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {

        super.onResume();
        AndroidAuthSession session = mDBApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:"
                        + e.getLocalizedMessage());
            }
        }
    }

    private void storeKeys(String key, String secret) {
        SharedPreferences prefs = getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(Constants.ACCESS_KEY_NAME, key);
        edit.putString(Constants.ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }


    private boolean downloadDropboxFile(Entry fileSelected) {// , String
        // localFilePath)
        // {
        File dir = new File(Utils.getPath());
        if (!dir.exists())
            dir.mkdirs();
        try {
            File localFile = new File(dir + "/" + fileSelected.fileName());
            if (!localFile.exists()) {
                localFile.createNewFile();
                copy(fileSelected, localFile);

            } else {
                showFileExitsDialog(fileSelected, localFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    private void copy(final Entry fileSelected, final File localFile) {
        final ProgressDialog pd = ProgressDialog.show(DropboxFileDisplay.this,
                "Downloading...", "Please wait...");
        new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedInputStream br = null;
                BufferedOutputStream bw = null;
                DropboxInputStream fd;
                try {
                    fd = mDBApi.getFileStream(fileSelected.path,
                            localFile.getPath());
                    br = new BufferedInputStream(fd);
                    bw = new BufferedOutputStream(new FileOutputStream(
                            localFile));

                    byte[] buffer = new byte[4096];
                    int read;
                    while (true) {
                        read = br.read(buffer);
                        if (read <= 0) {
                            break;
                        }
                        bw.write(buffer, 0, read);
                    }
                    pd.dismiss();
                    Message message = new Message();
                    message.obj = localFile.getAbsolutePath();
                    message.what = 1;
                    mHandler.sendMessage(message);
                } catch (DropboxException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                            if (br != null) {
                                br.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }).start();

    }


    private void showFileExitsDialog(final Entry fileSelected,
                                     final File localFile) {
        AlertDialog.Builder alertBuilder = new Builder(DropboxFileDisplay.this);
        alertBuilder.setMessage(Constants.OVERRIDEMSG);
        alertBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copy(fileSelected, localFile);
                    }
                });
        alertBuilder.setNegativeButton("Cancel", null);
        alertBuilder.create().show();

    }

    @Override
    public void onBackPressed() {
        if (isItemClicked) {
            if (DIR.length() == 0) {
                // logOut();
                setResult(RESULT_OK);
                super.onBackPressed();
            } else {
                DIR = DIR.substring(0, DIR.lastIndexOf('/'));
                setLoggedIn(true);

            }
        } else {
            setResult(RESULT_OK);
            super.onBackPressed();
        }

    }


}
