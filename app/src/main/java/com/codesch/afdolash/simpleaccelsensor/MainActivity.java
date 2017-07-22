package com.codesch.afdolash.simpleaccelsensor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

import at.markushi.ui.CircleButton;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private CircleButton btn_lock;
    private int RESULT;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private ActivityManager mActivityManager;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Membuat notifikasi bar transparan
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        changeStatusBarColor();

        // Inisialisasi activity manager
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // Memanggil component myAdmin
        mComponentName = new ComponentName(this, MyAdmin.class);

        // Inisialisasi device policy
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Inisialisasi sensor yang akan digunakan
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            // Mendapatkan list sensor gravity
            List<Sensor> gravSensor = mSensorManager.getSensorList(Sensor.TYPE_GRAVITY);

            for (int i = 0; i < gravSensor.size(); i++) {
                if ((gravSensor.get(i).getVendor().contains("Google Inc.")) && (gravSensor.get(i).getVersion() == 3)) {
                    // Menggunakan gravity sensor milik Google Inc. dengan versi ke-3
                    mSensor = gravSensor.get(i);
                }
            }
        }

        if (mSensor == null) {
            // Jika tidak memiliki sensr gravity diatas,
            // maka aplikasi akan menggunakan sensor Accelerometer
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            } else {
                // Jika smartphone tidak memiliki sensor ini, aplikasi tidak bisa digunakan
                Toast.makeText(this, "Your device not supported to use this application!", Toast.LENGTH_LONG).show();
            }
        }

        // Inisialisasi dan event button lock
        btn_lock = (CircleButton) findViewById(R.id.btn_lock);
        btn_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RESULT == 0) {
                    // Memanggi activity result dan hak akses Admin
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why this needs to be added.");
                    startActivityForResult(intent, 1);
                } else {
                    // Mereset nilai RESULT
                    RESULT = 0;
                    btn_lock.setColor(Color.parseColor("#3F51B5"));
                    btn_lock.setImageResource(R.drawable.ic_phonelink_lock_white_24dp);

                    mDevicePolicyManager.removeActiveAdmin(mComponentName);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Menggunakan request code untuk mengganti nilai RESULT
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    // Mengganti nilai RESULT
                    RESULT = 1;
                    btn_lock.setColor(Color.parseColor("#FF4081"));
                    btn_lock.setImageResource(R.drawable.ic_phonelink_erase_white_24dp);

                    Toast.makeText(this, "Admin enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Admin enable failed!", Toast.LENGTH_SHORT).show();
                }

                return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Mengecek nilai RESULT pada button lock
        if (RESULT == 0) {
            btn_lock.setColor(Color.parseColor("#3F51B5"));
            btn_lock.setImageResource(R.drawable.ic_phonelink_lock_white_24dp);
        } else {
            btn_lock.setColor(Color.parseColor("#FF4081"));
            btn_lock.setImageResource(R.drawable.ic_phonelink_erase_white_24dp);
        }

        // Memanggil sensor dan menjalankannya
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Menghilangkan sensor yang aktif
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Menghilangkan sensor yang aktif
        mSensorManager.unregisterListener(this);

        // Menonaktifkan admin policy
        mDevicePolicyManager.removeActiveAdmin(mComponentName);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Mendapatkan data dari sensor
        int xAxis = (int) sensorEvent.values[0];
        int yAxis = (int) sensorEvent.values[1];
        int zAxis = (int) sensorEvent.values[2];

        // Mengatur trigger/pemicu untuk melakukan screen lock pada Android
        if (Math.abs(xAxis) == 0 && Math.abs(yAxis) == 0 && Math.abs(zAxis) == 9) {
            if (mDevicePolicyManager.isAdminActive(mComponentName)) {
                // Mengunci Android
                mDevicePolicyManager.lockNow();
            }
        } else {
            // Kebalikan dari mengunci
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
