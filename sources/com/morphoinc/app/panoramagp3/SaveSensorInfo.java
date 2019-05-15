package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.os.SystemClock;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.panoramagp3.Camera2App.DeviceInfo;
import com.morphoinc.core.MorphoSensorFusion.SensorData;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class SaveSensorInfo {
    protected static final String CRLF = "\r\n";
    private static final String LOG_TAG = "SaveSensorInfo";
    protected static final String SEPARATOR = "\t";
    private UseClockType mImageClockType = UseClockType.USE_CLOCK_TYPE_INITIAL;
    private int[] mIndexBase = new int[4];
    private UseClockType mSensorClockType = UseClockType.USE_CLOCK_TYPE_INITIAL;
    private ArrayList<SensorInfoManager> mSensorInfoManagerList;
    private long[] mTimeOffset = new long[4];
    private long mTimestampDiff = 0;

    enum UseClockType {
        USE_CLOCK_TYPE_INITIAL,
        USE_CLOCK_TYPE_REAL,
        USE_CLOCK_TYPE_UNKNOWN
    }

    public SaveSensorInfo(ArrayList<SensorInfoManager> sensorInfoManagerList) {
        for (int i = 0; i < this.mTimeOffset.length; i++) {
            this.mTimeOffset[i] = -1;
            this.mIndexBase[i] = 0;
        }
        this.mSensorInfoManagerList = (ArrayList) sensorInfoManagerList.clone();
    }

    private void setTimestampDiff() {
        long tmp = 0;
        long diff = 0;
        int i = 0;
        while (i < 5) {
            long a = System.nanoTime();
            long b = SystemClock.elapsedRealtimeNanos();
            long c = SystemClock.elapsedRealtimeNanos();
            long d = System.nanoTime();
            if (tmp == 0 || d - a < tmp) {
                tmp = d - a;
                diff = ((((a - b) - c) + d) + 1) >> 1;
            }
            int i2 = i;
            LogFilter.d(LOG_TAG, String.format(Locale.US, "a=%d b=%d c=%d d=%d d-a=%d diff=%d", new Object[]{Long.valueOf(a), Long.valueOf(b), Long.valueOf(c), Long.valueOf(d), Long.valueOf(tmp), Long.valueOf(diff)}));
            i = i2 + 1;
        }
        this.mTimestampDiff = diff;
        this.mSensorClockType = UseClockType.USE_CLOCK_TYPE_INITIAL;
        this.mImageClockType = UseClockType.USE_CLOCK_TYPE_INITIAL;
    }

    private void updateTimestamp(SensorInfoManager sensorInfoManager) {
        if (this.mSensorClockType != UseClockType.USE_CLOCK_TYPE_INITIAL && this.mImageClockType != this.mSensorClockType) {
            sensorInfoManager.imageTimeStamp -= this.mTimestampDiff;
            sensorInfoManager.sensorTimeStamp -= this.mTimestampDiff;
        }
    }

    private void checkUseClockType(boolean sensor, long timestamp) {
        if ((sensor ? this.mSensorClockType : this.mImageClockType) == UseClockType.USE_CLOCK_TYPE_INITIAL) {
            UseClockType type;
            long realNanoTime = SystemClock.elapsedRealtimeNanos();
            long systemNanoTime = System.nanoTime();
            if (Math.abs(realNanoTime - timestamp) < Math.abs(systemNanoTime - timestamp)) {
                type = UseClockType.USE_CLOCK_TYPE_REAL;
            } else {
                type = UseClockType.USE_CLOCK_TYPE_UNKNOWN;
            }
            if (sensor) {
                this.mSensorClockType = type;
            } else {
                this.mImageClockType = type;
            }
            String simpleName = getClass().getSimpleName();
            Locale locale = Locale.US;
            String str = "%s timestamp=%d realNanoTime=%d systemNanoTime=%d type=%s";
            Object[] objArr = new Object[5];
            objArr[0] = sensor ? "Sensor" : "Image";
            objArr[1] = Long.valueOf(timestamp);
            objArr[2] = Long.valueOf(realNanoTime);
            objArr[3] = Long.valueOf(systemNanoTime);
            objArr[4] = type;
            LogFilter.d(simpleName, String.format(locale, str, objArr));
        }
    }

    public void save(Context context, String saveDir, DeviceInfo deviceInfo) {
        File checkSaveDir = new File(saveDir);
        if (!checkSaveDir.exists() && !checkSaveDir.mkdirs()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't create folder!! >>");
            stringBuilder.append(saveDir);
            LogFilter.w(str, stringBuilder.toString());
        } else if (this.mSensorInfoManagerList.isEmpty()) {
            LogFilter.w(LOG_TAG, "Sensor list is empty.");
        } else {
            setTimestampDiff();
            ArrayList<SensorData> gyroData = ((SensorInfoManager) this.mSensorInfoManagerList.get(0)).sensorData[0];
            if (gyroData != null && gyroData.size() > 0) {
                checkUseClockType(true, ((SensorData) gyroData.get(0)).mTimeStamp);
                checkUseClockType(false, ((SensorInfoManager) this.mSensorInfoManagerList.get(0)).imageTimeStamp);
            }
            String saveSGPath = new File(saveDir, "sg.txt").getPath();
            String saveSRPath = new File(saveDir, "sr.txt").getPath();
            String saveSAPath = new File(saveDir, "sa.txt").getPath();
            String saveSummaryPath = new File(saveDir, "input_sensor_info.txt").getPath();
            String saveDevicePath = new File(saveDir, "device_info.txt").getPath();
            saveSensorData(context, saveSGPath, 0);
            saveSensorData(context, saveSRPath, 3);
            saveSensorData(context, saveSAPath, 1);
            for (int i = 0; i < this.mIndexBase.length; i++) {
                this.mIndexBase[i] = 0;
            }
            saveSensorDataSummary(context, saveSummaryPath);
            saveDeviceInfo(context, saveDevicePath, deviceInfo);
        }
    }

    private void saveDeviceInfo(Context context, String filePath, DeviceInfo deviceInfo) {
        File file = null;
        FileWriter fWriter = null;
        BufferedWriter writer = null;
        try {
            file = new File(filePath);
            fWriter = new FileWriter(file, true);
            writer = new BufferedWriter(fWriter);
            long timestamp = (this.mSensorClockType == UseClockType.USE_CLOCK_TYPE_INITIAL || this.mImageClockType == this.mSensorClockType) ? 0 : this.mTimestampDiff;
            writer.write(String.format(Locale.US, "%d%s%f%s%f%s%f%s%f%s%s", new Object[]{Long.valueOf(timestamp), SEPARATOR, Double.valueOf(deviceInfo.aov_h), SEPARATOR, Double.valueOf(deviceInfo.aov_v), SEPARATOR, Double.valueOf(deviceInfo.physical_width), SEPARATOR, Double.valueOf(deviceInfo.physical_height), SEPARATOR, deviceInfo.model}));
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (IOException e22) {
            e22.printStackTrace();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e222) {
                    e222.printStackTrace();
                }
            }
            if (fWriter != null) {
                fWriter.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (fWriter != null) {
                try {
                    fWriter.close();
                } catch (IOException e32) {
                    e32.printStackTrace();
                }
            }
        }
        Camera2App.scanFile(context, file);
    }

    private void saveSensorData(Context context, String filePath, int typeIndex) {
        File file = null;
        FileWriter fWriter = null;
        BufferedWriter writer = null;
        try {
            file = new File(filePath);
            fWriter = new FileWriter(file, true);
            writer = new BufferedWriter(fWriter);
            Iterator it = this.mSensorInfoManagerList.iterator();
            while (it.hasNext()) {
                SensorInfoManager sensorInfoManager = (SensorInfoManager) it.next();
                if (this.mTimeOffset[typeIndex] == -1 && !sensorInfoManager.sensorData[typeIndex].isEmpty()) {
                    this.mTimeOffset[typeIndex] = ((SensorData) sensorInfoManager.sensorData[typeIndex].get(0)).mTimeStamp;
                }
                saveSensorData(writer, sensorInfoManager.sensorData[typeIndex], this.mIndexBase[typeIndex], this.mTimeOffset[typeIndex]);
                int[] iArr = this.mIndexBase;
                iArr[typeIndex] = iArr[typeIndex] + sensorInfoManager.sensorData[typeIndex].size();
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (IOException e22) {
            e22.printStackTrace();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e222) {
                    e222.printStackTrace();
                }
            }
            if (fWriter != null) {
                fWriter.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (fWriter != null) {
                try {
                    fWriter.close();
                } catch (IOException e32) {
                    e32.printStackTrace();
                }
            }
        }
        Camera2App.scanFile(context, file);
    }

    private void saveSensorData(BufferedWriter writer, ArrayList<SensorData> sensorDataList, int indexBase, long baseTime) throws IOException {
        if (!sensorDataList.isEmpty()) {
            for (int i = 0; i < sensorDataList.size(); i++) {
                SensorData sensorData = (SensorData) sensorDataList.get(i);
                String writeStr = new StringBuilder();
                writeStr.append(toStringSensorData(sensorData, indexBase + i, baseTime));
                writeStr.append(CRLF);
                writer.write(writeStr.toString());
            }
        }
    }

    private static String toStringSensorData(SensorData sensorData, int index, long baseTime) {
        StringBuilder builder = new StringBuilder();
        Object[] objArr = new Object[1];
        int i = 0;
        objArr[0] = Integer.valueOf(index);
        builder.append(String.format("%1$05d", objArr));
        builder.append(SEPARATOR);
        builder.append(String.format("%d", new Object[]{Long.valueOf(sensorData.mTimeStamp)}));
        double[] dArr = sensorData.mValues;
        int length = dArr.length;
        while (i < length) {
            double value = dArr[i];
            builder.append(SEPARATOR);
            builder.append(value);
            i++;
        }
        return builder.toString();
    }

    private void saveSensorDataSummary(Context context, String filePath) {
        File file = null;
        FileWriter fWriter = null;
        BufferedWriter writer = null;
        try {
            file = new File(filePath);
            fWriter = new FileWriter(file, true);
            writer = new BufferedWriter(fWriter);
            Iterator it = this.mSensorInfoManagerList.iterator();
            while (it.hasNext()) {
                SensorInfoManager sensorInfoManager = (SensorInfoManager) it.next();
                saveSensorDataSummary(writer, sensorInfoManager, this.mIndexBase);
                int[] iArr = this.mIndexBase;
                iArr[0] = iArr[0] + sensorInfoManager.sensorData[0].size();
                iArr = this.mIndexBase;
                iArr[3] = iArr[3] + sensorInfoManager.sensorData[3].size();
                iArr = this.mIndexBase;
                iArr[1] = iArr[1] + sensorInfoManager.sensorData[1].size();
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (IOException e22) {
            e22.printStackTrace();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e222) {
                    e222.printStackTrace();
                }
            }
            if (fWriter != null) {
                fWriter.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (fWriter != null) {
                try {
                    fWriter.close();
                } catch (IOException e32) {
                    e32.printStackTrace();
                }
            }
        }
        Camera2App.scanFile(context, file);
    }

    private void saveSensorDataSummary(BufferedWriter writer, SensorInfoManager sensorInfoManager, int[] indexBase) throws IOException {
        updateTimestamp(sensorInfoManager);
        String gyroStr = toStringSensorDataSummary3(sensorInfoManager.sensorData[0], sensorInfoManager.g_ix, indexBase[0], this.mTimeOffset[0]);
        String rvStr = toStringSensorDataSummary4(sensorInfoManager.sensorData[3], sensorInfoManager.r_ix, indexBase[3], this.mTimeOffset[3]);
        String acStr = toStringSensorDataSummary4(sensorInfoManager.sensorData[1], sensorInfoManager.a_ix, indexBase[1], this.mTimeOffset[1]);
        String writeStr = new StringBuilder();
        writeStr.append(gyroStr);
        writeStr.append(SEPARATOR);
        writeStr.append(acStr);
        writeStr.append(SEPARATOR);
        writeStr.append(rvStr);
        writeStr.append(SEPARATOR);
        writeStr.append(sensorInfoManager.imageTimeStamp);
        writeStr.append(SEPARATOR);
        writeStr.append(sensorInfoManager.sensorTimeStamp);
        writeStr.append(SEPARATOR);
        writeStr.append(sensorInfoManager.exposureTime);
        writeStr.append(SEPARATOR);
        writeStr.append(sensorInfoManager.sensitivity);
        writeStr.append(SEPARATOR);
        writeStr.append(sensorInfoManager.rollingShutterSkew);
        writeStr.append(CRLF);
        writer.write(writeStr.toString());
    }

    private static String toStringSensorDataSummary3(ArrayList<SensorData> sensorDataList, int index, int indexBase, long baseTime) {
        if (sensorDataList.isEmpty()) {
            return "Unsupported\t-\t-\t-\t-";
        }
        if (index >= 0) {
            return toStringSensorData((SensorData) sensorDataList.get(index), indexBase + index, baseTime);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(index);
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        return stringBuilder.toString();
    }

    private static String toStringSensorDataSummary4(ArrayList<SensorData> sensorDataList, int index, int indexBase, long baseTime) {
        if (sensorDataList.isEmpty()) {
            return "Unsupported\t-\t-\t-\t-\t-";
        }
        if (index >= 0) {
            return toStringSensorData((SensorData) sensorDataList.get(index), indexBase + index, baseTime);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(index);
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        stringBuilder.append(SEPARATOR);
        stringBuilder.append("-");
        return stringBuilder.toString();
    }
}
