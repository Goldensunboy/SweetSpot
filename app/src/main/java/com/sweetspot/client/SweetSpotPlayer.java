package com.sweetspot.client;

import java.util.concurrent.TimeUnit;
import android.app.Activity;
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

public class SweetSpotPlayer extends Activity
        implements OnSeekBarChangeListener {

    public TextView songName, duration;
    private double timeElapsed = 0, finalTime = 0;
    private int forwardTime = 2000, backwardTime = 2000;
    private Handler durationHandler = new Handler();
    private SeekBar seekbar;
    private String path; // holds the path of song to be played
    private VideoView mediaPlayer;
    // ********  MediaController commented out; using custom buttons instead
    // private MediaController mediaControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the layout of the Activity
        setContentView(R.layout.activity_media_player);

        //initialize views
        initializeViews();
    }

    public void initializeViews(){

        // Initialize mediaView and mediaControl to prepare for playing video/audio
        // Locate video view in activity_media_player
        mediaPlayer = (VideoView) findViewById(R.id.myMedia);

        // ********  MediaController commented out; using custom buttons instead
        // Make a new mediaController
        // mediaControl = new MediaController(this);
        // Anchor the media control with the mediaView
        // mediaControl.setAnchorView(mediaPlayer);
        // Make the media control reference the mediaView content
        // mediaPlayer.setMediaController(mediaControl);

        // Set Song Path
        // Set path as correct URL (currently hard-coded)
        path = "http://trixie.no-ip.info/content/music/Gamma Ray - Majestic/01 - My Temple.mp3";

        // Parse current URL string into Uri format; store as mediaUri
        Uri mediaUri = Uri.parse(path);

        // Set Song Name on screen
        songName = (TextView) findViewById(R.id.songName);
        String nameString = mediaUri.getLastPathSegment();
        songName.setText(nameString);

        // Set media URL within mediaView
        mediaPlayer.setVideoURI(mediaUri);
        // Set up seek bar within view
        duration = (TextView) findViewById(R.id.songDuration);
        seekbar = (SeekBar) findViewById(R.id.seekBar);

        // Listener
        seekbar.setOnSeekBarChangeListener(this);
    }

    // play song
    public void play(View view) {
        // Start mediaPlayer
        mediaPlayer.start();

        // Set max song time within view
        finalTime = mediaPlayer.getDuration();
        seekbar.setMax((int) finalTime);

        // Update Seek Bar
        updateSeekBar();
        //durationHandler.postDelayed(updateSeekBarTime, 100);
    }

    // update seek bar
    public void updateSeekBar() {
        durationHandler.postDelayed(updateSeekBarTime, 100);
    }

    //handler to change seekBarTime
    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            //get current position
            timeElapsed = mediaPlayer.getCurrentPosition();
            //set seek bar progress
            seekbar.setProgress((int) timeElapsed);
            //set time remaining
            double timeRemaining = finalTime - timeElapsed;
            long min_elapsed = TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed);
            long sec_elapsed = TimeUnit.MILLISECONDS.toSeconds((long) timeElapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed));
            long min_total = TimeUnit.MILLISECONDS.toMinutes((long) finalTime);
            long sec_total = TimeUnit.MILLISECONDS.toSeconds((long) finalTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime));
            duration.setText(String.format("%d:%d / %d:%d", min_elapsed, sec_elapsed, min_total, sec_total));

            //repeat again in 100 milliseconds
            durationHandler.postDelayed(this, 100);
        }
    };

    // pause media
    public void pause(View view) {
        mediaPlayer.pause();
    }

    // go forward at forwardTime seconds
    public void forward(View view) {
        //check if we can go forward at forwardTime seconds before song ends
        if ((timeElapsed + forwardTime) <= finalTime) {
            timeElapsed = timeElapsed + forwardTime;

            //seek to the exact second of the track
            mediaPlayer.seekTo((int) timeElapsed);
        }
    }

    // go backward at backwardTime seconds
    public void rewind(View view) {
        //check if we can go backward at backwardTime seconds
        if ((timeElapsed - backwardTime) >= 0 ) {
            timeElapsed = timeElapsed - backwardTime;

            //seek to the exact second of the track
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
        // forward or backward to position selected
        mediaPlayer.seekTo(currentPosition);
        // update seek bar again
        updateSeekBar();
    }

}