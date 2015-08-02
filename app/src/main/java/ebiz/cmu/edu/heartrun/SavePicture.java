package ebiz.cmu.edu.heartrun;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ebiz.cmu.edu.heartrun.Controller.MyDevice;
import ebiz.cmu.edu.heartrun.Twitter.ConstantValues;
import ebiz.cmu.edu.heartrun.Twitter.OAuthActivity;
import ebiz.cmu.edu.heartrun.Twitter.TwitterUtil;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SavePicture extends ActionBarActivity {
    ImageView imgView;
    ImageButton twitterButton;
    String pic_path;
    Bitmap bitmap;


    Context context;
    public static int MEDIA_TYPE_IMAGE = 1;
    public static int MEDIA_TYPE_VIDEO = 2;

    private static final String TAG = "---------";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.tweet_preview);
        imgView = (ImageView) findViewById(R.id.pic_to_save);
        showPicture();
        tweet();


//Twitter
        twitterButton = (ImageButton) findViewById(R.id.twitter);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tweet();
            }
        });


    }


    private void tweet() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean loggedIn = sharedPreferences.getBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, false);
//        boolean loggedIn = false;
        Log.d("===", "loggedIn ???" + loggedIn);

        if (!loggedIn) {
            dialog();
        } else {
            logIn();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(ConstantValues.NEED_TO_POST, false);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save_picture, menu);
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

    private void rescanMedia(String filepath) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        MediaScannerConnection.scanFile(context, new String[]{filepath}, null, new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
//                Log.i(TAG, "Scan completed");
//                Log.i(TAG, "path:" + path);
//                Log.i(TAG, "URI" + uri);
            }
        });
    }


    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "homework");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("--------", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void showPicture() {
        pic_path = (String) getIntent().getExtras().get("path");
        if (pic_path == null || pic_path.isEmpty()) {
            initControl();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            pic_path = sharedPreferences.getString(ConstantValues.LAST_MEDIA_PATH, "");
        }
        bitmap = BitmapFactory.decodeFile(pic_path);
        if (bitmap == null) {
            Log.d(TAG, "Can't load this picture:" + pic_path);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        imgView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width / 2, height / 2, false));
    }


    private void logIn() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean loggedIn = sharedPreferences.getBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, false);
        if (!sharedPreferences.getBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, false)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, true);
            editor.putBoolean(ConstantValues.NEED_TO_POST, true);
            editor.putString(ConstantValues.LAST_MEDIA_PATH, pic_path);
            editor.commit();
            new TwitterAuthenticateTask().execute();
        } else {
            String deviceName = MyDevice.getDeviceName();
            String myVersion = android.os.Build.VERSION.RELEASE;
            Date now = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("EST"));
            String timeStamp = df.format(now) + " EST";
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String miles = sharedPreferences.getString(ConstantValues.MILES, "0");

            String status = "#HeartRun I just finished " + miles + " Miles" + " at " + timeStamp;
            new TwitterUpdateStatusTask().execute(status);
            Intent run = new Intent(SavePicture.this,Run.class);
            startActivity(run);
        }
    }


    class TwitterGetAccessTokenTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            Twitter twitter = TwitterUtil.getInstance().getTwitter();
            RequestToken requestToken = TwitterUtil.getInstance().getRequestToken("PIC");
            //if (!StringUtil.isNullOrWhitespace(params[0])) {
            if (params[0].trim().length() != 0) {
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
            if (result) {
                Toast.makeText(getApplicationContext(), "Tweet successfully", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Tweet failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String accessTokenString = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
                String accessTokenSecret = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET, "");
                String consumerKey = ConstantValues.TWITTER_CONSUMER_KEY;
                String consumerSecret = ConstantValues.TWITTER_CONSUMER_SECRET;


                if (accessTokenString.trim() != null && accessTokenSecret.trim() != null) {
                    AccessToken accessToken = new AccessToken(accessTokenString, accessTokenSecret);
                    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
                    configurationBuilder.setOAuthConsumerKey(consumerKey);
                    configurationBuilder.setOAuthConsumerSecret(consumerSecret);
                    configurationBuilder.setOAuthAccessToken(accessTokenString);
                    configurationBuilder.setOAuthAccessTokenSecret(accessTokenSecret);
                    Configuration configuration = configurationBuilder.build();
                    Twitter twitter = new TwitterFactory(configuration).getInstance();


//                    File pictureFile = new File(pic_path);
//                    PhotoUpload upload = new PhotoUploadFactory(configuration).getInstance(MediaProvider.TWITTER);
//                    String media_id = upload.upload(pictureFile, params[0]);

//                    StatusUpdate status = new StatusUpdate(params[0]);
//                    long[] media_ids = new long[1];
//                    media_ids[0] = Long.parseLong(media_id);
//                    status.setMediaIds(media_ids);


                    StatusUpdate status = new StatusUpdate(params[0]);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    status.setMedia("mediaName", bs);  // set the image to be uploaded here.
                    twitter4j.Status result = twitter.updateStatus(status);

                }
                return true;

            } catch (TwitterException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return false;
            }

        }
    }


    protected void dialog() {


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("This App would like to access your twitter account on behalf of you");
        builder.setTitle("Request Twitter Access");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                logIn();
            }
        });
        builder.setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Log.d(TAG, "Twitter Account Access is not allowed");
            }
        });
        builder.create().show();

    }

    private boolean isUseWebViewForAuthentication = false;

    class TwitterAuthenticateTask extends AsyncTask<String, String, RequestToken> {
        @Override
        protected void onPostExecute(RequestToken requestToken) {
            if (requestToken != null) {
                if (!isUseWebViewForAuthentication) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), OAuthActivity.class);
                    intent.putExtra(ConstantValues.STRING_EXTRA_AUTHENCATION_URL, requestToken.getAuthenticationURL());
                    startActivity(intent);
                }
            } else {
                Log.d(TAG, "requestToken = null");
            }
        }

        @Override
        protected RequestToken doInBackground(String... params) {
            return TwitterUtil.getInstance().getRequestToken("PIC");
        }
    }

    private void initControl() {

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(ConstantValues.TWITTER_CALLBACK_URL)) {
            String verifier = uri.getQueryParameter(ConstantValues.URL_PARAMETER_TWITTER_OAUTH_VERIFIER);
            new TwitterGetAccessTokenTask().execute(verifier);
        } else
            new TwitterGetAccessTokenTask().execute("");
    }
}


