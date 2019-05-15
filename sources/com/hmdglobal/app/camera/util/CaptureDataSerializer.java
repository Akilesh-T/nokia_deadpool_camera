package com.hmdglobal.app.camera.util;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.util.Pair;
import android.util.Rational;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class CaptureDataSerializer {
    private static final Tag TAG = new Tag("CaptureDataSerilzr");

    private interface Writeable {
        void write(Writer writer) throws IOException;
    }

    public static String toString(String title, CaptureRequest metadata) {
        Writer writer = new StringWriter();
        dumpMetadata(title, metadata, writer);
        return writer.toString();
    }

    public static void toFile(String title, CameraMetadata<?> metadata, File file) {
        try {
            Writer writer = new FileWriter(file, true);
            if (metadata instanceof CaptureRequest) {
                dumpMetadata(title, (CaptureRequest) metadata, writer);
            } else if (metadata instanceof CaptureResult) {
                dumpMetadata(title, (CaptureResult) metadata, writer);
            } else {
                writer.close();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot generate debug data from type ");
                stringBuilder.append(metadata.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            writer.close();
        } catch (IOException ex) {
            Log.e(TAG, "Could not write capture data to file.", ex);
        }
    }

    private static void dumpMetadata(final String title, final CaptureRequest metadata, Writer writer) {
        dumpMetadata(new Writeable() {
            public void write(Writer writer) throws IOException {
                List<Key<?>> keys = metadata.getKeys();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(title);
                stringBuilder.append(10);
                writer.write(stringBuilder.toString());
                for (Key<?> key : keys) {
                    writer.write(String.format("    %s\n", new Object[]{key.getName()}));
                    writer.write(String.format("        %s\n", new Object[]{CaptureDataSerializer.metadataValueToString(metadata.get(key))}));
                }
            }
        }, new BufferedWriter(writer));
    }

    private static void dumpMetadata(final String title, final CaptureResult metadata, Writer writer) {
        dumpMetadata(new Writeable() {
            public void write(Writer writer) throws IOException {
                List<CaptureResult.Key<?>> keys = metadata.getKeys();
                writer.write(String.format(title, new Object[0]));
                for (CaptureResult.Key<?> key : keys) {
                    writer.write(String.format("    %s\n", new Object[]{key.getName()}));
                    writer.write(String.format("        %s\n", new Object[]{CaptureDataSerializer.metadataValueToString(metadata.get(key))}));
                }
            }
        }, new BufferedWriter(writer));
    }

    private static String metadataValueToString(Object object) {
        if (object == null) {
            return "<null>";
        }
        if (object.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                builder.append(metadataValueToString(Array.get(object, i)));
                if (i != length - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
            return builder.toString();
        } else if (object instanceof RggbChannelVector) {
            return toString((RggbChannelVector) object);
        } else {
            if (object instanceof ColorSpaceTransform) {
                return toString((ColorSpaceTransform) object);
            }
            if (object instanceof TonemapCurve) {
                return toString((TonemapCurve) object);
            }
            if (object instanceof Pair) {
                return toString((Pair) object);
            }
            return object.toString();
        }
    }

    private static void dumpMetadata(Writeable metadata, Writer writer) {
        try {
            metadata.write(writer);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, "dumpMetadata - Failed to close writer.", e);
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, "dumpMetadata - Failed to dump metadata", e2);
            if (writer != null) {
                writer.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    Log.e(TAG, "dumpMetadata - Failed to close writer.", e3);
                }
            }
        }
    }

    private static String toString(RggbChannelVector vector) {
        StringBuilder str = new StringBuilder();
        str.append("RggbChannelVector:");
        str.append(" R:");
        str.append(vector.getRed());
        str.append(" G(even):");
        str.append(vector.getGreenEven());
        str.append(" G(odd):");
        str.append(vector.getGreenOdd());
        str.append(" B:");
        str.append(vector.getBlue());
        return str.toString();
    }

    private static String toString(ColorSpaceTransform transform) {
        StringBuilder str = new StringBuilder();
        Rational[] rationals = new Rational[9];
        transform.copyElements(rationals, 0);
        str.append("ColorSpaceTransform: ");
        str.append(Arrays.toString(rationals));
        return str.toString();
    }

    private static String toString(TonemapCurve curve) {
        StringBuilder str = new StringBuilder();
        str.append("TonemapCurve:");
        float[] reds = new float[(curve.getPointCount(0) * 2)];
        curve.copyColorCurve(0, reds, 0);
        float[] greens = new float[(curve.getPointCount(1) * 2)];
        curve.copyColorCurve(1, greens, 0);
        float[] blues = new float[(curve.getPointCount(2) * 2)];
        curve.copyColorCurve(2, blues, 0);
        str.append("\n\nReds: ");
        str.append(Arrays.toString(reds));
        str.append("\n\nGreens: ");
        str.append(Arrays.toString(greens));
        str.append("\n\nBlues: ");
        str.append(Arrays.toString(blues));
        return str.toString();
    }

    private static String toString(Pair<?, ?> pair) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pair: ");
        stringBuilder.append(metadataValueToString(pair.first));
        stringBuilder.append(" / ");
        stringBuilder.append(metadataValueToString(pair.second));
        return stringBuilder.toString();
    }
}
