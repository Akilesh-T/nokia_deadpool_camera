package com.hmdglobal.app.camera.mpo;

import com.adobe.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.List;

public class MpoData {
    private ArrayList<MpoImageData> mAuxiliaryImages = new ArrayList();
    private MpoImageData mPrimaryMpoImage;

    public void setPrimaryMpoImage(MpoImageData image) {
        this.mPrimaryMpoImage = image;
        addDefaultAttribIfdTags(this.mPrimaryMpoImage, 1);
        addDefaultIndexIfdTags();
    }

    public void addAuxiliaryMpoImage(MpoImageData image) {
        this.mAuxiliaryImages.add(image);
        addDefaultAttribIfdTags(image, getAuxiliaryImageCount() + (this.mPrimaryMpoImage == null ? 0 : 1));
    }

    public boolean removeAuxiliaryMpoImage(MpoImageData image) {
        return this.mAuxiliaryImages.remove(image);
    }

    public MpoImageData getPrimaryMpoImage() {
        return this.mPrimaryMpoImage;
    }

    public List<MpoImageData> getAuxiliaryMpoImages() {
        return this.mAuxiliaryImages;
    }

    public int getAuxiliaryImageCount() {
        return this.mAuxiliaryImages.size();
    }

    public void addDefaultAttribIfdTags(MpoImageData image, int imageNum) {
        MpoTag mpFormatVersionTag = new MpoTag((short) MpoInterface.TAG_MP_FORMAT_VERSION, (short) 7, 4, 2, true);
        mpFormatVersionTag.setValue(MpoIfdData.MP_FORMAT_VER_VALUE);
        image.addTag(mpFormatVersionTag);
        MpoTag mpoTag = new MpoTag((short) MpoInterface.TAG_IMAGE_NUMBER, (short) 4, 1, 2, false);
        mpoTag.setValue(imageNum);
        image.addTag(mpoTag);
    }

    public void addDefaultIndexIfdTags() {
        if (this.mPrimaryMpoImage == null) {
            throw new IllegalArgumentException("Primary Mpo Image has not been set");
        } else if (getAuxiliaryImageCount() != 0) {
            if (this.mPrimaryMpoImage.getTag((short) MpoInterface.TAG_MP_FORMAT_VERSION, 1) == null) {
                MpoTag mpFormatVersionTag = new MpoTag((short) MpoInterface.TAG_MP_FORMAT_VERSION, (short) 7, 4, 1, true);
                mpFormatVersionTag.setValue(MpoIfdData.MP_FORMAT_VER_VALUE);
                this.mPrimaryMpoImage.addTag(mpFormatVersionTag);
            }
            MpoTag numImagesTag = this.mPrimaryMpoImage.getTag((short) MpoInterface.TAG_NUM_IMAGES, 1);
            if (numImagesTag == null) {
                numImagesTag = new MpoTag((short) MpoInterface.TAG_NUM_IMAGES, (short) 4, 1, 1, false);
            }
            numImagesTag.setValue(getAuxiliaryImageCount() + 1);
            this.mPrimaryMpoImage.addTag(numImagesTag);
            MpoTag mpoTag = new MpoTag((short) MpoInterface.TAG_MP_ENTRY, (short) 7, 0, 1, false);
            ArrayList<MpEntry> mpEntries = new ArrayList(getAuxiliaryImageCount() + 1);
            mpEntries.add(new MpEntry());
            for (int i = 0; i < getAuxiliaryImageCount(); i++) {
                mpEntries.add(new MpEntry());
            }
            mpoTag.setValue(mpEntries);
            this.mPrimaryMpoImage.addTag(mpoTag);
        } else {
            throw new IllegalArgumentException("No auxiliary images have been added");
        }
    }

    public void updateAllTags() {
        updateAttribIfdTags();
        updateIndexIfdTags();
    }

    private void updateIndexIfdTags() {
        if (this.mPrimaryMpoImage == null) {
            throw new IllegalArgumentException("Primary Mpo Image has not been set");
        } else if (getAuxiliaryImageCount() != 0) {
            MpoTag numImagesTag = this.mPrimaryMpoImage.getTag((short) MpoInterface.TAG_NUM_IMAGES, 1);
            if (numImagesTag == null) {
                numImagesTag = new MpoTag((short) MpoInterface.TAG_NUM_IMAGES, (short) 4, 1, 1, false);
            }
            numImagesTag.setValue(getAuxiliaryImageCount() + 1);
            this.mPrimaryMpoImage.addTag(numImagesTag);
            MpoTag mpoTag = new MpoTag((short) MpoInterface.TAG_MP_ENTRY, (short) 7, 0, 1, false);
            ArrayList<MpEntry> mpEntries = new ArrayList(getAuxiliaryImageCount() + 1);
            mpEntries.add(new MpEntry(PropertyOptions.DELETE_EXISTING, this.mPrimaryMpoImage.calculateImageSize(), 0));
            int imgOffset = 0 + this.mPrimaryMpoImage.calculateImageSize();
            for (MpoImageData image : getAuxiliaryMpoImages()) {
                int imageSize = image.calculateImageSize();
                mpEntries.add(new MpEntry(131074, imageSize, imgOffset));
                imgOffset += imageSize;
            }
            mpoTag.setValue(mpEntries);
            this.mPrimaryMpoImage.addTag(mpoTag);
        } else {
            throw new IllegalArgumentException("No auxiliary images have been added");
        }
    }

    private void updateAttribIfdTags() {
        if (this.mPrimaryMpoImage == null) {
            throw new IllegalArgumentException("Primary Mpo Image has not been set");
        } else if (getAuxiliaryImageCount() != 0) {
            int imageNum = 1;
            MpoTag imageNumTag = new MpoTag((short) MpoInterface.TAG_IMAGE_NUMBER, (short) 4, 1, 2, false);
            imageNumTag.setValue(4294967295L);
            this.mPrimaryMpoImage.addTag(imageNumTag);
            for (MpoImageData image : getAuxiliaryMpoImages()) {
                imageNumTag = new MpoTag((short) MpoInterface.TAG_IMAGE_NUMBER, (short) 4, 1, 2, false);
                int imageNum2 = imageNum + 1;
                imageNumTag.setValue(imageNum);
                image.addTag(imageNumTag);
                imageNum = imageNum2;
            }
        } else {
            throw new IllegalArgumentException("No auxiliary images have been added");
        }
    }
}
