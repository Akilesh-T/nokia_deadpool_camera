package com.hmdglobal.app.camera.mpo;

import com.hmdglobal.app.camera.exif.ExifTag;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MpoImageData {
    static final int APP_HEADER_SIZE = 6;
    static final int MP_FORMAT_IDENTIFIER = 1297106432;
    static final int MP_HEADER_SIZE = 8;
    static final int OFFSET_TO_FIRST_IFD = 8;
    private static final String TAG = "MpoImageData";
    private final ByteOrder mByteOrder;
    private final byte[] mJpegData;
    private final MpoIfdData mMpAttribIfdData = new MpoIfdData(2);
    private final MpoIfdData mMpIndexIfdData = new MpoIfdData(1);

    public MpoImageData(byte[] jpegData, ByteOrder byteOrder) {
        this.mJpegData = jpegData;
        this.mByteOrder = byteOrder;
    }

    /* Access modifiers changed, original: protected */
    public byte[] getJpegData() {
        return this.mJpegData;
    }

    /* Access modifiers changed, original: protected */
    public ByteOrder getByteOrder() {
        return this.mByteOrder;
    }

    /* Access modifiers changed, original: protected */
    public MpoIfdData getAttribIfdData() {
        return this.mMpAttribIfdData;
    }

    /* Access modifiers changed, original: protected */
    public MpoIfdData getIndexIfdData() {
        return this.mMpIndexIfdData;
    }

    /* Access modifiers changed, original: protected */
    public MpoIfdData getMpIfdData(int ifdId) {
        return ifdId == 1 ? this.mMpIndexIfdData : this.mMpAttribIfdData;
    }

    /* Access modifiers changed, original: protected */
    public MpoTag getTag(short tag, int ifd) {
        return getMpIfdData(ifd).getTag(tag);
    }

    /* Access modifiers changed, original: protected */
    public MpoTag addTag(MpoTag tag) {
        if (tag != null) {
            return addTag(tag, tag.getIfd());
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public MpoTag addTag(MpoTag tag, int ifdId) {
        if (tag == null || !ExifTag.isValidIfd(ifdId)) {
            return null;
        }
        return getMpIfdData(ifdId).setTag(tag);
    }

    /* Access modifiers changed, original: protected */
    public void removeTag(short tagId, int ifdId) {
        getMpIfdData(ifdId).removeTag(tagId);
    }

    /* Access modifiers changed, original: protected */
    public List<MpoTag> getAllTags() {
        int length;
        ArrayList<MpoTag> ret = new ArrayList();
        MpoTag[] tags = this.mMpIndexIfdData.getAllTags();
        int i = 0;
        if (tags != null) {
            for (MpoTag t : tags) {
                ret.add(t);
            }
        }
        tags = this.mMpAttribIfdData.getAllTags();
        if (tags != null) {
            length = tags.length;
            while (i < length) {
                ret.add(tags[i]);
                i++;
            }
        }
        if (ret.size() == 0) {
            return null;
        }
        return ret;
    }

    /* Access modifiers changed, original: protected */
    public List<MpoTag> getAllTagsForIfd(int ifd) {
        MpoTag[] tags = getMpIfdData(ifd).getAllTags();
        if (tags == null) {
            return null;
        }
        ArrayList<MpoTag> ret = new ArrayList(tags.length);
        for (MpoTag t : tags) {
            ret.add(t);
        }
        if (ret.size() == 0) {
            return null;
        }
        return ret;
    }

    /* Access modifiers changed, original: protected */
    public List<MpoTag> getAllTagsForTagId(short tag) {
        ArrayList<MpoTag> ret = new ArrayList();
        MpoTag t = this.mMpIndexIfdData.getTag(tag);
        if (t != null) {
            ret.add(t);
        }
        t = this.mMpAttribIfdData.getTag(tag);
        if (t != null) {
            ret.add(t);
        }
        if (ret.size() == 0) {
            return null;
        }
        return ret;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MpoImageData)) {
            return false;
        }
        MpoImageData data = (MpoImageData) obj;
        if (data.mByteOrder != this.mByteOrder) {
            return false;
        }
        MpoIfdData indexIfd1 = data.getMpIfdData(1);
        MpoIfdData indexIfd2 = getMpIfdData(1);
        if (indexIfd1 != indexIfd2 && indexIfd1 != null && !indexIfd1.equals(indexIfd2)) {
            return false;
        }
        MpoIfdData attribIfd1 = data.getMpIfdData(2);
        MpoIfdData attribIfd2 = getMpIfdData(2);
        if (attribIfd1 == attribIfd2 || attribIfd1 == null || attribIfd1.equals(attribIfd2)) {
            return true;
        }
        return false;
    }

    private int calculateOffsetOfIfd(MpoIfdData ifd, int offset) {
        offset += (2 + (ifd.getTagCount() * 12)) + 4;
        for (MpoTag tag : ifd.getAllTags()) {
            if (tag.getDataSize() > 4) {
                tag.setOffset(offset);
                offset += tag.getDataSize();
            }
        }
        return offset;
    }

    public int calculateAllIfdOffsets() {
        int offset = 8;
        MpoIfdData indexIfd = getIndexIfdData();
        if (indexIfd.getTagCount() > 0) {
            offset = calculateOffsetOfIfd(indexIfd, 8);
        }
        MpoIfdData attribIfd = getAttribIfdData();
        if (attribIfd.getTagCount() <= 0) {
            return offset;
        }
        indexIfd.setOffsetToNextIfd(offset);
        return calculateOffsetOfIfd(attribIfd, offset);
    }

    public int calculateImageSize() {
        return (8 + calculateAllIfdOffsets()) + this.mJpegData.length;
    }
}
