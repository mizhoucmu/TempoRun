package ebiz.cmu.edu.heartrun;

/**
 * Created by julie on 7/27/15.
 */


import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Activity activity;
    private static final String TAG = "----------";

    public CameraPreview(Activity activity, Context context, Camera camera) {
        super(context);
        mCamera = camera;
        this.activity = activity;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
//            correctOrientation();
            initPreview();
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e("-------------", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);

//            correctOrientation();

            initPreview();
            mCamera.startPreview();

            Log.e("tag", "Preview Started");

        } catch (Exception e) {
            Log.d("Tag", "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Returns the device rotation
     *
     * @return ROTATION_0 = 0, ROTATION_90 = 90, ROTATION_180 = 180, ROTATION_270 = 270
     */
    private int getRotationOfActivity() {

        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    /**
     * Returns the device orientation
     *
     * @return 1 (Portrait), 2 (Landscape)
     */
    private int getOrientation() {

        return getResources().getConfiguration().orientation;
    }

    /**
     * Correct the orientation of the Preview based on the Device Orientation and Rotation
     */
    private void correctOrientation() {
        if (getOrientation() == 1) {
            mCamera.setDisplayOrientation(90);
        } else if (getOrientation() == 2 && getRotationOfActivity() == 3) {
            mCamera.setDisplayOrientation(180);
        }
    }

    private void initPreview() {
        if (mCamera != null && mHolder.getSurface() != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Throwable t) {
                Log.e(TAG, "Exception in setPreviewDisplay()", t);
                Toast.makeText(getContext(), t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }

            try {
                Camera.Parameters parameters = mCamera.getParameters();

                Camera.Size size = getBestPreviewSize();
                Camera.Size pictureSize = getSmallestPictureSize(parameters);

                Display display = activity.getWindowManager().getDefaultDisplay();

                switch (display.getRotation()) {
                    case Surface.ROTATION_0: // This is display orientation
                        if (size.height > size.width)
                            parameters.setPreviewSize(size.height, size.width);
                        else parameters.setPreviewSize(size.width, size.height);
                        mCamera.setDisplayOrientation(90);
                        break;
                    case Surface.ROTATION_90:
                        if (size.height > size.width)
                            parameters.setPreviewSize(size.height, size.width);
                        else parameters.setPreviewSize(size.width, size.height);
                        mCamera.setDisplayOrientation(0);
                        break;
                    case Surface.ROTATION_180:

                        if (size.height > size.width)
                            parameters.setPreviewSize(size.height, size.width);
                        else parameters.setPreviewSize(size.width, size.height);
                        mCamera.setDisplayOrientation(270);
                        break;
                    case Surface.ROTATION_270:

                        if (size.height > size.width)
                            parameters.setPreviewSize(size.height, size.width);
                        else parameters.setPreviewSize(size.width, size.height);
                        mCamera.setDisplayOrientation(180);
                        break;
                }


                parameters.setPictureSize(size.width, size.height);
                parameters.setPictureFormat(ImageFormat.JPEG);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Camera.Size getBestPreviewSize() {
        Camera.Size result = null;
        Camera.Parameters p = mCamera.getParameters();
        for (Camera.Size size : p.getSupportedPreviewSizes()) {

            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }

        return result;
    }

    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }
        return (result);
    }
}
