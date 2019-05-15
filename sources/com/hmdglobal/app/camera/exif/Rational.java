package com.hmdglobal.app.camera.exif;

public class Rational {
    private final long mDenominator;
    private final long mNumerator;

    public Rational(long nominator, long denominator) {
        this.mNumerator = nominator;
        this.mDenominator = denominator;
    }

    public Rational(Rational r) {
        this.mNumerator = r.mNumerator;
        this.mDenominator = r.mDenominator;
    }

    public long getNumerator() {
        return this.mNumerator;
    }

    public long getDenominator() {
        return this.mDenominator;
    }

    public double toDouble() {
        return ((double) this.mNumerator) / ((double) this.mDenominator);
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rational)) {
            return false;
        }
        Rational data = (Rational) obj;
        if (this.mNumerator == data.mNumerator && this.mDenominator == data.mDenominator) {
            z = true;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mNumerator);
        stringBuilder.append("/");
        stringBuilder.append(this.mDenominator);
        return stringBuilder.toString();
    }
}
