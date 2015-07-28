package ebiz.cmu.edu.heartrun.Twitter;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import ebiz.cmu.edu.heartrun.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterActivity extends ActionBarActivity {

    Button buttonUpdateStatus, buttonLogout;
    //EditText editTextStatus;
    TextView textViewStatus, textViewUserName;
    public static String message;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter);
        Bundle extras = getIntent().getExtras();
        message = extras.getString("message");
        initializeComponent();
        initControl();
    }
    private void initControl() {
        Uri uri = getIntent().getData();

        if (uri != null && uri.toString().startsWith(ConstantValues.TWITTER_CALLBACK_URL)) {
            String verifier = uri.getQueryParameter(ConstantValues.URL_PARAMETER_TWITTER_OAUTH_VERIFIER);
            new TwitterGetAccessTokenTask().execute(verifier);
        } else
            new TwitterGetAccessTokenTask().execute("");
    }

    private void initializeComponent() {
        buttonUpdateStatus = (Button) findViewById(R.id.buttonUpdateStatus);
        // buttonLogout = (Button) findViewById(R.id.buttonLogout);
        //editTextStatus = (EditText) findViewById(R.id.editTextStatus);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewUserName = (TextView) findViewById(R.id.textViewUserName);
        buttonUpdateStatus.setOnClickListener(buttonUpdateStatusOnClickListener);
        // buttonLogout.setOnClickListener(buttonLogoutOnClickListener);
    }

    private View.OnClickListener buttonUpdateStatusOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //String status = editTextStatus.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            String currentDateTime = sdf.format(new Date());
            String status = "Check In:" +  message + ", "+ currentDateTime + " http://www.pvgp.org";

            //String status =message;
            if (status.trim()!=null) {
                new TwitterUpdateStatusTask().execute(status);
                Toast.makeText(getApplicationContext(), "tweet Time", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Please enter a status", Toast.LENGTH_SHORT).show();
            }

        }
    };

    class TwitterGetAccessTokenTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPostExecute(String userName) {
            textViewUserName.setText(Html.fromHtml("<b> Welcome " + userName + "</b>"));
        }

        @Override
        protected String doInBackground(String... params) {

            Twitter twitter = TwitterUtil.getInstance().getTwitter();
            RequestToken requestToken = TwitterUtil.getInstance().getRequestToken("video");
            if (requestToken == null) {
                Log.d("TAG","requestToken == null");
            }
            if (params[0].trim().length()!=0) {
                try {
                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, params[0]);
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, accessToken.getToken());
                    editor.putString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET, accessToken.getTokenSecret());
                    editor.putBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, true);
                    editor.commit();
                    return twitter.showUser(accessToken.getUserId()).getName();
                } catch (TwitterException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String accessTokenString = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
                String accessTokenSecret = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET, "");
                AccessToken accessToken = new AccessToken(accessTokenString, accessTokenSecret);
                try {
                    TwitterUtil.getInstance().setTwitterFactory(accessToken);
                    return TwitterUtil.getInstance().getTwitter().showUser(accessToken.getUserId()).getName();
                } catch (TwitterException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    class TwitterUpdateStatusTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(getApplicationContext(), "Tweet successfully", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "Tweet failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String accessTokenString = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
                String accessTokenSecret = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET, "");

                if (accessTokenString.trim()!=null && accessTokenSecret.trim()!=null) {
                    AccessToken accessToken = new AccessToken(accessTokenString, accessTokenSecret);
                    twitter4j.Status status = TwitterUtil.getInstance().getTwitterFactory().getInstance(accessToken).updateStatus(params[0]);
                    return true;
                }

            } catch (TwitterException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return false;  //To change body of implemented methods use File | Settings | File Templates.

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_twitter, menu);
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
