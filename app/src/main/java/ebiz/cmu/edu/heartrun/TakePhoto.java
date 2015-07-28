package ebiz.cmu.edu.heartrun;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ebiz.cmu.edu.heartrun.Controller.MyDevice;

/**
 * Created by julie on 7/27/15.
 */
public class TakePhoto extends Activity {

    private final String TAG = "======";
    private Camera mCamera;
    private Context context = this;
    private CameraPreview mPreview;
    public Camera.PictureCallback mPicture;
    public static int MEDIA_TYPE_IMAGE = 1;
    public static int MEDIA_TYPE_VIDEO = 2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_picture);

        if (!checkCameraHardware(getApplicationContext())) {
            Log.e("Camera", "Not supported");
        } else {
            Log.d("Camera", "Supported");
        }

        // Create an instance of Camera
        mCamera = getCameraInstance();




        if (mCamera == null) {
            Log.e("GetCameraInstance", "Failed to get a Camera instance.");
            Toast.makeText(getApplicationContext(), "Error opening the camera.",
                    Toast.LENGTH_LONG).show();
        } else {
            // Do nothing
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, this, mCamera);

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                Display display = getWindowManager().getDefaultDisplay();
                int rotation = 0;
                switch (display.getRotation()) {
                    case Surface.ROTATION_0: // This is display orientation
                        rotation = 90;
                        break;
                    case Surface.ROTATION_90:
                        rotation = 0;
                        break;
                    case Surface.ROTATION_180:
                        rotation = 270;
                        break;
                    case Surface.ROTATION_270:
                        rotation = 180;
                        break;
                }
                Bitmap bitmap = ImageTools.toBitmap(data);
                bitmap = ImageTools.rotate(bitmap,rotation);
                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    Log.e("PictureSave", "Error creating media file, check storage permissions: ");
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    printInfo();

                } catch (FileNotFoundException e) {
                    Log.e("PictureSave", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.e("PictureSave", "Error accessing file: " + e.getMessage());
                }
                Intent it = new Intent(context, SavePicture.class);
                it.putExtra("path", pictureFile.getAbsolutePath());
                startActivity(it);
            }
        };

        // Add a listener to the Capture button
        final ImageButton captureButton = (ImageButton) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                        Log.d("Capture", "Picture taken.");
                        captureButton.setEnabled(false);
                    }
                }
        );


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     *
     * @return The camera instance (if successful)
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent it = new Intent(context, Run.class);
            startActivity(it);
        }
        return false;

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

    private void printInfo() {
        String andrewID = "mizhou";
        String deviceName = MyDevice.getDeviceName();
        String myVersion = android.os.Build.VERSION.RELEASE; // e.g. myVersion := "1.6"
        int sdkVersion = android.os.Build.VERSION.SDK_INT;
        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("EST"));
        String timeStamp = df.format(now) + " EST";
        Log.d(TAG, andrewID + " : " + deviceName + " " + myVersion + " API " + sdkVersion + " : " + timeStamp);
    }

}