package com.hmdglobal.app.camera.beauty.component;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorEventUtil implements SensorEventListener {
    private Sensor mSensor;
    private SensorManager mSensorManager;
    public int orientation = 0;

    public SensorEventUtil(Activity activity) {
        this.mSensorManager = (SensorManager) activity.getSystemService("sensor");
        this.mSensor = this.mSensorManager.getDefaultSensor(1);
        this.mSensorManager.registerListener(this, this.mSensor, 3);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        SensorEvent sensorEvent = event;
        if (sensorEvent.sensor != null && sensorEvent.sensor.getType() == 1) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            if (((double) sensorEvent.values[2]) < 6.93672028188116d) {
                float y2 = y;
                if (((double) x) >= 6.93672028188116d) {
                    this.orientation = 1;
                } else if (((double) x) <= -6.93672028188116d) {
                    this.orientation = 2;
                } else if (((double) y2) <= -6.93672028188116d) {
                    this.orientation = 3;
                } else {
                    this.orientation = 0;
                }
            } else if (((double) x) >= 4.905d) {
                this.orientation = 1;
            } else if (((double) x) <= -4.905d) {
                this.orientation = 2;
            } else if (((double) y) <= -4.905d) {
                this.orientation = 3;
            } else {
                this.orientation = 0;
            }
        }
    }

    public void unRegister() {
        this.mSensorManager.unregisterListener(this);
    }
}
