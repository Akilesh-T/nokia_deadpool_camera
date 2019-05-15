package com.hmdglobal.app.camera.mpo;

import java.util.HashMap;
import java.util.Map;

public class MpoIfdData {
    public static final byte[] MP_FORMAT_VER_VALUE = new byte[]{(byte) 48, (byte) 49, (byte) 48, (byte) 48};
    public static final int TYPE_MP_ATTRIB_IFD = 2;
    public static final int TYPE_MP_INDEX_IFD = 1;
    private final int mIfdId;
    private int mOffsetToNextIfd = 0;
    private final Map<Short, MpoTag> mTags = new HashMap();

    public MpoIfdData(int ifdId) {
        this.mIfdId = ifdId;
    }

    /* Access modifiers changed, original: protected */
    public MpoTag[] getAllTags() {
        return (MpoTag[]) this.mTags.values().toArray(new MpoTag[this.mTags.size()]);
    }

    /* Access modifiers changed, original: protected */
    public MpoTag getTag(short tagId) {
        return (MpoTag) this.mTags.get(Short.valueOf(tagId));
    }

    /* Access modifiers changed, original: protected */
    public MpoTag setTag(MpoTag tag) {
        tag.setIfd(this.mIfdId);
        return (MpoTag) this.mTags.put(Short.valueOf(tag.getTagId()), tag);
    }

    /* Access modifiers changed, original: protected */
    public boolean checkCollision(short tagId) {
        return this.mTags.get(Short.valueOf(tagId)) != null;
    }

    /* Access modifiers changed, original: protected */
    public void removeTag(short tagId) {
        this.mTags.remove(Short.valueOf(tagId));
    }

    /* Access modifiers changed, original: protected */
    public int getTagCount() {
        return this.mTags.size();
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
        if (obj != null && (obj instanceof MpoIfdData)) {
            MpoIfdData data = (MpoIfdData) obj;
            if (data.getTagCount() == getTagCount()) {
                for (MpoTag tag : data.getAllTags()) {
                    if (!tag.equals((MpoTag) this.mTags.get(Short.valueOf(tag.getTagId())))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
