package com.hmdglobal.app.camera.settings;

import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.util.ApiHelper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ResolutionUtil {
    public static final String NEXUS_5_LARGE_16_BY_9 = "1836x3264";
    public static final float NEXUS_5_LARGE_16_BY_9_ASPECT_RATIO = 1.7777778f;
    public static Size NEXUS_5_LARGE_16_BY_9_SIZE = new Size(1836, 3264);
    private static final float RATIO_TOLERANCE = 0.05f;
    private static Size[] sDesiredAspectRatioSizes = new Size[]{new Size(16, 9), new Size(4, 3), new Size(1, 1)};
    private static Float[] sDesiredAspectRatios = new Float[]{Float.valueOf(1.7777778f), Float.valueOf(1.3333334f), Float.valueOf(1.0f)};

    private static class ResolutionBucket {
        public Float aspectRatio;
        public Size largest;
        public Integer maxPixels;
        public List<Size> sizes;

        private ResolutionBucket() {
            this.sizes = new LinkedList();
            this.maxPixels = Integer.valueOf(0);
        }

        /* synthetic */ ResolutionBucket(AnonymousClass1 x0) {
            this();
        }

        public void add(Size size) {
            this.sizes.add(size);
            Collections.sort(this.sizes, new Comparator<Size>() {
                public int compare(Size size, Size size2) {
                    return Integer.compare(size2.width() * size2.height(), size.width() * size.height());
                }
            });
            this.maxPixels = Integer.valueOf(((Size) this.sizes.get(0)).width() * ((Size) this.sizes.get(0)).height());
        }
    }

    public static List<Size> getDisplayableSizesFromSupported(List<Size> sizes, boolean isBackCamera) {
        Float aspectRatio;
        List<ResolutionBucket> buckets = parseAvailableSizes(sizes, isBackCamera);
        List<Float> sortedDesiredAspectRatios = new ArrayList();
        sortedDesiredAspectRatios.add(Float.valueOf(((ResolutionBucket) buckets.get(0)).aspectRatio.floatValue()));
        for (ResolutionBucket bucket : buckets) {
            aspectRatio = bucket.aspectRatio;
            if (Arrays.asList(sDesiredAspectRatios).contains(aspectRatio) && !sortedDesiredAspectRatios.contains(aspectRatio)) {
                sortedDesiredAspectRatios.add(aspectRatio);
            }
        }
        List<Size> result = new ArrayList(sizes.size());
        for (Float aspectRatio2 : sortedDesiredAspectRatios) {
            for (ResolutionBucket bucket2 : buckets) {
                if (Math.abs(bucket2.aspectRatio.floatValue() - aspectRatio2.floatValue()) <= RATIO_TOLERANCE) {
                    result.addAll(pickUpToThree(bucket2.sizes));
                }
            }
        }
        return result;
    }

    private static int area(Size size) {
        if (size == null) {
            return 0;
        }
        return size.width() * size.height();
    }

    private static List<Size> pickUpToThree(List<Size> sizes) {
        List<Size> result = new ArrayList();
        Size largest = (Size) sizes.get(0);
        result.add(largest);
        Size lastSize = largest;
        for (Size size : sizes) {
            double targetArea = Math.pow(0.5d, (double) result.size()) * ((double) area(largest));
            if (((double) area(size)) < targetArea) {
                if (result.contains(lastSize) || targetArea - ((double) area(lastSize)) >= ((double) area(size)) - targetArea) {
                    result.add(size);
                } else {
                    result.add(lastSize);
                }
            }
            lastSize = size;
            if (result.size() == 3) {
                break;
            }
        }
        if (result.size() < 3 && !result.contains(lastSize)) {
            result.add(lastSize);
        }
        return result;
    }

    private static float fuzzAspectRatio(float aspectRatio) {
        for (float desiredAspectRatio : sDesiredAspectRatios) {
            float desiredAspectRatio2 = desiredAspectRatio2.floatValue();
            if (Math.abs(aspectRatio - desiredAspectRatio2) < RATIO_TOLERANCE) {
                return desiredAspectRatio2;
            }
        }
        return aspectRatio;
    }

    private static List<ResolutionBucket> parseAvailableSizes(List<Size> sizes, boolean isBackCamera) {
        HashMap<Float, ResolutionBucket> aspectRatioToBuckets = new HashMap();
        for (Size size : sizes) {
            Float aspectRatio = Float.valueOf(fuzzAspectRatio(Float.valueOf(((float) size.width()) / ((float) size.height())).floatValue()));
            ResolutionBucket bucket = (ResolutionBucket) aspectRatioToBuckets.get(aspectRatio);
            if (bucket == null) {
                bucket = new ResolutionBucket();
                bucket.aspectRatio = aspectRatio;
                aspectRatioToBuckets.put(aspectRatio, bucket);
            }
            bucket.add(size);
        }
        if (ApiHelper.IS_NEXUS_5 && isBackCamera) {
            ((ResolutionBucket) aspectRatioToBuckets.get(Float.valueOf(1.7777778f))).add(NEXUS_5_LARGE_16_BY_9_SIZE);
        }
        List<ResolutionBucket> sortedBuckets = new ArrayList(aspectRatioToBuckets.values());
        Collections.sort(sortedBuckets, new Comparator<ResolutionBucket>() {
            public int compare(ResolutionBucket resolutionBucket, ResolutionBucket resolutionBucket2) {
                return Integer.compare(resolutionBucket2.maxPixels.intValue(), resolutionBucket.maxPixels.intValue());
            }
        });
        return sortedBuckets;
    }

    public static String aspectRatioDescription(Size size) {
        Size aspectRatio = reduce(size);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(aspectRatio.width());
        stringBuilder.append("x");
        stringBuilder.append(aspectRatio.height());
        return stringBuilder.toString();
    }

    public static Size reduce(Size aspectRatio) {
        BigInteger width = BigInteger.valueOf((long) aspectRatio.width());
        BigInteger height = BigInteger.valueOf((long) aspectRatio.height());
        BigInteger gcd = width.gcd(height);
        return new Size(Math.max(width.intValue(), height.intValue()) / gcd.intValue(), Math.min(width.intValue(), height.intValue()) / gcd.intValue());
    }

    public static int aspectRatioNumerator(Size size) {
        return reduce(size).width();
    }

    public static Size getApproximateSize(Size size) {
        Size aspectRatio = reduce(size);
        int index = Arrays.asList(sDesiredAspectRatios).indexOf(Float.valueOf(fuzzAspectRatio(((float) size.width()) / ((float) size.height()))));
        if (index != -1) {
            return new Size(sDesiredAspectRatioSizes[index]);
        }
        return aspectRatio;
    }

    public static com.hmdglobal.app.camera.util.Size getApproximateSize(com.hmdglobal.app.camera.util.Size size) {
        Size result = getApproximateSize(new Size(size.getWidth(), size.getHeight()));
        return new com.hmdglobal.app.camera.util.Size(result.width(), result.height());
    }

    public static int aspectRatioDenominator(Size size) {
        BigInteger width = BigInteger.valueOf((long) size.width());
        BigInteger height = BigInteger.valueOf((long) size.height());
        return Math.min(width.intValue(), height.intValue()) / width.gcd(height).intValue();
    }
}
