package com.bumptech.glide.load.resource.bitmap;

import android.support.v4.internal.view.SupportMenu;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import com.bumptech.glide.load.Key;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageHeaderParser {
    private static final int[] BYTES_PER_FORMAT = new int[]{0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
    private static final int EXIF_MAGIC_NUMBER = 65496;
    private static final int EXIF_SEGMENT_TYPE = 225;
    private static final int GIF_HEADER = 4671814;
    private static final int INTEL_TIFF_MAGIC_NUMBER = 18761;
    private static final String JPEG_EXIF_SEGMENT_PREAMBLE = "Exif\u0000\u0000";
    private static final byte[] JPEG_EXIF_SEGMENT_PREAMBLE_BYTES;
    private static final int MARKER_EOI = 217;
    private static final int MOTOROLA_TIFF_MAGIC_NUMBER = 19789;
    private static final int ORIENTATION_TAG_TYPE = 274;
    private static final int PNG_HEADER = -1991225785;
    private static final int SEGMENT_SOS = 218;
    private static final int SEGMENT_START_ID = 255;
    private static final String TAG = "ImageHeaderParser";
    private final StreamReader streamReader;

    public enum ImageType {
        GIF(true),
        JPEG(false),
        PNG_A(true),
        PNG(false),
        UNKNOWN(false);
        
        private final boolean hasAlpha;

        private ImageType(boolean hasAlpha) {
            this.hasAlpha = hasAlpha;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }
    }

    private static class RandomAccessReader {
        private final ByteBuffer data;

        public RandomAccessReader(byte[] data) {
            this.data = ByteBuffer.wrap(data);
            this.data.order(ByteOrder.BIG_ENDIAN);
        }

        public void order(ByteOrder byteOrder) {
            this.data.order(byteOrder);
        }

        public int length() {
            return this.data.array().length;
        }

        public int getInt32(int offset) {
            return this.data.getInt(offset);
        }

        public short getInt16(int offset) {
            return this.data.getShort(offset);
        }
    }

    private static class StreamReader {
        private final InputStream is;

        public StreamReader(InputStream is) {
            this.is = is;
        }

        public int getUInt16() throws IOException {
            return ((this.is.read() << 8) & MotionEventCompat.ACTION_POINTER_INDEX_MASK) | (this.is.read() & 255);
        }

        public short getUInt8() throws IOException {
            return (short) (this.is.read() & 255);
        }

        public long skip(long total) throws IOException {
            return this.is.skip(total);
        }

        public int read(byte[] buffer) throws IOException {
            return this.is.read(buffer);
        }

        public int getByte() throws IOException {
            return this.is.read();
        }
    }

    static {
        byte[] bytes = new byte[null];
        try {
            bytes = JPEG_EXIF_SEGMENT_PREAMBLE.getBytes(Key.STRING_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
        }
        JPEG_EXIF_SEGMENT_PREAMBLE_BYTES = bytes;
    }

    public ImageHeaderParser(InputStream is) {
        this.streamReader = new StreamReader(is);
    }

    public boolean hasAlpha() throws IOException {
        return getType().hasAlpha();
    }

    public ImageType getType() throws IOException {
        int firstByte = this.streamReader.getUInt8();
        if (firstByte == 255) {
            return ImageType.JPEG;
        }
        int firstFourBytes = ((((255 & this.streamReader.getUInt8()) | ((firstByte << 8) & MotionEventCompat.ACTION_POINTER_INDEX_MASK)) << 16) & SupportMenu.CATEGORY_MASK) | (this.streamReader.getUInt16() & SupportMenu.USER_MASK);
        if (firstFourBytes == PNG_HEADER) {
            this.streamReader.skip(21);
            return this.streamReader.getByte() >= 3 ? ImageType.PNG_A : ImageType.PNG;
        } else if ((firstFourBytes >> 8) == GIF_HEADER) {
            return ImageType.GIF;
        } else {
            return ImageType.UNKNOWN;
        }
    }

    public int getOrientation() throws IOException {
        if (!handles(this.streamReader.getUInt16())) {
            return -1;
        }
        byte[] exifData = getExifSegment();
        int i = 0;
        boolean hasJpegExifPreamble = exifData != null && exifData.length >= JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length;
        if (hasJpegExifPreamble) {
            while (i < JPEG_EXIF_SEGMENT_PREAMBLE_BYTES.length) {
                if (exifData[i] != JPEG_EXIF_SEGMENT_PREAMBLE_BYTES[i]) {
                    hasJpegExifPreamble = false;
                    break;
                }
                i++;
            }
        }
        if (hasJpegExifPreamble) {
            return parseExifSegment(new RandomAccessReader(exifData));
        }
        return -1;
    }

    private byte[] getExifSegment() throws IOException {
        short segmentType;
        String str;
        int segmentLength;
        do {
            short segmentId = this.streamReader.getUInt8();
            if (segmentId != (short) 255) {
                if (Log.isLoggable(TAG, 3)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown segmentId=");
                    stringBuilder.append(segmentId);
                    Log.d(str2, stringBuilder.toString());
                }
                return null;
            }
            segmentType = this.streamReader.getUInt8();
            if (segmentType == (short) 218) {
                return null;
            }
            if (segmentType == (short) 217) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Found MARKER_EOI in exif segment");
                }
                return null;
            }
            segmentLength = this.streamReader.getUInt16() - 2;
            if (segmentType == (short) 225) {
                byte[] segmentData = new byte[segmentLength];
                if (segmentLength == this.streamReader.read(segmentData)) {
                    return segmentData;
                }
                if (Log.isLoggable(TAG, 3)) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to read segment data for type=");
                    stringBuilder2.append(segmentType);
                    stringBuilder2.append(" length=");
                    stringBuilder2.append(segmentLength);
                    Log.d(str, stringBuilder2.toString());
                }
                return null;
            }
        } while (((long) segmentLength) == this.streamReader.skip((long) segmentLength));
        if (Log.isLoggable(TAG, 3)) {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Unable to skip enough data for type=");
            stringBuilder3.append(segmentType);
            Log.d(str, stringBuilder3.toString());
        }
        return null;
    }

    private static int parseExifSegment(RandomAccessReader segmentData) {
        ByteOrder byteOrder;
        RandomAccessReader randomAccessReader = segmentData;
        int headerOffsetSize = JPEG_EXIF_SEGMENT_PREAMBLE.length();
        short byteOrderIdentifier = randomAccessReader.getInt16(headerOffsetSize);
        int i = 3;
        if (byteOrderIdentifier == (short) 19789) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else if (byteOrderIdentifier == (short) 18761) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else {
            if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown endianness = ");
                stringBuilder.append(byteOrderIdentifier);
                Log.d(str, stringBuilder.toString());
            }
            byteOrder = ByteOrder.BIG_ENDIAN;
        }
        randomAccessReader.order(byteOrder);
        int firstIfdOffset = randomAccessReader.getInt32(headerOffsetSize + 4) + headerOffsetSize;
        int tagCount = randomAccessReader.getInt16(firstIfdOffset);
        int i2 = 0;
        while (i2 < tagCount) {
            int i3;
            int tagOffset = calcTagOffset(firstIfdOffset, i2);
            int tagType = randomAccessReader.getInt16(tagOffset);
            if (tagType == ORIENTATION_TAG_TYPE) {
                int formatCode = randomAccessReader.getInt16(tagOffset + 2);
                String str2;
                if (formatCode < 1 || formatCode > 12) {
                    i3 = 3;
                    if (Log.isLoggable(TAG, 3)) {
                        str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Got invalid format code = ");
                        stringBuilder2.append(formatCode);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    i2++;
                    i = i3;
                } else {
                    i3 = randomAccessReader.getInt32(tagOffset + 4);
                    if (i3 >= 0) {
                        if (Log.isLoggable(TAG, i)) {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Got tagIndex=");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(" tagType=");
                            stringBuilder3.append(tagType);
                            stringBuilder3.append(" formatCode =");
                            stringBuilder3.append(formatCode);
                            stringBuilder3.append(" componentCount=");
                            stringBuilder3.append(i3);
                            Log.d(str3, stringBuilder3.toString());
                        }
                        int byteCount = BYTES_PER_FORMAT[formatCode] + i3;
                        StringBuilder stringBuilder4;
                        if (byteCount <= 4) {
                            int tagValueOffset = tagOffset + 8;
                            if (tagValueOffset < 0 || tagValueOffset > segmentData.length()) {
                                if (Log.isLoggable(TAG, 3)) {
                                    str2 = TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Illegal tagValueOffset=");
                                    stringBuilder4.append(tagValueOffset);
                                    stringBuilder4.append(" tagType=");
                                    stringBuilder4.append(tagType);
                                    Log.d(str2, stringBuilder4.toString());
                                }
                            } else if (byteCount >= 0 && tagValueOffset + byteCount <= segmentData.length()) {
                                return randomAccessReader.getInt16(tagValueOffset);
                            } else {
                                if (Log.isLoggable(TAG, i)) {
                                    String str4 = TAG;
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("Illegal number of bytes for TI tag data tagType=");
                                    stringBuilder5.append(tagType);
                                    Log.d(str4, stringBuilder5.toString());
                                }
                            }
                            i3 = 3;
                            i2++;
                            i = i3;
                        } else if (Log.isLoggable(TAG, i)) {
                            String str5 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Got byte count > 4, not orientation, continuing, formatCode=");
                            stringBuilder4.append(formatCode);
                            Log.d(str5, stringBuilder4.toString());
                        }
                    } else if (Log.isLoggable(TAG, i)) {
                        Log.d(TAG, "Negative tiff component count");
                    }
                }
            }
            i3 = i;
            i2++;
            i = i3;
        }
        return -1;
    }

    private static int calcTagOffset(int ifdOffset, int tagIndex) {
        return (ifdOffset + 2) + (12 * tagIndex);
    }

    private static boolean handles(int imageMagicNumber) {
        return (imageMagicNumber & EXIF_MAGIC_NUMBER) == EXIF_MAGIC_NUMBER || imageMagicNumber == MOTOROLA_TIFF_MAGIC_NUMBER || imageMagicNumber == INTEL_TIFF_MAGIC_NUMBER;
    }
}
