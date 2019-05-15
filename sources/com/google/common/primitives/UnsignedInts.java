package com.google.common.primitives;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Comparator;

@GwtCompatible
@Beta
public final class UnsignedInts {
    static final long INT_MASK = 4294967295L;

    enum LexicographicalComparator implements Comparator<int[]> {
        INSTANCE;

        public int compare(int[] left, int[] right) {
            int minLength = Math.min(left.length, right.length);
            for (int i = 0; i < minLength; i++) {
                if (left[i] != right[i]) {
                    return UnsignedInts.compare(left[i], right[i]);
                }
            }
            return left.length - right.length;
        }
    }

    private UnsignedInts() {
    }

    static int flip(int value) {
        return Integer.MIN_VALUE ^ value;
    }

    public static int compare(int a, int b) {
        return Ints.compare(flip(a), flip(b));
    }

    public static long toLong(int value) {
        return ((long) value) & INT_MASK;
    }

    public static int min(int... array) {
        int i = 1;
        Preconditions.checkArgument(array.length > 0);
        int min = flip(array[0]);
        while (true) {
            int i2 = i;
            if (i2 >= array.length) {
                return flip(min);
            }
            i = flip(array[i2]);
            if (i < min) {
                min = i;
            }
            i = i2 + 1;
        }
    }

    public static int max(int... array) {
        int i = 1;
        Preconditions.checkArgument(array.length > 0);
        int max = flip(array[0]);
        while (true) {
            int i2 = i;
            if (i2 >= array.length) {
                return flip(max);
            }
            i = flip(array[i2]);
            if (i > max) {
                max = i;
            }
            i = i2 + 1;
        }
    }

    public static String join(String separator, int... array) {
        Preconditions.checkNotNull(separator);
        if (array.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(array.length * 5);
        builder.append(toString(array[0]));
        for (int i = 1; i < array.length; i++) {
            builder.append(separator);
            builder.append(toString(array[i]));
        }
        return builder.toString();
    }

    public static Comparator<int[]> lexicographicalComparator() {
        return LexicographicalComparator.INSTANCE;
    }

    public static int divide(int dividend, int divisor) {
        return (int) (toLong(dividend) / toLong(divisor));
    }

    public static int remainder(int dividend, int divisor) {
        return (int) (toLong(dividend) % toLong(divisor));
    }

    public static int decode(String stringValue) {
        ParseRequest request = ParseRequest.fromString(stringValue);
        try {
            return parseUnsignedInt(request.rawValue, request.radix);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing value: ");
            stringBuilder.append(stringValue);
            NumberFormatException decodeException = new NumberFormatException(stringBuilder.toString());
            decodeException.initCause(e);
            throw decodeException;
        }
    }

    public static int parseUnsignedInt(String s) {
        return parseUnsignedInt(s, 10);
    }

    public static int parseUnsignedInt(String string, int radix) {
        Preconditions.checkNotNull(string);
        long result = Long.parseLong(string, radix);
        if ((INT_MASK & result) == result) {
            return (int) result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Input ");
        stringBuilder.append(string);
        stringBuilder.append(" in base ");
        stringBuilder.append(radix);
        stringBuilder.append(" is not in the range of an unsigned integer");
        throw new NumberFormatException(stringBuilder.toString());
    }

    public static String toString(int x) {
        return toString(x, 10);
    }

    public static String toString(int x, int radix) {
        return Long.toString(((long) x) & INT_MASK, radix);
    }
}
