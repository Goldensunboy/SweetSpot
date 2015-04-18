package com.sweetspot.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Metadata;

import javax.net.SocketFactory;

public class SweetSpotPlayer extends Activity
        implements OnSeekBarChangeListener {

    // Constants used by the media player
    private static final int FORWARD_TIME = 5000;
    private static final int BACKWARD_TIME = 5000;

    // Variables used by the media player
    public TextView songName, duration;
    private static double timeElapsed = 0, finalTime = 0;
    private Handler durationHandler = new Handler();
    private SeekBar seekbar;
    private MediaPlayer mp;
    private ProgressBar prog;
    //private Handler progHandler;

    // Data passed in from main
    private String filepath;
    private Metadata metadata = new Metadata();
    private ServerEntryData entry;

    // Register and unregister events pertaining to using the headphone jack
    private AudioManager manager;
    private BroadcastReceiver hpJackRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(manager != null) {
                if (!manager.isWiredHeadsetOn()) {
                    pause(null);
                } else {
                    if (!manager.isMusicActive()) {
                        play(null);
                    }
                }
            }
        }
    };
    IntentFilter hpJackFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mp = SweetSpotMain.mp;
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Get arguments from intent map
        Intent intent = getIntent();
        filepath = intent.getStringExtra("filepath");
        metadata.artist = intent.getStringExtra("artist");
        metadata.album = intent.getStringExtra("album");
        metadata.filename = intent.getStringExtra("filename");
        metadata.filesize = intent.getLongExtra("filesize", -1);
        metadata.length = intent.getIntExtra("length", -1);
        metadata.samplerate = intent.getIntExtra("samplerate", -1);
        metadata.title = intent.getStringExtra("title");
        entry = new ServerEntryData(intent.getStringExtra("name"), intent.getStringExtra("url"), intent.getIntExtra("port", -1));

        //set the layout of the Activity
        setContentView(R.layout.activity_media_player);

        // Register the broadcast receiver for headphones
        enableHPJackEvents();

        //initialize views
        initializeViews();

        // Signal controller to play music from input file on a new thread
        if(!metadata.filename.equals(SweetSpotMain.currSong)) {
            SweetSpotMain.currSong = metadata.filename;
            new PlaySongFromInternet().execute();
        } else {
            // Set max song time within view
            finalTime = mp.getDuration();
            seekbar.setMax((int) finalTime);
            updateSeekBar();
        }
    }

    public void initializeViews() {

        // Set Song Name on screen
        songName = (TextView) findViewById(R.id.songName);
        songName.setText(metadata.title);
        prog = (ProgressBar) findViewById(R.id.progressBar);
        prog.setProgress(0);

        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                prog.setVisibility(ProgressBar.GONE);
                play(null);
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer m, int what, int extra) {
                return false;
            }
        });

        // Set up seek bar within view
        duration = (TextView) findViewById(R.id.songDuration);
        duration.setText("");
        seekbar = (SeekBar) findViewById(R.id.seekBar);

        // Listener
        seekbar.setOnSeekBarChangeListener(this);
    }

    private class PlaySongFromInternet extends AsyncTask<Void, Void, Void> {
        @Override
        public Void doInBackground(Void ... params) {
            try {
                // Create a buffered input stream
                if (!entry.isDropBoxServer()) {

                    // Establish connection with the server
                    Socket sock = SocketFactory.getDefault().createSocket(entry.url, entry.port);
                    InputStream in = sock.getInputStream();
                    ObjectOutputStream objout = new ObjectOutputStream(sock.getOutputStream());
                    ObjectInputStream objin = new ObjectInputStream(in);

                    // Initiate get songfile transaction
                    objout.writeObject(Definitions.TransactionType.GET_SONGFILE);
                    objout.writeObject(filepath);
                    objout.flush();
                    long size = objin.readLong();
                    byte[] songBuffer = new byte[(int) size];
                    int recv = 0;

                    // Read file into buffer
                    while(recv < size) {
                        int bytes_read = in.read(songBuffer, recv, (int) (size - recv));
                        recv += bytes_read;
                        prog.setProgress((int) (recv * 100 / size));
                    }

                    // Write data to cache
                    mp.pause();
                    FileOutputStream fos = openFileOutput(Definitions.PLAY_BUFFER_FILE, MODE_PRIVATE);
                    fos.write(songBuffer, 0, (int) size);
                    fos.close();

                } else {

                    // The cache file that Dropbox will write to
                    FileOutputStream fos = openFileOutput(Definitions.PLAY_BUFFER_FILE, MODE_PRIVATE);
                    DropboxAPI.DropboxFileInfo info = SweetSpotMain.mDBApi.getFile(filepath, null, fos, new ProgressListener() {
                        @Override
                        public void onProgress(long recv, long size) {
                            prog.setProgress((int) (recv * 100 / size));
                        }
                    });
                    fos.close();
                }

                // Configure the media player to use the cache file
                timeElapsed = 0;
                mp.reset();
                FileInputStream fis = openFileInput(Definitions.PLAY_BUFFER_FILE);
                mp.setDataSource(fis.getFD());
                fis.close();
                while(!mp.isPlaying()) {
                    try {
                        mp.prepare();
                    } catch (IllegalStateException e) {
                        Log.e("MP Prepare", "" + e.getMessage());
                    } catch (IOException e) {
                        Log.e("MP Prepare", "" + e.getMessage());
                    }
                }

            } catch(IOException e) {
                String s = e.toString();
                for(StackTraceElement s2 : e.getStackTrace()) {
                    s += "\n\t" + s2;
                }
                Log.e(e.getMessage() + "\t\t" + e.getCause(), s);
            } catch(DropboxException e) {
                String s = e.toString();
                for(StackTraceElement s2 : e.getStackTrace()) {
                    s += "\n\t" + s2;
                }
                Log.e(e.getMessage() + "\t\t" + e.getCause(), s);
            }

            return null;
        }
    }

    // Play song
    public void play(View view) {

        // Start mediaPlayer
        mp.start();

        // Set max song time within view
        finalTime = mp.getDuration();
        seekbar.setMax((int) finalTime);

        // Update Seek Bar
        updateSeekBar();
    }

    // Update seek bar
    public void updateSeekBar() {
        durationHandler.postDelayed(updateSeekBarTime, 100);
    }

    // Handler to change seekBarTime
    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {

            // Get current position
            timeElapsed = mp.getCurrentPosition();

            // Set seek bar progress
            seekbar.setProgress((int) timeElapsed);

            // Set time remaining
            long min_elapsed = TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed);
            long sec_elapsed = TimeUnit.MILLISECONDS.toSeconds((long) timeElapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed));
            long min_total = TimeUnit.MILLISECONDS.toMinutes((long) finalTime);
            long sec_total = TimeUnit.MILLISECONDS.toSeconds((long) finalTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime));
            duration.setText(String.format("%d:%02d / %d:%02d", min_elapsed, sec_elapsed, min_total, sec_total));

            // Repeat again in 100 milliseconds
            durationHandler.postDelayed(this, 100);
        }
    };

    // Pause media
    public void pause(View view) {
        mp.pause();
    }

    // Go forward at forwardTime seconds
    public void forward(View view) {

        // Check if we can go forward at forwardTime seconds before song ends
        if ((timeElapsed + FORWARD_TIME) <= finalTime) {
            timeElapsed = timeElapsed + FORWARD_TIME;

            // Seek to the exact second of the track
            mp.seekTo((int) timeElapsed);
        }
    }

    // Go backward at backwardTime seconds
    public void rewind(View view) {

        //Check if we can go backward at backwardTime seconds
        if ((timeElapsed - BACKWARD_TIME) >= 0 ) {
            timeElapsed = timeElapsed - BACKWARD_TIME;

            //Seek to the exact second of the track
            mp.seekTo((int) timeElapsed);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    // When user starts moving the seek bar
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        // remove message Handler from updating progress bar
        durationHandler.removeCallbacks(updateSeekBarTime);
    }

    // When user stops moving the seek bar
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        int currentPosition = seekBar.getProgress();

        // Forward or backward to position selected
        mp.seekTo(currentPosition);

        // Update seek bar again
        updateSeekBar();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Register the broadcast receiver for headphones
        disableHPJackEvents();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Register the broadcast receiver for headphones
        disableHPJackEvents();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        // Register the broadcast receiver for headphones
        enableHPJackEvents();
    }

    private static boolean registered = false;
    public void enableHPJackEvents() {
        if(!registered) {
            //registerReceiver(hpJackRecv, hpJackFilter);
            registered = true;
        }
    }
    public void disableHPJackEvents() {
        if(registered) {
            //unregisterReceiver(hpJackRecv);
            registered = false;
        }
    }
}