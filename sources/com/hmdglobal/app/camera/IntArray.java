package com.hmdglobal.app.camera;

public class IntArray {
    private static final int INIT_CAPACITY = 8;
    private int[] mData = new int[8];
    private int mSize = 0;

    public void add(int value) {
        int[] temp;
        if (this.mData.length == this.mSize) {
            temp = new int[(this.mSize + this.mSize)];
            System.arraycopy(this.mData, 0, temp, 0, this.mSize);
            this.mData = temp;
        }
        temp = this.mData;
        int i = this.mSize;
        this.mSize = i + 1;
        temp[i] = value;
    }

    public int size() {
        return this.mSize;
    }

    public int[] toArray(int[] result) {
        Object result2;
        if (result2 == null || result2.length < this.mSize) {
            result2 = new int[this.mSize];
        }
        System.arraycopy(this.mData, 0, result2, 0, this.mSize);
        return result2;
    }
}
