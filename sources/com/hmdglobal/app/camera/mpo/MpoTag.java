package com.hmdglobal.app.camera.mpo;

import android.util.Log;
import com.hmdglobal.app.camera.exif.ExifTag;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MpoTag extends ExifTag {
    private static final String TAG = "MpoTag";
    static final int TAG_SIZE = 12;

    static class MpEntry {
        static final int SIZE = 16;
        private short mDependantImage1;
        private short mDependantImage2;
        private int mImageAttrib;
        private int mImageOffset;
        private int mImageSize;

        public MpEntry() {
            this(0, 0, 0, (short) 0, (short) 0);
        }

        public MpEntry(int imageAttrib, int imageSize, int imageOffset) {
            this(imageAttrib, imageSize, imageOffset, (short) 0, (short) 0);
        }

        public MpEntry(int imageAttrib, int imageSize, int imageOffset, short dependantImage1, short dependantImage2) {
            this.mImageAttrib = imageAttrib;
            this.mImageSize = imageSize;
            this.mImageOffset = imageOffset;
            this.mDependantImage1 = dependantImage1;
            this.mDependantImage2 = dependantImage2;
        }

        public MpEntry(ByteBuffer buffer) {
            this.mImageAttrib = buffer.getInt();
            this.mImageSize = buffer.getInt();
            this.mImageOffset = buffer.getInt();
            this.mDependantImage1 = buffer.getShort();
            this.mDependantImage2 = buffer.getShort();
        }

        public int getImageAttrib() {
            return this.mImageAttrib;
        }

        public int getImageSize() {
            return this.mImageSize;
        }

        public int getImageOffset() {
            return this.mImageOffset;
        }

        public short getDependantImage1() {
            return this.mDependantImage1;
        }

        public short getDependantImage2() {
            return this.mDependantImage2;
        }

        public void setImageAttrib(int imageAttrib) {
            this.mImageAttrib = imageAttrib;
        }

        public void setImageSize(int imageSize) {
            this.mImageSize = imageSize;
        }

        public void setImageOffset(int imageOffset) {
            this.mImageOffset = imageOffset;
        }

        public void setDependantImage1(short depImage1) {
            this.mDependantImage1 = depImage1;
        }

        public void setDependantImage2(short depImage2) {
            this.mDependantImage2 = depImage2;
        }

        public boolean getBytes(ByteBuffer buffer) {
            try {
                buffer.putInt(this.mImageAttrib);
                buffer.putInt(this.mImageSize);
                buffer.putInt(this.mImageOffset);
                buffer.putShort(this.mDependantImage1);
                buffer.putShort(this.mDependantImage2);
                return true;
            } catch (BufferOverflowException e) {
                Log.w(MpoTag.TAG, "Buffer size too small");
                return false;
            }
        }
    }

    MpoTag(short tagId, short type, int componentCount, int ifd, boolean hasDefinedComponentCount) {
        super(tagId, type, componentCount, ifd, hasDefinedComponentCount);
    }

    public boolean setValue(List<MpEntry> entries) {
        int i = 0;
        if (getTagId() != ((short) MpoInterface.TAG_MP_ENTRY)) {
            return false;
        }
        byte[] bytes = new byte[(entries.size() * 16)];
        while (i < entries.size()) {
            ((MpEntry) entries.get(i)).getBytes(ByteBuffer.wrap(bytes, i * 16, 16));
            i++;
        }
        return setValue(bytes);
    }

    public List<MpEntry> getMpEntryValue() {
        if (getTagId() != ((short) MpoInterface.TAG_MP_ENTRY)) {
            return null;
        }
        byte[] bytes = getValueAsBytes();
        List<MpEntry> entries = new ArrayList(bytes.length / 16);
        for (int i = 0; i < bytes.length; i += 16) {
            entries.add(new MpEntry(ByteBuffer.wrap(bytes, i, 16)));
        }
        return entries;
    }
}
