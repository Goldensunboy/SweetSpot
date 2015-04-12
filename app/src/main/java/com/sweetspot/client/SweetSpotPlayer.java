package com.sweetspot.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
// ********  MediaController commented out; using custom buttons instead
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.sweetspot.shared.Definitions;
import com.sweetspot.shared.Metadata;

import javax.net.SocketFactory;

public class SweetSpotPlayer extends Activity
        implements OnSeekBarChangeListener {

    // Constants used by the media player
    private static final int BYTES_TO_READ = 50 * 1024;
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int FORWARD_TIME = 5000;
    private static final int BACKWARD_TIME = 5000;

    // Variables used by the media player
    public TextView songName, duration;
    private double timeElapsed = 0, finalTime = 0;
    private Handler durationHandler = new Handler();
    private SeekBar seekbar;
    private long totalRead = 0;
    private VideoView mediaPlayer;
    private MediaController mediaController;

    // Low-level file buffering implementation variables
    private byte[] songBuffer;
    private volatile int recv;

    // Data passed in from main
    private String filepath;
    private Metadata metadata = new Metadata();
    private ServerEntryData entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        //initialize views
        initializeViews();
    }

    public void initializeViews() {

        // Locate video view in activity_media_player
        mediaPlayer = (VideoView) findViewById(R.id.myMedia);

        // Set Song Name on screen
        songName = (TextView) findViewById(R.id.songName);
        songName.setText(metadata.title);

        // Set media source via controller
        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(mediaPlayer);
        mediaPlayer.setMediaController(mediaController);

        // Set up seek bar within view
        duration = (TextView) findViewById(R.id.songDuration);
        int seconds = metadata.length % 60;
        duration.setText("0:00 / " + (metadata.length / 60) + ":" + (seconds < 10 ? "0" : "") + seconds);
        seekbar = (SeekBar) findViewById(R.id.seekBar);

        // Listener
        seekbar.setOnSeekBarChangeListener(this);

        // Signal controller to play music from input file on a new thread
        new PlaySongFromInternet().execute();
    }

    private class PlaySongFromInternet extends AsyncTask<Void, Void, Void> {
        @Override
        public Void doInBackground(Void ... params) {
            try {
                // Create a buffered input stream
                if (!entry.isDropBoxServer()) {

                    // Establish connection with the server
                    Socket sock = SocketFactory.getDefault().createSocket(entry.url, entry.port);
                    ObjectOutputStream objout = new ObjectOutputStream(sock.getOutputStream());
                    InputStream in = sock.getInputStream();
                    ObjectInputStream objin = new ObjectInputStream(in);

                    // Initiate get songfile transaction
                    objout.writeObject(Definitions.TransactionType.GET_SONGFILE);
                    objout.writeObject(filepath);
                    objout.flush();
                    long size = objin.readLong();
                    songBuffer = new byte[(int) size];
                    recv = 0;

                    // Initiate player thread


                    // Read file into buffer
                    while(recv < size) {
                        int bytes_read = in.read(songBuffer, recv, (int) (size - recv));
                        recv += bytes_read;
                    }

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            songName = (TextView) findViewById(R.id.songName);
//                            songName.setText("Completed!");
//                        }
//                    });

                } else {
                    // Drop box TODO
                }
            } catch(IOException e) {
                String s = "What happened: ";
                for(StackTraceElement s2 : e.getStackTrace()) {
                    s += s2 + "\t\t";
                }
                Log.e(e.getMessage(), s);
            }

            return null;
        }
    }

    // Play song
    public void play(View view) {

        // Start mediaPlayer
        mediaPlayer.start();

        // Set max song time within view
        finalTime = mediaPlayer.getDuration();
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
            timeElapsed = mediaPlayer.getCurrentPosition();

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
        mediaPlayer.pause();
    }

    // Go forward at forwardTime seconds
    public void forward(View view) {

        // Check if we can go forward at forwardTime seconds before song ends
        if ((timeElapsed + FORWARD_TIME) <= finalTime) {
            timeElapsed = timeElapsed + FORWARD_TIME;

            // Seek to the exact second of the track
            mediaPlayer.seekTo((int) timeElapsed);
        }
    }

    // Go backward at backwardTime seconds
    public void rewind(View view) {

        //Check if we can go backward at backwardTime seconds
        if ((timeElapsed - BACKWARD_TIME) >= 0 ) {
            timeElapsed = timeElapsed - BACKWARD_TIME;

            //Seek to the exact second of the track
            mediaPlayer.seekTo((int) timeElapsed);
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
        mediaPlayer.seekTo(currentPosition);

        // Update seek bar again
        updateSeekBar();
    }

}