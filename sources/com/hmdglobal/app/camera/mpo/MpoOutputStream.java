package com.hmdglobal.app.camera.mpo;

import android.util.Log;
import com.hmdglobal.app.camera.exif.JpegHeader;
import com.hmdglobal.app.camera.exif.OrderedDataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

class MpoOutputStream extends FilterOutputStream {
    private static final String DC_CROP_INFO = "Qualcomm Dual Camera Attributes";
    private static final int DC_CROP_INFO_BYTE_SIZE = DC_CROP_INFO.length();
    private static final boolean DEBUG = true;
    private static final int MAX_EXIF_SIZE = 65535;
    private static final int STATE_FRAME_HEADER = 1;
    private static final int STATE_JPEG_DATA = 3;
    private static final int STATE_SKIP_CROP = 2;
    private static final int STATE_SOI = 0;
    private static final int STREAMBUFFER_SIZE = 65536;
    private static final String TAG = "MpoOutputStream";
    private static final short TIFF_BIG_ENDIAN = (short) 19789;
    private static final short TIFF_HEADER = (short) 42;
    private static final short TIFF_LITTLE_ENDIAN = (short) 18761;
    private ByteBuffer mBuffer = ByteBuffer.allocate(4);
    private int mByteToCopy;
    private int mByteToSkip;
    private ByteBuffer mCropInfo = ByteBuffer.allocate(DC_CROP_INFO_BYTE_SIZE);
    private MpoImageData mCurrentImageData;
    private MpoData mMpoData;
    private int mMpoOffsetStart = -1;
    private byte[] mSingleByteArray = new byte[1];
    private int mSize = 0;
    private boolean mSkipCropData = false;
    private int mState = 0;

    protected MpoOutputStream(OutputStream ou) {
        super(new BufferedOutputStream(ou, 65536));
    }

    /* Access modifiers changed, original: protected */
    public void setMpoData(MpoData mpoData) {
        this.mMpoData = mpoData;
        this.mMpoData.updateAllTags();
    }

    private void resetStates() {
        this.mState = 0;
        this.mByteToSkip = 0;
        this.mByteToCopy = 0;
        this.mBuffer.rewind();
    }

    private int requestByteToBuffer(ByteBuffer buffer, int requestByteCount, byte[] data, int offset, int length) {
        int byteNeeded = requestByteCount - buffer.position();
        int byteToRead = length > byteNeeded ? byteNeeded : length;
        buffer.put(data, offset, byteToRead);
        return byteToRead;
    }

    private boolean isDualCamCropInfo() {
        if (this.mCropInfo.position() != DC_CROP_INFO_BYTE_SIZE) {
            return false;
        }
        this.mCropInfo.rewind();
        for (int i = 0; i < DC_CROP_INFO.length(); i++) {
            if (DC_CROP_INFO.charAt(i) != ((char) this.mCropInfo.get(i))) {
                return false;
            }
        }
        return true;
    }

    /* Access modifiers changed, original: 0000 */
    public void writeMpoFile() throws IOException {
        this.mCurrentImageData = this.mMpoData.getPrimaryMpoImage();
        if (this.mMpoData.getAuxiliaryImageCount() > 1) {
            this.mSkipCropData = true;
        }
        write(this.mCurrentImageData.getJpegData());
        flush();
        this.mSkipCropData = false;
        for (MpoImageData image : this.mMpoData.getAuxiliaryMpoImages()) {
            resetStates();
            this.mCurrentImageData = image;
            write(this.mCurrentImageData.getJpegData());
            flush();
        }
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        byte[] bArr = buffer;
        int offset2 = offset;
        int length2 = length;
        while (true) {
            if ((this.mByteToSkip > 0 || this.mByteToCopy > 0 || this.mState != 3) && length2 > 0) {
                int byteToProcess;
                if (this.mByteToSkip > 0) {
                    byteToProcess = length2 > this.mByteToSkip ? this.mByteToSkip : length2;
                    length2 -= byteToProcess;
                    this.mByteToSkip -= byteToProcess;
                    offset2 += byteToProcess;
                }
                if (this.mByteToCopy > 0) {
                    byteToProcess = length2 > this.mByteToCopy ? this.mByteToCopy : length2;
                    this.out.write(bArr, offset2, byteToProcess);
                    this.mSize += byteToProcess;
                    length2 -= byteToProcess;
                    this.mByteToCopy -= byteToProcess;
                    offset2 += byteToProcess;
                }
                int length3 = length2;
                int offset3 = offset2;
                if (length3 != 0) {
                    int i;
                    switch (this.mState) {
                        case 0:
                            i = 2;
                            length2 = requestByteToBuffer(this.mBuffer, 2, bArr, offset3, length3);
                            offset3 += length2;
                            length3 -= length2;
                            if (this.mBuffer.position() >= i) {
                                this.mBuffer.rewind();
                                if (this.mBuffer.getShort() == (short) -40) {
                                    this.out.write(this.mBuffer.array(), 0, i);
                                    this.mSize += i;
                                    this.mState = 1;
                                    this.mBuffer.rewind();
                                    break;
                                }
                                throw new IOException("Not a valid jpeg image, cannot write exif");
                            }
                            return;
                        case 1:
                            i = 2;
                            length2 = requestByteToBuffer(this.mBuffer, 4, bArr, offset3, length3);
                            if (this.mBuffer.position() == i && this.mBuffer.getShort() == (short) -39) {
                                this.out.write(this.mBuffer.array(), 0, i);
                                this.mSize += i;
                                this.mBuffer.rewind();
                            }
                            if (this.mBuffer.position() >= 4) {
                                this.mBuffer.rewind();
                                short marker = this.mBuffer.getShort();
                                if (marker == (short) -31 || marker == (short) -32) {
                                    this.out.write(this.mBuffer.array(), 0, 4);
                                    this.mSize += 4;
                                    this.mByteToCopy = (this.mBuffer.getShort() & 65535) - i;
                                    offset3 += length2;
                                    length3 -= length2;
                                } else {
                                    writeMpoData();
                                    if (this.mSkipCropData) {
                                        this.mState = i;
                                    } else {
                                        this.mState = 3;
                                    }
                                }
                                this.mBuffer.rewind();
                                break;
                            }
                            return;
                            break;
                        case 2:
                            i = 2;
                            int byteRead = requestByteToBuffer(this.mBuffer, 4, bArr, offset3, length3);
                            if (this.mBuffer.position() == i && this.mBuffer.getShort() == (short) -39) {
                                this.out.write(this.mBuffer.array(), 0, i);
                                this.mSize += i;
                                this.mBuffer.rewind();
                            }
                            if (this.mBuffer.position() >= 4) {
                                offset3 += byteRead;
                                length3 -= byteRead;
                                this.mBuffer.rewind();
                                if (JpegHeader.isSofMarker(this.mBuffer.getShort())) {
                                    this.out.write(this.mBuffer.array(), 0, 4);
                                    this.mSize += 4;
                                    this.mState = 3;
                                } else {
                                    byteRead = requestByteToBuffer(this.mCropInfo, DC_CROP_INFO_BYTE_SIZE, bArr, offset3, length3);
                                    if (isDualCamCropInfo()) {
                                        this.out.write(this.mBuffer.array(), 0, 4);
                                        this.mSize += 4;
                                        length2 = (this.mBuffer.getShort() & 65535) - i;
                                        this.mByteToSkip = length2;
                                        while (length2 > 0) {
                                            this.out.write(0);
                                            this.mSize++;
                                            length2--;
                                        }
                                        this.mState = 3;
                                    } else {
                                        this.out.write(this.mBuffer.array(), 0, 4);
                                        this.mSize += 4;
                                        this.mByteToCopy = (this.mBuffer.getShort() & 65535) - i;
                                    }
                                    this.mCropInfo.rewind();
                                }
                                this.mBuffer.rewind();
                                break;
                            }
                            return;
                    }
                    length2 = length3;
                    offset2 = offset3;
                } else {
                    return;
                }
            }
        }
        if (length2 > 0) {
            this.out.write(bArr, offset2, length2);
            this.mSize += length2;
        }
    }

    public void write(int oneByte) throws IOException {
        this.mSingleByteArray[0] = (byte) (255 & oneByte);
        write(this.mSingleByteArray);
    }

    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    private void writeMpoData() throws IOException {
        if (this.mMpoData != null) {
            Log.v(TAG, "Writing mpo data...");
            int exifSize = this.mCurrentImageData.calculateAllIfdOffsets() + 6;
            if (exifSize <= 65535) {
                OrderedDataOutputStream dataOutputStream = new OrderedDataOutputStream(this.out);
                dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
                dataOutputStream.writeShort((short) -30);
                dataOutputStream.writeShort((short) exifSize);
                dataOutputStream.writeInt(1297106432);
                if (this.mMpoOffsetStart == -1) {
                    this.mMpoOffsetStart = this.mSize + dataOutputStream.size();
                }
                if (this.mCurrentImageData.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                    dataOutputStream.writeShort(TIFF_BIG_ENDIAN);
                } else {
                    dataOutputStream.writeShort(TIFF_LITTLE_ENDIAN);
                }
                dataOutputStream.setByteOrder(this.mCurrentImageData.getByteOrder());
                dataOutputStream.writeShort(TIFF_HEADER);
                if (exifSize > 14) {
                    dataOutputStream.writeInt(8);
                    writeAllTags(dataOutputStream);
                } else {
                    dataOutputStream.writeInt(0);
                }
                this.mSize += dataOutputStream.size();
                return;
            }
            throw new IOException("Exif header is too large (>64Kb)");
        }
    }

    private void updateIndexIfdOffsets(MpoIfdData indexIfd, int mpoOffset) {
        int i = 1;
        MpoTag mpEntryTag = this.mMpoData.getPrimaryMpoImage().getTag((short) MpoInterface.TAG_MP_ENTRY, 1);
        List<MpEntry> mpEntries = mpEntryTag.getMpEntryValue();
        while (i < mpEntries.size()) {
            MpEntry entry = (MpEntry) mpEntries.get(i);
            entry.setImageOffset(entry.getImageOffset() - mpoOffset);
            i++;
        }
        mpEntryTag.setValue(mpEntries);
    }

    private void writeAllTags(OrderedDataOutputStream dataOutputStream) throws IOException {
        MpoIfdData indexIfd = this.mCurrentImageData.getIndexIfdData();
        if (indexIfd.getTagCount() > 0) {
            updateIndexIfdOffsets(indexIfd, this.mMpoOffsetStart);
            writeIfd(indexIfd, dataOutputStream);
        }
        MpoIfdData attribIfd = this.mCurrentImageData.getAttribIfdData();
        if (attribIfd.getTagCount() > 0) {
            writeIfd(attribIfd, dataOutputStream);
        }
    }

    private void writeIfd(MpoIfdData ifd, OrderedDataOutputStream dataOutputStream) throws IOException {
        MpoTag[] tags = ifd.getAllTags();
        dataOutputStream.writeShort((short) tags.length);
        int i = 0;
        for (MpoTag tag : tags) {
            dataOutputStream.writeShort(tag.getTagId());
            dataOutputStream.writeShort(tag.getDataType());
            dataOutputStream.writeInt(tag.getComponentCount());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");
            stringBuilder.append(tag.toString());
            Log.v(str, stringBuilder.toString());
            if (tag.getDataSize() > 4) {
                dataOutputStream.writeInt(tag.getOffset());
            } else {
                writeTagValue(tag, dataOutputStream);
                int n = 4 - tag.getDataSize();
                for (int i2 = 0; i2 < n; i2++) {
                    dataOutputStream.write(0);
                }
            }
        }
        dataOutputStream.writeInt(ifd.getOffsetToNextIfd());
        int length = tags.length;
        while (i < length) {
            MpoTag tag2 = tags[i];
            if (tag2.getDataSize() > 4) {
                writeTagValue(tag2, dataOutputStream);
            }
            i++;
        }
    }

    static void writeTagValue(MpoTag tag, OrderedDataOutputStream dataOutputStream) throws IOException {
        byte[] buf;
        int n;
        int i;
        switch (tag.getDataType()) {
            case (short) 1:
            case (short) 7:
                buf = new byte[tag.getComponentCount()];
                tag.getBytes(buf);
                dataOutputStream.write(buf);
                return;
            case (short) 2:
                buf = tag.getStringByte();
                if (buf.length == tag.getComponentCount()) {
                    buf[buf.length - 1] = (byte) 0;
                    dataOutputStream.write(buf);
                    return;
                }
                dataOutputStream.write(buf);
                dataOutputStream.write(0);
                return;
            case (short) 3:
                n = tag.getComponentCount();
                for (i = 0; i < n; i++) {
                    dataOutputStream.writeShort((short) ((int) tag.getValueAt(i)));
                }
                return;
            case (short) 4:
            case (short) 9:
                n = tag.getComponentCount();
                for (i = 0; i < n; i++) {
                    dataOutputStream.writeInt((int) tag.getValueAt(i));
                }
                return;
            case (short) 5:
            case (short) 10:
                n = tag.getComponentCount();
                for (i = 0; i < n; i++) {
                    dataOutputStream.writeRational(tag.getRational(i));
                }
                return;
            default:
                return;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public int size() {
        return this.mSize;
    }
}
