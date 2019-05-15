package com.morphoinc.utils.os;

import android.os.Build;
import android.os.Build.VERSION;

public class BuildUtil {
    private BuildUtil() {
    }

    public static int getSdkVersion() {
        return VERSION.SDK_INT;
    }

    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getModel() {
        return Build.MODEL;
    }

    public static String getBrand() {
        return Build.BRAND;
    }

    public static boolean isOverJellyBeanMr2() {
        return getSdkVersion() >= 18;
    }

    public static boolean isSony() {
        return getManufacturer().equals("Sony");
    }

    public static boolean isLGE() {
        return getManufacturer().equals("LGE");
    }

    public static boolean isSamsung() {
        return getManufacturer().equals("samsung");
    }

    public static boolean isG4() {
        String model = getModel();
        return model.contains("LG-H810") || model.contains("LG-VS986") || model.contains("LG-LS991") || model.contains("LG-US991") || model.contains("LG-H811") || model.contains("LG-H818") || model.contains("LG-H819") || model.contains("LG-H812") || model.contains("LG-F500L") || model.contains("LG-F500S") || model.contains("LG-F500K") || model.contains("LG-H815T") || model.contains("LG-H818N") || model.contains("LG-V32") || model.contains("LG-H815P") || model.contains("LG-H815");
    }

    public static boolean isG5() {
        String model = getModel();
        return model.equals("LG-H850") || model.equals("LG-H860") || model.equals("LG-H860N") || model.equals("LG-F700L");
    }

    public static boolean isV20() {
        String model = getModel();
        return model.contains("LG-F800") || model.contains("LG-VS995") || model.contains("LG-H910") || model.contains("LG-LS997") || model.contains("LG-H918") || model.contains("LG-US996") || model.contains("LG-H915");
    }

    public static boolean isG6() {
        String model = getModel();
        return model.contains("LG-H870") || model.contains("LG-H871") || model.contains("LG-H872") || model.contains("LG-G600") || model.contains("LGM-G600") || model.contains("LG-US997") || model.contains("LG-LS993") || model.contains("LG-AS993") || model.contains("LG-VS998");
    }

    public static boolean isV30() {
        String model = getModel();
        return model.contains("LG-H930") || model.contains("LG-H931") || model.contains("LG-H933") || model.contains("LG-V300") || model.contains("LG-LS998") || model.contains("LG-VS996") || model.contains("LG-VS996") || model.contains("LG-H932") || model.contains("LG-US998");
    }

    public static boolean isXperiaZ5() {
        String model = getModel();
        return model.equals("E6653") || model.equals("E6683");
    }

    public static boolean isHuaweiP9() {
        String model = getModel();
        return model.equals("EVA-TL00") || model.equals("EVA-L09");
    }

    public static boolean isGalaxyS7() {
        String model = getModel();
        return model.startsWith("SM-G93") || model.equals("SC-02H");
    }

    public static boolean isSonyHinoki() {
        return getModel().equals("G3121");
    }

    public static boolean isSonyRedwood() {
        return getModel().equals("G3221");
    }

    public static boolean isPixel2() {
        return getModel().equals("Pixel 2");
    }
}
