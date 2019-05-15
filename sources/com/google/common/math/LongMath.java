package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import java.math.RoundingMode;

@GwtCompatible(emulated = true)
public final class LongMath {
    @VisibleForTesting
    static final long FLOOR_SQRT_MAX_LONG = 3037000499L;
    @VisibleForTesting
    static final long MAX_POWER_OF_SQRT2_UNSIGNED = -5402926248376769404L;
    static final int[] biggestBinomials = new int[]{ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, 3810779, 121977, 16175, 4337, 1733, 887, 534, 361, 265, 206, 169, 143, 125, 111, 101, 94, 88, 83, 79, 76, 74, 72, 70, 69, 68, 67, 67, 66, 66, 66, 66};
    @VisibleForTesting
    static final int[] biggestSimpleBinomials = new int[]{ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, 2642246, 86251, 11724, 3218, 1313, 684, 419, 287, 214, 169, Const.CODE_C1_TGW, 119, 105, 95, 87, 81, 76, 73, 70, 68, 66, 64, 63, 62, 62, 61, 61, 61};
    static final long[] factorials = new long[]{1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600, 6227020800L, 87178291200L, 1307674368000L, 20922789888000L, 355687428096000L, 6402373705728000L, 121645100408832000L, 2432902008176640000L};
    @GwtIncompatible("TODO")
    @VisibleForTesting
    static final long[] halfPowersOf10 = new long[]{3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, 3162277660L, 31622776601L, 316227766016L, 3162277660168L, 31622776601683L, 316227766016837L, 3162277660168379L, 31622776601683793L, 316227766016837933L, 3162277660168379331L};
    @VisibleForTesting
    static final byte[] maxLog10ForLeadingZeros = new byte[]{(byte) 19, Ascii.DC2, Ascii.DC2, Ascii.DC2, Ascii.DC2, (byte) 17, (byte) 17, (byte) 17, Ascii.DLE, Ascii.DLE, Ascii.DLE, Ascii.SI, Ascii.SI, Ascii.SI, Ascii.SI, Ascii.SO, Ascii.SO, Ascii.SO, Ascii.CR, Ascii.CR, Ascii.CR, Ascii.FF, Ascii.FF, Ascii.FF, Ascii.FF, Ascii.VT, Ascii.VT, Ascii.VT, (byte) 10, (byte) 10, (byte) 10, (byte) 9, (byte) 9, (byte) 9, (byte) 9, (byte) 8, (byte) 8, (byte) 8, (byte) 7, (byte) 7, (byte) 7, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 5, (byte) 5, (byte) 5, (byte) 4, (byte) 4, (byte) 4, (byte) 3, (byte) 3, (byte) 3, (byte) 3, (byte) 2, (byte) 2, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    @GwtIncompatible("TODO")
    @VisibleForTesting
    static final long[] powersOf10 = new long[]{1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L};

    /* renamed from: com.google.common.math.LongMath$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$java$math$RoundingMode = new int[RoundingMode.values().length];

        static {
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UNNECESSARY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.FLOOR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.UP.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.CEILING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_UP.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$java$math$RoundingMode[RoundingMode.HALF_EVEN.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    public static boolean isPowerOfTwo(long x) {
        int i = 0;
        int i2 = x > 0 ? 1 : 0;
        if (((x - 1) & x) == 0) {
            i = 1;
        }
        return i2 & i;
    }

    @VisibleForTesting
    static int lessThanBranchFree(long x, long y) {
        return (int) ((~(~(x - y))) >>> 63);
    }

    public static int log2(long x, RoundingMode mode) {
        MathPreconditions.checkPositive("x", x);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(x));
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return 64 - Long.numberOfLeadingZeros(x - 1);
            case 6:
            case 7:
            case 8:
                int leadingZeros = Long.numberOfLeadingZeros(x);
                return lessThanBranchFree(MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros, x) + (63 - leadingZeros);
            default:
                throw new AssertionError("impossible");
        }
        return 63 - Long.numberOfLeadingZeros(x);
    }

    @GwtIncompatible("TODO")
    public static int log10(long x, RoundingMode mode) {
        MathPreconditions.checkPositive("x", x);
        int logFloor = log10Floor(x);
        long floorPow = powersOf10[logFloor];
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(x == floorPow);
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return lessThanBranchFree(floorPow, x) + logFloor;
            case 6:
            case 7:
            case 8:
                return lessThanBranchFree(halfPowersOf10[logFloor], x) + logFloor;
            default:
                throw new AssertionError();
        }
        return logFloor;
    }

    @GwtIncompatible("TODO")
    static int log10Floor(long x) {
        int y = maxLog10ForLeadingZeros[Long.numberOfLeadingZeros(x)];
        return y - lessThanBranchFree(x, powersOf10[y]);
    }

    @GwtIncompatible("TODO")
    public static long pow(long b, int k) {
        MathPreconditions.checkNonNegative("exponent", k);
        long j = 1;
        if (-2 > b || b > 2) {
            long b2 = b;
            b = 1;
            while (true) {
                switch (k) {
                    case 0:
                        return b;
                    case 1:
                        return b * b2;
                    default:
                        b *= (k & 1) == 0 ? 1 : b2;
                        b2 *= b2;
                        k >>= 1;
                }
            }
        } else {
            long j2 = 0;
            switch ((int) b) {
                case -2:
                    if (k >= 64) {
                        return 0;
                    }
                    return (k & 1) == 0 ? 1 << k : -(1 << k);
                case -1:
                    if ((k & 1) != 0) {
                        j = -1;
                    }
                    return j;
                case 0:
                    if (k != 0) {
                        j = 0;
                    }
                    return j;
                case 1:
                    return 1;
                case 2:
                    if (k < 64) {
                        j2 = 1 << k;
                    }
                    return j2;
                default:
                    throw new AssertionError();
            }
        }
    }

    @GwtIncompatible("TODO")
    public static long sqrt(long x, RoundingMode mode) {
        MathPreconditions.checkNonNegative("x", x);
        if (fitsInInt(x)) {
            return (long) IntMath.sqrt((int) x, mode);
        }
        long guess = (long) Math.sqrt((double) x);
        long guessSquared = guess * guess;
        boolean z = false;
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
            case 1:
                if (guessSquared == x) {
                    z = true;
                }
                MathPreconditions.checkRoundingUnnecessary(z);
                return guess;
            case 2:
            case 3:
                if (x < guessSquared) {
                    return guess - 1;
                }
                return guess;
            case 4:
            case 5:
                if (x > guessSquared) {
                    return 1 + guess;
                }
                return guess;
            case 6:
            case 7:
            case 8:
                int i;
                if (x < guessSquared) {
                    i = 1;
                }
                long sqrtFloor = guess - ((long) i);
                return ((long) lessThanBranchFree((sqrtFloor * sqrtFloor) + sqrtFloor, x)) + sqrtFloor;
            default:
                throw new AssertionError();
        }
    }

    @GwtIncompatible("TODO")
    public static long divide(long p, long q, RoundingMode mode) {
        RoundingMode roundingMode = mode;
        Preconditions.checkNotNull(mode);
        long div = p / q;
        long rem = p - (q * div);
        if (rem == 0) {
            return div;
        }
        boolean increment;
        int signum = ((int) ((p ^ q) >> 63)) | 1;
        boolean z = false;
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
            case 1:
                if (rem == 0) {
                    z = true;
                }
                MathPreconditions.checkRoundingUnnecessary(z);
                break;
            case 2:
                break;
            case 3:
                if (signum < 0) {
                    z = true;
                }
                increment = z;
                break;
            case 4:
                increment = true;
                break;
            case 5:
                if (signum > 0) {
                    z = true;
                }
                increment = z;
                break;
            case 6:
            case 7:
            case 8:
                long absRem = Math.abs(rem);
                long cmpRemToHalfDivisor = absRem - (Math.abs(q) - absRem);
                if (cmpRemToHalfDivisor != 0) {
                    if (cmpRemToHalfDivisor > 0) {
                        z = true;
                    }
                    increment = z;
                    break;
                }
                int i;
                int i2 = roundingMode == RoundingMode.HALF_UP ? 1 : 0;
                int i3 = roundingMode == RoundingMode.HALF_EVEN ? 1 : 0;
                if ((1 & div) != 0) {
                    i = 1;
                }
                increment = (i3 & i) | i2;
                break;
            default:
                throw new AssertionError();
        }
        increment = false;
        return increment ? ((long) signum) + div : div;
    }

    @GwtIncompatible("TODO")
    public static int mod(long x, int m) {
        return (int) mod(x, (long) m);
    }

    @GwtIncompatible("TODO")
    public static long mod(long x, long m) {
        if (m > 0) {
            long result = x % m;
            return result >= 0 ? result : result + m;
        } else {
            throw new ArithmeticException("Modulus must be positive");
        }
    }

    public static long gcd(long a, long b) {
        MathPreconditions.checkNonNegative("a", a);
        MathPreconditions.checkNonNegative("b", b);
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        int aTwos = Long.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Long.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            long delta = a - b;
            long minDeltaOrZero = (delta >> 63) & delta;
            long a2 = (delta - minDeltaOrZero) - minDeltaOrZero;
            b += minDeltaOrZero;
            a = a2 >> Long.numberOfTrailingZeros(a2);
        }
        return a << Math.min(aTwos, bTwos);
    }

    @GwtIncompatible("TODO")
    public static long checkedAdd(long a, long b) {
        long result = a + b;
        int i = 0;
        int i2 = (a ^ b) < 0 ? 1 : 0;
        if ((a ^ result) >= 0) {
            i = 1;
        }
        MathPreconditions.checkNoOverflow(i2 | i);
        return result;
    }

    @GwtIncompatible("TODO")
    public static long checkedSubtract(long a, long b) {
        long result = a - b;
        int i = 0;
        int i2 = (a ^ b) >= 0 ? 1 : 0;
        if ((a ^ result) >= 0) {
            i = 1;
        }
        MathPreconditions.checkNoOverflow(i2 | i);
        return result;
    }

    @GwtIncompatible("TODO")
    public static long checkedMultiply(long a, long b) {
        int leadingZeros = ((Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a)) + Long.numberOfLeadingZeros(b)) + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > 65) {
            return a * b;
        }
        boolean z = false;
        MathPreconditions.checkNoOverflow(leadingZeros >= 64);
        MathPreconditions.checkNoOverflow((a >= 0 ? 1 : 0) | (b != Long.MIN_VALUE ? 1 : 0));
        long result = a * b;
        if (a == 0 || result / a == b) {
            z = true;
        }
        MathPreconditions.checkNoOverflow(z);
        return result;
    }

    @GwtIncompatible("TODO")
    public static long checkedPow(long b, int k) {
        MathPreconditions.checkNonNegative("exponent", k);
        boolean z = false;
        long accum = 1;
        if (((b >= -2 ? 1 : 0) & (b <= 2 ? 1 : 0)) != 0) {
            switch ((int) b) {
                case -2:
                    if (k < 64) {
                        z = true;
                    }
                    MathPreconditions.checkNoOverflow(z);
                    return (k & 1) == 0 ? 1 << k : -1 << k;
                case -1:
                    if ((k & 1) != 0) {
                        accum = -1;
                    }
                    return accum;
                case 0:
                    if (k != 0) {
                        accum = 0;
                    }
                    return accum;
                case 1:
                    return 1;
                case 2:
                    if (k < 63) {
                        z = true;
                    }
                    MathPreconditions.checkNoOverflow(z);
                    return 1 << k;
                default:
                    throw new AssertionError();
            }
        }
        while (true) {
            switch (k) {
                case 0:
                    return accum;
                case 1:
                    return checkedMultiply(accum, b);
                default:
                    if ((k & 1) != 0) {
                        accum = checkedMultiply(accum, b);
                    }
                    k >>= 1;
                    if (k > 0) {
                        MathPreconditions.checkNoOverflow(b <= FLOOR_SQRT_MAX_LONG);
                        b *= b;
                    }
            }
        }
    }

    @GwtIncompatible("TODO")
    public static long factorial(int n) {
        MathPreconditions.checkNonNegative("n", n);
        return n < factorials.length ? factorials[n] : Long.MAX_VALUE;
    }

    public static long binomial(int n, int k) {
        int i = n;
        int k2 = k;
        MathPreconditions.checkNonNegative("n", i);
        MathPreconditions.checkNonNegative("k", k2);
        int i2 = 2;
        Preconditions.checkArgument(k2 <= i, "k (%s) > n (%s)", Integer.valueOf(k), Integer.valueOf(n));
        if (k2 > (i >> 1)) {
            k2 = i - k2;
        }
        switch (k2) {
            case 0:
                return 1;
            case 1:
                return (long) i;
            default:
                if (i < factorials.length) {
                    return factorials[i] / (factorials[k2] * factorials[i - k2]);
                }
                if (k2 >= biggestBinomials.length || i > biggestBinomials[k2]) {
                    return Long.MAX_VALUE;
                }
                int nBits;
                long result;
                if (k2 >= biggestSimpleBinomials.length || i > biggestSimpleBinomials[k2]) {
                    nBits = log2((long) i, RoundingMode.CEILING);
                    result = 1;
                    int n2 = i - 1;
                    long numerator = (long) i;
                    i = nBits;
                    long numerator2 = numerator;
                    long denominator = 1;
                    while (i2 <= k2) {
                        if (i + nBits < 63) {
                            numerator2 *= (long) n2;
                            denominator *= (long) i2;
                            i += nBits;
                        } else {
                            result = multiplyFraction(result, numerator2, denominator);
                            i = nBits;
                            numerator2 = (long) n2;
                            denominator = (long) i2;
                        }
                        i2++;
                        n2--;
                    }
                    return multiplyFraction(result, numerator2, denominator);
                }
                nBits = i - 1;
                result = (long) i;
                while (true) {
                    i = i2;
                    if (i > k2) {
                        return result;
                    }
                    result = (result * ((long) nBits)) / ((long) i);
                    nBits--;
                    i2 = i + 1;
                }
                break;
        }
    }

    static long multiplyFraction(long x, long numerator, long denominator) {
        if (x == 1) {
            return numerator / denominator;
        }
        long commonDivisor = gcd(x, denominator);
        return (numerator / (denominator / commonDivisor)) * (x / commonDivisor);
    }

    static boolean fitsInInt(long x) {
        return ((long) ((int) x)) == x;
    }

    public static long mean(long x, long y) {
        return (x & y) + ((x ^ y) >> 1);
    }

    private LongMath() {
    }
}
