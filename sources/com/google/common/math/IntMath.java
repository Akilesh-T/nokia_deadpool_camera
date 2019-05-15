package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import java.math.RoundingMode;

@GwtCompatible(emulated = true)
public final class IntMath {
    @VisibleForTesting
    static final int FLOOR_SQRT_MAX_INT = 46340;
    @VisibleForTesting
    static final int MAX_POWER_OF_SQRT2_UNSIGNED = -1257966797;
    @VisibleForTesting
    static int[] biggestBinomials = new int[]{ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, 65536, 2345, 477, 193, 110, 75, 58, 49, 43, 39, 37, 35, 34, 34, 33};
    private static final int[] factorials = new int[]{1, 1, 2, 6, 24, 120, MotionPictureHelper.FRAME_HEIGHT_9, 5040, 40320, 362880, 3628800, 39916800, 479001600};
    @VisibleForTesting
    static final int[] halfPowersOf10 = new int[]{3, 31, 316, 3162, 31622, 316227, 3162277, 31622776, 316227766, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED};
    @VisibleForTesting
    static final byte[] maxLog10ForLeadingZeros = new byte[]{(byte) 9, (byte) 9, (byte) 9, (byte) 8, (byte) 8, (byte) 8, (byte) 7, (byte) 7, (byte) 7, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 5, (byte) 5, (byte) 5, (byte) 4, (byte) 4, (byte) 4, (byte) 3, (byte) 3, (byte) 3, (byte) 3, (byte) 2, (byte) 2, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    @VisibleForTesting
    static final int[] powersOf10 = new int[]{1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    /* renamed from: com.google.common.math.IntMath$1 */
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

    public static boolean isPowerOfTwo(int x) {
        int i = 0;
        int i2 = x > 0 ? 1 : 0;
        if (((x - 1) & x) == 0) {
            i = 1;
        }
        return i & i2;
    }

    @VisibleForTesting
    static int lessThanBranchFree(int x, int y) {
        return (~(~(x - y))) >>> 31;
    }

    public static int log2(int x, RoundingMode mode) {
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
                return 32 - Integer.numberOfLeadingZeros(x - 1);
            case 6:
            case 7:
            case 8:
                int leadingZeros = Integer.numberOfLeadingZeros(x);
                return lessThanBranchFree(MAX_POWER_OF_SQRT2_UNSIGNED >>> leadingZeros, x) + (31 - leadingZeros);
            default:
                throw new AssertionError();
        }
        return 31 - Integer.numberOfLeadingZeros(x);
    }

    @GwtIncompatible("need BigIntegerMath to adequately test")
    public static int log10(int x, RoundingMode mode) {
        MathPreconditions.checkPositive("x", x);
        int logFloor = log10Floor(x);
        int floorPow = powersOf10[logFloor];
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

    private static int log10Floor(int x) {
        int y = maxLog10ForLeadingZeros[Integer.numberOfLeadingZeros(x)];
        return y - lessThanBranchFree(x, powersOf10[y]);
    }

    @GwtIncompatible("failing tests")
    public static int pow(int b, int k) {
        MathPreconditions.checkNonNegative("exponent", k);
        int i = 0;
        int i2 = 1;
        switch (b) {
            case -2:
                if (k >= 32) {
                    return 0;
                }
                return (k & 1) == 0 ? 1 << k : -(1 << k);
            case -1:
                if ((k & 1) != 0) {
                    i2 = -1;
                }
                return i2;
            case 0:
                if (k == 0) {
                    i = 1;
                }
                return i;
            case 1:
                return 1;
            case 2:
                if (k < 32) {
                    i = 1 << k;
                }
                return i;
            default:
                int b2 = b;
                b = 1;
                while (true) {
                    switch (k) {
                        case 0:
                            return b;
                        case 1:
                            return b2 * b;
                        default:
                            b *= (k & 1) == 0 ? 1 : b2;
                            b2 *= b2;
                            k >>= 1;
                    }
                }
        }
    }

    @GwtIncompatible("need BigIntegerMath to adequately test")
    public static int sqrt(int x, RoundingMode mode) {
        MathPreconditions.checkNonNegative("x", x);
        int sqrtFloor = sqrtFloor(x);
        switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
            case 1:
                MathPreconditions.checkRoundingUnnecessary(sqrtFloor * sqrtFloor == x);
                break;
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                return lessThanBranchFree(sqrtFloor * sqrtFloor, x) + sqrtFloor;
            case 6:
            case 7:
            case 8:
                return lessThanBranchFree((sqrtFloor * sqrtFloor) + sqrtFloor, x) + sqrtFloor;
            default:
                throw new AssertionError();
        }
        return sqrtFloor;
    }

    private static int sqrtFloor(int x) {
        return (int) Math.sqrt((double) x);
    }

    public static int divide(int p, int q, RoundingMode mode) {
        Preconditions.checkNotNull(mode);
        if (q != 0) {
            int div = p / q;
            int rem = p - (q * div);
            if (rem == 0) {
                return div;
            }
            boolean increment = true;
            int signum = ((p ^ q) >> 31) | 1;
            switch (AnonymousClass1.$SwitchMap$java$math$RoundingMode[mode.ordinal()]) {
                case 1:
                    if (rem != 0) {
                        increment = false;
                    }
                    MathPreconditions.checkRoundingUnnecessary(increment);
                    break;
                case 2:
                    break;
                case 3:
                    if (signum >= 0) {
                        increment = false;
                        break;
                    }
                    break;
                case 4:
                    increment = true;
                    break;
                case 5:
                    if (signum <= 0) {
                        increment = false;
                        break;
                    }
                    break;
                case 6:
                case 7:
                case 8:
                    int absRem = Math.abs(rem);
                    int cmpRemToHalfDivisor = absRem - (Math.abs(q) - absRem);
                    if (cmpRemToHalfDivisor != 0) {
                        if (cmpRemToHalfDivisor <= 0) {
                            increment = false;
                            break;
                        }
                    } else if (mode != RoundingMode.HALF_UP) {
                        if (((mode == RoundingMode.HALF_EVEN ? 1 : 0) & ((div & 1) != 0 ? 1 : 0)) == 0) {
                            increment = false;
                            break;
                        }
                    }
                    break;
                default:
                    throw new AssertionError();
            }
            increment = false;
            return increment ? div + signum : div;
        }
        throw new ArithmeticException("/ by zero");
    }

    public static int mod(int x, int m) {
        if (m > 0) {
            int result = x % m;
            return result >= 0 ? result : result + m;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Modulus ");
            stringBuilder.append(m);
            stringBuilder.append(" must be > 0");
            throw new ArithmeticException(stringBuilder.toString());
        }
    }

    public static int gcd(int a, int b) {
        MathPreconditions.checkNonNegative("a", a);
        MathPreconditions.checkNonNegative("b", b);
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos;
        int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos;
        while (a != b) {
            int delta = a - b;
            int minDeltaOrZero = (delta >> 31) & delta;
            int a2 = (delta - minDeltaOrZero) - minDeltaOrZero;
            b += minDeltaOrZero;
            a = a2 >> Integer.numberOfTrailingZeros(a2);
        }
        return a << Math.min(aTwos, bTwos);
    }

    public static int checkedAdd(int a, int b) {
        long result = ((long) a) + ((long) b);
        MathPreconditions.checkNoOverflow(result == ((long) ((int) result)));
        return (int) result;
    }

    public static int checkedSubtract(int a, int b) {
        long result = ((long) a) - ((long) b);
        MathPreconditions.checkNoOverflow(result == ((long) ((int) result)));
        return (int) result;
    }

    public static int checkedMultiply(int a, int b) {
        long result = ((long) a) * ((long) b);
        MathPreconditions.checkNoOverflow(result == ((long) ((int) result)));
        return (int) result;
    }

    public static int checkedPow(int b, int k) {
        MathPreconditions.checkNonNegative("exponent", k);
        int i = -1;
        boolean z = false;
        switch (b) {
            case -2:
                if (k < 32) {
                    z = true;
                }
                MathPreconditions.checkNoOverflow(z);
                return (k & 1) == 0 ? 1 << k : -1 << k;
            case -1:
                if ((k & 1) == 0) {
                    i = 1;
                }
                return i;
            case 0:
                int i2;
                if (k == 0) {
                    i2 = 1;
                }
                return i2;
            case 1:
                return 1;
            case 2:
                if (k < 31) {
                    z = true;
                }
                MathPreconditions.checkNoOverflow(z);
                return 1 << k;
            default:
                i = b;
                b = 1;
                while (true) {
                    switch (k) {
                        case 0:
                            return b;
                        case 1:
                            return checkedMultiply(b, i);
                        default:
                            if ((k & 1) != 0) {
                                b = checkedMultiply(b, i);
                            }
                            k >>= 1;
                            if (k > 0) {
                                MathPreconditions.checkNoOverflow((-46340 <= i ? 1 : 0) & (i <= FLOOR_SQRT_MAX_INT ? 1 : 0));
                                i *= i;
                            }
                    }
                }
        }
    }

    public static int factorial(int n) {
        MathPreconditions.checkNonNegative("n", n);
        return n < factorials.length ? factorials[n] : ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    }

    @GwtIncompatible("need BigIntegerMath to adequately test")
    public static int binomial(int n, int k) {
        MathPreconditions.checkNonNegative("n", n);
        MathPreconditions.checkNonNegative("k", k);
        int i = 0;
        Preconditions.checkArgument(k <= n, "k (%s) > n (%s)", Integer.valueOf(k), Integer.valueOf(n));
        if (k > (n >> 1)) {
            k = n - k;
        }
        if (k >= biggestBinomials.length || n > biggestBinomials[k]) {
            return ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }
        switch (k) {
            case 0:
                return 1;
            case 1:
                return n;
            default:
                long result = 1;
                while (i < k) {
                    result = (result * ((long) (n - i))) / ((long) (i + 1));
                    i++;
                }
                return (int) result;
        }
    }

    public static int mean(int x, int y) {
        return (x & y) + ((x ^ y) >> 1);
    }

    private IntMath() {
    }
}
