package ebiz.cmu.edu.heartrun;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * Created by julie on 7/27/15.
 */
public class ImageTools {
    public static Bitmap toBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static Bitmap rotate(Bitmap in, int angle) {
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(in, 0, 0, in.getWidth(), in.getHeight(), mat, true);
    }

}