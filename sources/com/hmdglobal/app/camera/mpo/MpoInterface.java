package com.hmdglobal.app.camera.mpo;

import android.util.Log;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MpoInterface {
    private static final String NULL_ARGUMENT_STRING = "Argument is null";
    private static final String TAG = "MpoInterface";
    public static final int TAG_AXIS_DISTANCE_X = ExifInterface.defineTag(2, (short) -19960);
    public static final int TAG_AXIS_DISTANCE_Y = ExifInterface.defineTag(2, (short) -19959);
    public static final int TAG_AXIS_DISTANCE_Z = ExifInterface.defineTag(2, (short) -19958);
    public static final int TAG_BASELINE_LEN = ExifInterface.defineTag(2, (short) -19962);
    public static final int TAG_BASE_VIEWPOINT_NUM = ExifInterface.defineTag(2, (short) -19964);
    public static final int TAG_CONVERGE_ANGLE = ExifInterface.defineTag(2, (short) -19963);
    public static final int TAG_DIVERGE_ANGLE = ExifInterface.defineTag(2, (short) -19961);
    public static final int TAG_IMAGE_NUMBER = ExifInterface.defineTag(2, (short) -20223);
    public static final int TAG_IMAGE_UNIQUE_ID_LIST = ExifInterface.defineTag(1, (short) -20477);
    public static final int TAG_MP_ENTRY = ExifInterface.defineTag(1, (short) -20478);
    public static final int TAG_MP_FORMAT_VERSION = ExifInterface.defineTag(3, (short) -20480);
    public static final int TAG_NUM_CAPTURED_FRAMES = ExifInterface.defineTag(1, (short) -20476);
    public static final int TAG_NUM_IMAGES = ExifInterface.defineTag(1, (short) -20479);
    public static final int TAG_PAN_ORIENTATION = ExifInterface.defineTag(2, (short) -19967);
    public static final int TAG_PAN_OVERLAP_H = ExifInterface.defineTag(2, (short) -19966);
    public static final int TAG_PAN_OVERLAP_V = ExifInterface.defineTag(2, (short) -19965);
    public static final int TAG_PITCH_ANGLE = ExifInterface.defineTag(2, (short) -19956);
    public static final int TAG_ROLL_ANGLE = ExifInterface.defineTag(2, (short) -19955);
    public static final int TAG_YAW_ANGLE = ExifInterface.defineTag(2, (short) -19957);

    public static int writeMpo(MpoData mpo, OutputStream out) {
        if (mpo == null || out == null) {
            throw new IllegalArgumentException(NULL_ARGUMENT_STRING);
        }
        MpoOutputStream s = getMpoWriterStream(out);
        s.setMpoData(mpo);
        try {
            s.writeMpoFile();
            CameraUtil.closeSilently(s);
            return s.size();
        } catch (IOException e) {
            CameraUtil.closeSilently(s);
            Log.w(TAG, "IO Exception when writing mpo image");
            return -1;
        }
    }

    public static int writeMpo(MpoData mpo, String outFilename) {
        if (mpo != null && outFilename != null) {
            return writeMpo(mpo, getFileWriterStream(outFilename));
        }
        throw new IllegalArgumentException(NULL_ARGUMENT_STRING);
    }

    private static MpoOutputStream getMpoWriterStream(OutputStream outStream) {
        if (outStream != null) {
            return new MpoOutputStream(outStream);
        }
        throw new IllegalArgumentException(NULL_ARGUMENT_STRING);
    }

    private static OutputStream getFileWriterStream(String outFileName) {
        if (outFileName != null) {
            try {
                return new FileOutputStream(outFileName);
            } catch (FileNotFoundException e) {
                CameraUtil.closeSilently(null);
                Log.w(TAG, "File not found");
                return null;
            }
        }
        throw new IllegalArgumentException(NULL_ARGUMENT_STRING);
    }

    private static short getShort(byte[] b, int index) {
        return (short) ((b[index] << 8) | (b[index + 1] & 255));
    }

    private static byte[] openNewStream(ByteArrayOutputStream stream) {
        byte[] bytes = stream.toByteArray();
        stream.reset();
        return bytes;
    }

    public static ArrayList<byte[]> generateXmpFromMpo(String mpoFilePath) {
        IOException e;
        File mpoFile = new File(mpoFilePath);
        ArrayList<byte[]> bytes = new ArrayList();
        byte[] readBuffer = new byte[1024];
        int eoiNumber = 0;
        int index = 0;
        boolean endByFF = false;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            FileInputStream sourceStream = new FileInputStream(mpoFile);
            while (true) {
                int read = sourceStream.read(readBuffer);
                int readedCount = read;
                if (read != -1) {
                    read = 2;
                    boolean z = true;
                    if (endByFF && readBuffer[0] == (byte) -39) {
                        eoiNumber++;
                        if (eoiNumber == 2) {
                            eoiNumber = 0;
                            outputStream.write(-39);
                            index = 1;
                            if (readedCount == 1) {
                                break;
                            }
                            bytes.add(openNewStream(outputStream));
                        }
                    }
                    int index2 = index;
                    index = eoiNumber;
                    eoiNumber = 0;
                    while (eoiNumber < readedCount - 1) {
                        try {
                            if (getShort(readBuffer, eoiNumber) == (short) -39) {
                                index++;
                                if (index == read) {
                                    int startIndex = index2;
                                    index2 = eoiNumber + 2;
                                    outputStream.write(readBuffer, startIndex, index2 - startIndex);
                                    bytes.add(openNewStream(outputStream));
                                    index = 0;
                                }
                            }
                            eoiNumber++;
                            read = 2;
                        } catch (IOException e2) {
                            e = e2;
                            eoiNumber = index;
                            index = index2;
                            e.printStackTrace();
                            return bytes;
                        }
                    }
                    if (readBuffer[readedCount - 1] != (byte) -1) {
                        z = false;
                    }
                    endByFF = z;
                    if (index2 < readedCount) {
                        outputStream.write(readBuffer, index2, readedCount - index2);
                    }
                    eoiNumber = index;
                    index = 0;
                }
            }
            sourceStream.close();
            outputStream.close();
        } catch (IOException e3) {
            e = e3;
        }
        return bytes;
    }

    public static ArrayList<byte[]> generateXmpFromMpo(byte[] mpoSourceBytes) {
        ArrayList<byte[]> bytes = new ArrayList();
        int eoiNumber = 0;
        int index = 0;
        int imageNumber = 0;
        boolean isFirstImage = true;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Log.e("Mpo", "generateXmpFromMpo ");
        int i = 0;
        while (i < mpoSourceBytes.length - 1) {
            try {
                if (getShort(mpoSourceBytes, i) == (short) -39) {
                    eoiNumber++;
                    if (eoiNumber == 1 || !isFirstImage) {
                        imageNumber++;
                        int startIndex = index;
                        index = i + 2;
                        outputStream.write(mpoSourceBytes, startIndex, index - startIndex);
                        bytes.add(openNewStream(outputStream));
                        eoiNumber = 0;
                        isFirstImage = false;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("generateXmpFromMpo 22mpoSourceBytes");
                        stringBuilder.append(mpoSourceBytes.length);
                        Log.e("Mpo", stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("generateXmpFromMpo ");
                        stringBuilder.append(imageNumber);
                        stringBuilder.append(" ");
                        stringBuilder.append(index - startIndex);
                        Log.e("Mpo", stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("generateXmpFromMpo ");
                        stringBuilder.append(imageNumber);
                        stringBuilder.append(" ");
                        stringBuilder.append(index);
                        Log.e("Mpo", stringBuilder.toString());
                        if (imageNumber == 2) {
                            outputStream.write(mpoSourceBytes, index, mpoSourceBytes.length - index);
                            bytes.add(openNewStream(outputStream));
                            break;
                        }
                    }
                }
                i++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        outputStream.close();
        return bytes;
    }
}
