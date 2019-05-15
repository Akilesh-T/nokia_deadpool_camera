package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.math.BigInteger;
import javax.annotation.Nullable;

@GwtCompatible
final class MathPreconditions {
    static int checkPositive(@Nullable String role, int x) {
        if (x > 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be > 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static long checkPositive(@Nullable String role, long x) {
        if (x > 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be > 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static BigInteger checkPositive(@Nullable String role, BigInteger x) {
        if (x.signum() > 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be > 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static int checkNonNegative(@Nullable String role, int x) {
        if (x >= 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be >= 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static long checkNonNegative(@Nullable String role, long x) {
        if (x >= 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be >= 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static BigInteger checkNonNegative(@Nullable String role, BigInteger x) {
        if (x.signum() >= 0) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be >= 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static double checkNonNegative(@Nullable String role, double x) {
        if (x >= Camera2ParamsFragment.TARGET_EV) {
            return x;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(role);
        stringBuilder.append(" (");
        stringBuilder.append(x);
        stringBuilder.append(") must be >= 0");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static void checkRoundingUnnecessary(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        }
    }

    static void checkInRange(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("not in range");
        }
    }

    static void checkNoOverflow(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("overflow");
        }
    }

    private MathPreconditions() {
    }
}
