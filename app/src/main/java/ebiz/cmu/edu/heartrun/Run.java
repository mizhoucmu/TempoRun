package ebiz.cmu.edu.heartrun;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;


import java.util.Date;


public class Run extends ActionBarActivity implements SensorEventListener, PlayerNotificationCallback, ConnectionStateCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMyLocationChangeListener, ResultCallback<Status> {
    private final String TAG = "======";
    private final int STATUS_CALMDOWN = 0;
    private final int STATUS_WARMUP = 1;
    private final int STATUS_PEAK = 2;
    private final int WARMUP_THRESHOLD = 50;
    private final int PEAK_THRESHOLD = 80;
    private final long ONE_MIN = 60000l;
    private final long TEN_SEC = 10000l;
    private final int RECORD_WINDOW = 10;
    private final int TIMES_REQUIRED_TO_CHANGE = 3; //防止歌曲被change的频率太高，设置必须request要求多少次以后才能换歌
    private final int MIN_ACC = 10;
    private final long MIN_INTERVAL = 500l;


    private ImageButton musicOnButton;
    private ImageButton heartOnButton;

    /**
     * Running State
     */
    private final int RUNNING_READY = 0;
    private final int RUNNING_STARTED = 1;
    private final int RUNNING_PAUSED = 2;
    private final int RUNNING_STOPPED = 3;
    private int runningState = RUNNING_READY;

    /**
     * Running Tempo
     */
    private int tempo;
    TextView tempoTV;
    long lastHitTime = 0l;
    long current = 0l;
    private long stopInterval = TEN_SEC;
    private SensorManager sensorManager;
    private boolean heartOn = true;
    private int curTempo = 0;
    private long[] intervals = new long[RECORD_WINDOW];
    private int cursor = -1;
    long sumInterval = 0l;


    /**
     * Spotify Login
     */
    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "8babe6b09e98463793a4e7f271814d80";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "oauth://ebiz.cmu.edu.heartrun";


    /**
     * Spotify Music List
     */
    private String[] uris = new String[3];
    private final String coolList = "spotify:user:charlotepirkis4:playlist:5rqxR5EqAMxAjADUURi9rc";
    private final String warmupList = "spotify:user:spotify:playlist:16BpjqQV1Ey0HeDueNDSYz";
    private final String heatList = "spotify:user:playalistic-sweden:playlist:3bqLq0LzRzvQ0dIkRlYtKS";


    /**
     * Spotify Music Stream
     */
    private Player mPlayer = null;
    ImageButton nextSong;
    private boolean musicOn = false;
    private boolean isPlaying = false;
    int changeRequests = 0;

    /**
     * Location and running distance
     */

    private GoogleApiClient mLocationClient = null;
    private Location lastLocation = null;
    private double total_miles = 0;
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(1000)         // 1 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    private TextView totalMileTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        buildGoogleApiClient();
        mLocationClient.connect();
        totalMileTV = (TextView) findViewById(R.id.total_miles_val);
        setRunning();


        musicOnButton = (ImageButton) findViewById(R.id.music);
        musicOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicOn = !musicOn;
                if (musicOn) {
                    musicOnButton.setBackgroundResource(R.drawable.music);
                    if (mPlayer != null) {
                        mPlayer.setShuffle(true);
                        Log.d(TAG, "curTempo = " + curTempo);
                        playOnTempo(tempo);
                    }

                } else {
                    musicOnButton.setBackgroundResource(R.drawable.nomusic);
                    if (mPlayer != null) {
                        mPlayer.pause();
                        isPlaying = false;
                    }
                }
            }
        });

        heartOnButton = (ImageButton) findViewById(R.id.heart);
        heartOnButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                heartOn = !heartOn;
                if (heartOn) {
                    heartOnButton.setBackgroundResource(R.drawable.heart);
                } else {
                    heartOnButton.setBackgroundResource(R.drawable.sadheart);
                }
            }
        });

        tempoTV = (TextView) findViewById(R.id.tempo_value);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        if (sensorManager != null) {// 注册监听器
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }


        // Spotify Music Stream
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        uris[0] = coolList;
        uris[1] = warmupList;
        uris[2] = heatList;

        nextSong = (ImageButton) findViewById(R.id.nextSong);
        nextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicOn && mPlayer != null) {
                    mPlayer.skipToNext();
                }
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_run, menu);
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
    public void onSensorChanged(SensorEvent event) {
        // 传感器信息改变时执行该方法

        Date now = new Date();
        current = now.getTime();
        if (lastHitTime != 0l && (current - lastHitTime) > TEN_SEC) {
            reset();
        }

        float[] values = event.values;
        float x = values[0]; // x轴方向的重力加速度，向右为正
        float y = values[1]; // y轴方向的重力加速度，向前为正
        float z = values[2]; // z轴方向的重力加速度，向上为正


        if (Math.abs(y) > MIN_ACC && y > 0) {
            boolean isValid = false;
            long threshold = MIN_INTERVAL;
            if (Math.abs(y) > 18) {
                threshold = 300l;
            }
            if (lastHitTime == 0l) {
                isValid = true;
            } else {
                if ((current - lastHitTime) > stopInterval) { //中间间隔的时间太长就会被清零
                    reset();
                    isValid = true; //当作第一次
                } else if ((current - lastHitTime) > threshold) { //两个hit之间要间隔500ms才算
                    isValid = true;
                }
            }

            if (isValid) {
                if (lastHitTime == 0l) {
                    lastHitTime = current;
                } else {
                    cursor++;
                    sumInterval -= intervals[cursor % RECORD_WINDOW];
                    long current_interval = current - lastHitTime;
                    intervals[cursor % RECORD_WINDOW] = current_interval;
                    sumInterval += intervals[cursor % RECORD_WINDOW];
                    long avgInterval = sumInterval / (cursor >= RECORD_WINDOW ? RECORD_WINDOW : cursor + 1);
                    tempo = (int) (ONE_MIN / avgInterval);
                    Log.d(TAG, "Cursor: " + cursor + ", one time Tempo: " + ONE_MIN / current_interval + "  Current Inteval: " + current_interval + "         tempo: " + tempo);
                    tempoTV.setText(String.valueOf(tempo));
                    playOnTempo(tempo);
                    lastHitTime = current;
                }

            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void playOnTempo(int tempo) {
        Log.d(TAG,"playOnTempo:" + tempo);
        if (mPlayer == null) {
            Log.d(TAG,"mPlayer not ready");
        }
        // change music according to tempo
        if (musicOn && heartOn && mPlayer != null) {
            Log.d(TAG,"READY TO PLAY");
            if (isPlaying == false) {
                Log.d(TAG, "Start to play");
                mPlayer.play(uris[getStage(tempo)]);
                int random = (int) (Math.random() * 10);
                for (int i = 0; i < random; i++) {
                    mPlayer.skipToNext();
                }
                Log.d(TAG, "Going to skip to " + random + " song");
                isPlaying = true;
                curTempo = tempo;
            } else if (getStage(tempo) != getStage(curTempo)) {
                Log.d(TAG, "changeRequests:" + changeRequests);
                if (changeRequests >= TIMES_REQUIRED_TO_CHANGE) {
                    changeRequests = 0;
                    Log.d(TAG, "++++++++++++" + getStage(tempo) + "++++++++++++");
                    Log.d(TAG, "tempo changed:   tempo: " + tempo + ", status: " + getStage(tempo) + ", curtempo: " + curTempo + ", status: " + getStage(curTempo));
                    mPlayer.play(uris[getStage(tempo)]);
                    int random = (int) (Math.random() * 10);
                    for (int i = 0; i < random; i++) {
                        mPlayer.skipToNext();
                    }
                    isPlaying = true;
                    curTempo = tempo;
                } else {
                    changeRequests++;
                }
            }
        }
    }

    private int getStage(int tempo) {
        if (tempo < WARMUP_THRESHOLD) {
            return STATUS_CALMDOWN;
        } else if (tempo < PEAK_THRESHOLD) {
            return STATUS_WARMUP;
        } else {
            return STATUS_PEAK;
        }
    }

    private void reset() {
        changeRequests = 0;
        cursor = -1;
        sumInterval = 0l;
        lastHitTime = 0l;
        for (int i = 0; i < RECORD_WINDOW; i++) {
            intervals[i] = 0l;
        }
        tempo = 0;
        curTempo = 0;
        if (heartOn && musicOn && mPlayer != null) {
            mPlayer.play(uris[0]);
        }
        tempoTV.setText(String.valueOf(curTempo));
    }


    // Spotify Music Stream


    private static final int REQUEST_CODE = 1337;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Log.d("requestCode", requestCode + "");
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(Run.this);
                        mPlayer.addPlayerNotificationCallback(Run.this);
                        mPlayer.setShuffle(true);
                        reset();
                        playOnTempo(curTempo);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }


    private void setRunning() {
        RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.button_layout);
        buttonLayout.removeAllViews();
        switch (runningState) {
            case RUNNING_READY: {
                resetDistance();
                startLocationService();
                Log.d(TAG, "setRunning: Running state: READY");
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                ImageButton playButton = new ImageButton(this);
                playButton.setLayoutParams(params);
                playButton.setBackgroundColor(Color.parseColor("#F2F2F2"));
                playButton.setImageResource(R.drawable.play3);
                buttonLayout.addView(playButton);
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runningState = RUNNING_STARTED;
                        setRunning();
                    }
                });
                break;
            }
            case RUNNING_STARTED: {
                if (mLocationClient != null && mLocationClient.isConnected() == false) {
                    mLocationClient.connect();
                }
                Log.d(TAG, "setRunning: Running state: STARTED");
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                ImageButton pauseButton = new ImageButton(this);
                pauseButton.setLayoutParams(params);
                pauseButton.setBackgroundColor(Color.parseColor("#F2F2F2"));
                pauseButton.setImageResource(R.drawable.pause);
                buttonLayout.addView(pauseButton);
                pauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runningState = RUNNING_PAUSED;
                        setRunning();
                    }
                });
                break;
            }
            case RUNNING_PAUSED: {
                Log.d(TAG, "setRunning: Running state: PAUSED");
                RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params1.addRule(RelativeLayout.CENTER_VERTICAL);
                ImageButton stopButton = new ImageButton(this);
                stopButton.setLayoutParams(params1);
                stopButton.setId(R.id.stop);
                stopButton.setBackgroundColor(Color.parseColor("#F2F2F2"));
                stopButton.setImageResource(R.drawable.smallstop);
                buttonLayout.addView(stopButton);
                stopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runningState = RUNNING_STOPPED;
                        setRunning();
                    }
                });

                RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params2.addRule(RelativeLayout.RIGHT_OF, R.id.stop);
                params2.addRule(RelativeLayout.CENTER_VERTICAL);
                ImageButton resumeButton = new ImageButton(this);
                resumeButton.setLayoutParams(params2);
                resumeButton.setBackgroundColor(Color.parseColor("#F2F2F2"));
                resumeButton.setImageResource(R.drawable.smallplay);
                buttonLayout.addView(resumeButton);
                resumeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runningState = RUNNING_STARTED;
                        setRunning();
                    }
                });
                break;
            }
            case RUNNING_STOPPED: {
                Log.d(TAG, "setRunning: Running state: STOPPED");
                stopLocationService();
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                ImageButton shareButton = new ImageButton(this);
                shareButton.setLayoutParams(params);
                shareButton.setBackgroundColor(Color.parseColor("#F2F2F2"));
                shareButton.setImageResource(R.drawable.share);
                buttonLayout.addView(shareButton);
                shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        runningState = RUNNING_READY;
                        setRunning();
                    }
                });

                break;
            }
            default: {
            }
        }
    }

    /**
     * Google Map
     */

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "buildGoogleApiClient");
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        PendingResult<Status> status = LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient,
                REQUEST, this);
        status.setResultCallback(this);

        Log.i(TAG, "onConnected:");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        if (runningState == RUNNING_STARTED) {
            if (lastLocation == null) {
                total_miles = 0;
            } else {
                if (mLocationClient != null && mLocationClient.isConnected()) {

                    double deltaLat = location.getLatitude() - lastLocation.getLatitude();
                    double deltaLong = location.getLongitude() - lastLocation.getLongitude();
                    double dis = Math.sqrt(deltaLat * deltaLat + deltaLong * deltaLong);
                    Log.d(TAG, "Current Location: " + deltaLat + ", " + deltaLong);
                    total_miles += dis;
                    totalMileTV.setText(String.format("%,.2f", total_miles));
                }
            }
            lastLocation = location;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private void resetDistance() {
        total_miles = 0;
        totalMileTV.setText(String.format("%.2f", 0.0));
        lastLocation = null;
    }

    private void startLocationService() {
        if (mLocationClient == null) {
            buildGoogleApiClient();
        }
        if (!mLocationClient.isConnected()) {
            mLocationClient.connect();
        }
    }

    private void stopLocationService() {
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.disconnect();
        }
    }


    public void onMyLocationChange(Location location) {
        Log.d(TAG, "onMyLocationChange");
        if (runningState == RUNNING_STARTED) {
            if (lastLocation == null) {
                total_miles = 0;
            } else {
                if (mLocationClient != null && mLocationClient.isConnected()) {

                    double deltaLat = location.getLatitude() - lastLocation.getLatitude();
                    double deltaLong = location.getLongitude() - lastLocation.getLongitude();
                    double dis = Math.sqrt(deltaLat * deltaLat + deltaLong * deltaLong);
                    Log.d(TAG, "Current Location: " + deltaLat + ", " + deltaLong);
                    total_miles += dis;
                    totalMileTV.setText(String.format("%,.2f", total_miles));
                }
            }
            lastLocation = location;
        }
    }

    @Override
    public void onResult(Status status) {
        Log.d(TAG,"update status: " + status.getStatus() + " " + status.getStatusMessage());
    }
}
