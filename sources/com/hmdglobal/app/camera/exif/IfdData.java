package com.hmdglobal.app.camera.exif;

import java.util.HashMap;
import java.util.Map;

class IfdData {
    private static final int[] sIfds = new int[]{0, 1, 2, 3, 4};
    private final Map<Short, ExifTag> mExifTags = new HashMap();
    private final int mIfdId;
    private int mOffsetToNextIfd = 0;

    IfdData(int ifdId) {
        this.mIfdId = ifdId;
    }

    protected static int[] getIfds() {
        return sIfds;
    }

    /* Access modifiers changed, original: protected */
    public ExifTag[] getAllTags() {
        return (ExifTag[]) this.mExifTags.values().toArray(new ExifTag[this.mExifTags.size()]);
    }

    /* Access modifiers changed, original: protected */
    public int getId() {
        return this.mIfdId;
    }

    /* Access modifiers changed, original: protected */
    public ExifTag getTag(short tagId) {
        return (ExifTag) this.mExifTags.get(Short.valueOf(tagId));
    }

    /* Access modifiers changed, original: protected */
    public ExifTag setTag(ExifTag tag) {
        tag.setIfd(this.mIfdId);
        return (ExifTag) this.mExifTags.put(Short.valueOf(tag.getTagId()), tag);
    }

    /* Access modifiers changed, original: protected */
    public boolean checkCollision(short tagId) {
        return this.mExifTags.get(Short.valueOf(tagId)) != null;
    }

    /* Access modifiers changed, original: protected */
    public void removeTag(short tagId) {
        this.mExifTags.remove(Short.valueOf(tagId));
    }

    /* Access modifiers changed, original: protected */
    public int getTagCount() {
        return this.mExifTags.size();
    }

    /* Access modifiers changed, original: protected */
    public void setOffsetToNextIfd(int offset) {
        this.mOffsetToNextIfd = offset;
    }

    /* Access modifiers changed, original: protected */
    public int getOffsetToNextIfd() {
        return this.mOffsetToNextIfd;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof IfdData)) {
            IfdData data = (IfdData) obj;
            if (data.getId() == this.mIfdId && data.getTagCount() == getTagCount()) {
                for (ExifTag tag : data.getAllTags()) {
                    if (!ExifInterface.isOffsetTag(tag.getTagId()) && !tag.equals((ExifTag) this.mExifTags.get(Short.valueOf(tag.getTagId())))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
