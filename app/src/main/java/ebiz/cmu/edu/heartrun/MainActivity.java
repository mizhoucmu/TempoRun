package ebiz.cmu.edu.heartrun;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "8babe6b09e98463793a4e7f271814d80";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "oauth://ebiz.cmu.edu.heartrun";

    private String[] uris = new String[6];

    //    private final String _140 = "spotify:user:1265369479:playlist:7dCl7w0dpqZNU9usw5k9Xr";
    private final String _140 = "spotify:user:spotify:playlist:16BpjqQV1Ey0HeDueNDSYz";
    private final String _140_150 = "spotify:user:curvesoosterhout:playlist:68yU417kO4LML8gSnwLxQ3";
    private final String _150_160 = "spotify:user:lisahoving:playlist:46r0SqD0z80Su48AJTmVxE";
    private final String _160_170 = "spotify:user:lisahoving:playlist:4r0FcKtHukV06cwQSi9mNZ";
    private final String _170_180 = "spotify:user:spotify:playlist:2KS4O4BjHy7twHMuuXPfId";
    private final String _180_ = "spotify:user:1178333802:playlist:6nascdzBeEo8hd2mIbzLOU";

    private Player mPlayer;
    int index;

    Button nextButton;
    TextView bpm;

    // Request code that will be used to verify if the result comes from correct activity
// Can be any integer
    private static final int REQUEST_CODE = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        uris[0] = _140;
        uris[1] = _140_150;
        uris[2] = _150_160;
        uris[3] = _160_170;
        uris[4] = _170_180;
        uris[5] = _180_;
    }


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
                        bpm = (TextView) findViewById(R.id.bpm);
                        index = 0;
                        bpm.setText(String.valueOf(index));
                        nextButton = (Button) findViewById(R.id.nextSong);
                        nextButton.setText("Next Song");
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                        mPlayer.setShuffle(true);

                        mPlayer.play(uris[index]);
                        nextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                index = (index + 1) % 6;
                                mPlayer.setShuffle(true);
                                mPlayer.play(uris[index]);
                                bpm.setText(String.valueOf(index));
//                                mPlayer.skipToNext();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
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

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}