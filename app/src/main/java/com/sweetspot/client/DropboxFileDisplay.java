package com.sweetspot.client;

import android.app.ProgressDialog;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import java.util.ArrayList;


public class DropboxFileDisplay extends ActionBarActivity {
    private DropboxAPI<AndroidAuthSession> mApi;
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

}
