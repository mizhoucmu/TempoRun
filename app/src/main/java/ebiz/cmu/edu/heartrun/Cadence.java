package ebiz.cmu.edu.heartrun;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;


public class Cadence extends ActionBarActivity implements SensorEventListener {

    TextView avgCadenceTv;
    Button resetBtn;
    long first = 0l;
    long last = 0l;
    long lastrecord = 0l;
    long current = 0l;
    int steps = 0;
    private long stopInterval = 50000;


    private SensorManager sensorManager;
    private Vibrator vibrator;
    private final String TAG = "---";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadence);
        avgCadenceTv = (TextView) findViewById(R.id.avg_cadence);
        resetBtn = (Button) findViewById(R.id.reset);
        resetBtn.setText("Reset");
        resetBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            reset();
                                        }
                                    }
        );
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (sensorManager != null) {// 注册监听器
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cadence, menu);
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
        float[] values = event.values;
        float x = values[0]; // x轴方向的重力加速度，向右为正
        float y = values[1]; // y轴方向的重力加速度，向前为正
        float z = values[2]; // z轴方向的重力加速度，向上为正

        // 一般在这三个方向的重力加速度达到40就达到了摇晃手机的状态。
        int medumValue = 19;// 三星 i9250怎么晃都不会超过20，没办法，只设置19了
//        if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) {

        if (Math.abs(y) > medumValue && y > 0) {
            Log.i(TAG, "Steps: " + steps + " [" + x + "；" + y + "；" + z + "]");
            boolean isValid = false;
            long interval = 500l;
            if (Math.abs(y) > 30) {
                interval = 300l;
            }

            Date now = new Date();
            current = now.getTime();
            if (lastrecord == 0l) {
                isValid = true;
            } else {
                if ((current - lastrecord) > 5000) {

                }
                if ((current - lastrecord) > interval) {
                    isValid = true;
                }
            }

            if (isValid) {
                steps++;
//                vibrator.vibrate(100);
                lastrecord = current;

                if (first == 0l) {
                    first = lastrecord;
                } else {
                    last = lastrecord;
                    Log.d(TAG, "first:" + String.valueOf(first));
                    Log.d(TAG, "last:" + String.valueOf(last));
                    int avg = (steps * 1000 * 60) / (int) (last - first); // steps per min
                    Log.d(TAG, "Average step per min:" + avg);
                    avgCadenceTv.setText("Total Steps: " + steps + " Average pace: " + String.valueOf(avg));
                }
            }
        }
    }


    private void reset() {
        first = 0l;
        last = 0l;
        steps = 0;
        avgCadenceTv.setText("0");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
