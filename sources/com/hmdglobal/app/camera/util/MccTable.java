package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.support.v4.media.MediaPlayer2;
import android.support.v4.view.InputDeviceCompat;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.text.TextUtils;
import android.util.Log;
import com.android.ex.camera2.portability.CameraActions;
import com.android.internal.app.LocaleStore;
import com.hmdglobal.app.camera.ButtonManager;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.provider.InfoTable;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import libcore.icu.ICU;

public final class MccTable {
    private static final Map<Locale, Locale> FALLBACKS = new HashMap();
    static final String LOG_TAG = "MccTable";
    static ArrayList<MccEntry> sTable = new ArrayList(240);

    static class MccEntry implements Comparable<MccEntry> {
        final String mIso;
        final int mMcc;
        final int mSmallestDigitsMnc;

        MccEntry(int mnc, String iso, int smallestDigitsMCC) {
            if (iso != null) {
                this.mMcc = mnc;
                this.mIso = iso;
                this.mSmallestDigitsMnc = smallestDigitsMCC;
                return;
            }
            throw new NullPointerException();
        }

        public int compareTo(MccEntry o) {
            return this.mMcc - o.mMcc;
        }
    }

    private static MccEntry entryForMcc(int mcc) {
        int index = Collections.binarySearch(sTable, new MccEntry(mcc, "", 0));
        if (index < 0) {
            return null;
        }
        return (MccEntry) sTable.get(index);
    }

    public static String countryCodeForMcc(int mcc) {
        MccEntry entry = entryForMcc(mcc);
        if (entry == null) {
            return "";
        }
        return entry.mIso;
    }

    public static String defaultLanguageForMcc(int mcc) {
        MccEntry entry = entryForMcc(mcc);
        String str;
        if (entry == null) {
            str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("defaultLanguageForMcc(");
            stringBuilder.append(mcc);
            stringBuilder.append("): no country for mcc");
            Log.d(str, stringBuilder.toString());
            return null;
        }
        str = entry.mIso;
        if ("in".equals(str)) {
            return "en";
        }
        String likelyLanguage = ICU.addLikelySubtags(new Locale("und", str)).getLanguage();
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("defaultLanguageForMcc(");
        stringBuilder2.append(mcc);
        stringBuilder2.append("): country ");
        stringBuilder2.append(str);
        stringBuilder2.append(" uses ");
        stringBuilder2.append(likelyLanguage);
        Log.d(str2, stringBuilder2.toString());
        return likelyLanguage;
    }

    static {
        FALLBACKS.put(Locale.ENGLISH, Locale.US);
        sTable.add(new MccEntry(202, "gr", 2));
        sTable.add(new MccEntry(204, "nl", 2));
        sTable.add(new MccEntry(206, "be", 2));
        sTable.add(new MccEntry(208, "fr", 2));
        sTable.add(new MccEntry(212, "mc", 2));
        sTable.add(new MccEntry(213, "ad", 2));
        sTable.add(new MccEntry(214, "es", 2));
        sTable.add(new MccEntry(216, "hu", 2));
        sTable.add(new MccEntry(218, "ba", 2));
        sTable.add(new MccEntry(219, "hr", 2));
        sTable.add(new MccEntry(ButtonManager.MASK_TOTAL_HEIGHT, "rs", 2));
        sTable.add(new MccEntry(222, "it", 2));
        sTable.add(new MccEntry(225, "va", 2));
        sTable.add(new MccEntry(226, "ro", 2));
        sTable.add(new MccEntry(228, "ch", 2));
        sTable.add(new MccEntry(230, "cz", 2));
        sTable.add(new MccEntry(231, "sk", 2));
        sTable.add(new MccEntry(232, "at", 2));
        sTable.add(new MccEntry(234, "gb", 2));
        sTable.add(new MccEntry(235, "gb", 2));
        sTable.add(new MccEntry(238, "dk", 2));
        sTable.add(new MccEntry(240, "se", 2));
        sTable.add(new MccEntry(242, "no", 2));
        sTable.add(new MccEntry(244, "fi", 2));
        sTable.add(new MccEntry(246, "lt", 2));
        sTable.add(new MccEntry(247, "lv", 2));
        sTable.add(new MccEntry(248, "ee", 2));
        sTable.add(new MccEntry(Callback.DEFAULT_SWIPE_ANIMATION_DURATION, "ru", 2));
        sTable.add(new MccEntry(255, "ua", 2));
        sTable.add(new MccEntry(InputDeviceCompat.SOURCE_KEYBOARD, "by", 2));
        sTable.add(new MccEntry(259, "md", 2));
        sTable.add(new MccEntry(260, "pl", 2));
        sTable.add(new MccEntry(262, "de", 2));
        sTable.add(new MccEntry(266, "gi", 2));
        sTable.add(new MccEntry(268, "pt", 2));
        sTable.add(new MccEntry(MediaProviderUtils.ROTATION_270, "lu", 2));
        sTable.add(new MccEntry(272, "ie", 2));
        sTable.add(new MccEntry(274, "is", 2));
        sTable.add(new MccEntry(CameraUtil.BOOM_KEY, "al", 2));
        sTable.add(new MccEntry(278, "mt", 2));
        sTable.add(new MccEntry(280, "cy", 2));
        sTable.add(new MccEntry(282, "ge", 2));
        sTable.add(new MccEntry(283, "am", 2));
        sTable.add(new MccEntry(284, "bg", 2));
        sTable.add(new MccEntry(286, "tr", 2));
        sTable.add(new MccEntry(288, "fo", 2));
        sTable.add(new MccEntry(289, "ge", 2));
        sTable.add(new MccEntry(290, "gl", 2));
        sTable.add(new MccEntry(292, "sm", 2));
        sTable.add(new MccEntry(293, "si", 2));
        sTable.add(new MccEntry(294, "mk", 2));
        sTable.add(new MccEntry(295, "li", 2));
        sTable.add(new MccEntry(297, "me", 2));
        sTable.add(new MccEntry(CameraActions.CANCEL_AUTO_FOCUS, "ca", 3));
        sTable.add(new MccEntry(308, "pm", 2));
        sTable.add(new MccEntry(310, "us", 3));
        sTable.add(new MccEntry(311, "us", 3));
        sTable.add(new MccEntry(312, "us", 3));
        sTable.add(new MccEntry(313, "us", 3));
        sTable.add(new MccEntry(314, "us", 3));
        sTable.add(new MccEntry(315, "us", 3));
        sTable.add(new MccEntry(316, "us", 3));
        sTable.add(new MccEntry(330, "pr", 2));
        sTable.add(new MccEntry(332, "vi", 2));
        sTable.add(new MccEntry(334, "mx", 3));
        sTable.add(new MccEntry(338, "jm", 3));
        sTable.add(new MccEntry(340, "gp", 2));
        sTable.add(new MccEntry(342, "bb", 3));
        sTable.add(new MccEntry(344, "ag", 3));
        sTable.add(new MccEntry(346, "ky", 3));
        sTable.add(new MccEntry(348, "vg", 3));
        sTable.add(new MccEntry(350, "bm", 2));
        sTable.add(new MccEntry(352, "gd", 2));
        sTable.add(new MccEntry(354, "ms", 2));
        sTable.add(new MccEntry(356, "kn", 2));
        sTable.add(new MccEntry(358, "lc", 2));
        sTable.add(new MccEntry(360, "vc", 2));
        sTable.add(new MccEntry(362, "ai", 2));
        sTable.add(new MccEntry(363, "aw", 2));
        sTable.add(new MccEntry(364, "bs", 2));
        sTable.add(new MccEntry(365, "ai", 3));
        sTable.add(new MccEntry(366, "dm", 2));
        sTable.add(new MccEntry(368, "cu", 2));
        sTable.add(new MccEntry(370, "do", 2));
        sTable.add(new MccEntry(372, "ht", 2));
        sTable.add(new MccEntry(374, "tt", 2));
        sTable.add(new MccEntry(376, "tc", 2));
        sTable.add(new MccEntry(400, "az", 2));
        sTable.add(new MccEntry(401, "kz", 2));
        sTable.add(new MccEntry(402, "bt", 2));
        sTable.add(new MccEntry(404, "in", 2));
        sTable.add(new MccEntry(405, "in", 2));
        sTable.add(new MccEntry(406, "in", 2));
        sTable.add(new MccEntry(410, "pk", 2));
        sTable.add(new MccEntry(412, "af", 2));
        sTable.add(new MccEntry(413, "lk", 2));
        sTable.add(new MccEntry(414, "mm", 2));
        sTable.add(new MccEntry(415, "lb", 2));
        sTable.add(new MccEntry(416, "jo", 2));
        sTable.add(new MccEntry(417, "sy", 2));
        sTable.add(new MccEntry(418, "iq", 2));
        sTable.add(new MccEntry(419, "kw", 2));
        sTable.add(new MccEntry(420, "sa", 2));
        sTable.add(new MccEntry(421, "ye", 2));
        sTable.add(new MccEntry(422, "om", 2));
        sTable.add(new MccEntry(423, "ps", 2));
        sTable.add(new MccEntry(424, "ae", 2));
        sTable.add(new MccEntry(425, "il", 2));
        sTable.add(new MccEntry(426, "bh", 2));
        sTable.add(new MccEntry(427, "qa", 2));
        sTable.add(new MccEntry(428, "mn", 2));
        sTable.add(new MccEntry(429, "np", 2));
        sTable.add(new MccEntry(430, "ae", 2));
        sTable.add(new MccEntry(431, "ae", 2));
        sTable.add(new MccEntry(432, "ir", 2));
        sTable.add(new MccEntry(434, "uz", 2));
        sTable.add(new MccEntry(436, "tj", 2));
        sTable.add(new MccEntry(437, "kg", 2));
        sTable.add(new MccEntry(438, "tm", 2));
        sTable.add(new MccEntry(440, "jp", 2));
        sTable.add(new MccEntry(441, "jp", 2));
        sTable.add(new MccEntry(450, "kr", 2));
        sTable.add(new MccEntry(452, "vn", 2));
        sTable.add(new MccEntry(454, "hk", 2));
        sTable.add(new MccEntry(455, "mo", 2));
        sTable.add(new MccEntry(456, "kh", 2));
        sTable.add(new MccEntry(457, "la", 2));
        sTable.add(new MccEntry(460, "cn", 2));
        sTable.add(new MccEntry(CameraActions.SET_FACE_DETECTION_LISTENER, "cn", 2));
        sTable.add(new MccEntry(466, "tw", 2));
        sTable.add(new MccEntry(467, "kp", 2));
        sTable.add(new MccEntry(470, "bd", 2));
        sTable.add(new MccEntry(472, "mv", 2));
        sTable.add(new MccEntry(CameraActions.SET_DISPLAY_ORIENTATION, "my", 2));
        sTable.add(new MccEntry(505, "au", 2));
        sTable.add(new MccEntry(510, InfoTable.ID, 2));
        sTable.add(new MccEntry(514, "tl", 2));
        sTable.add(new MccEntry(515, "ph", 2));
        sTable.add(new MccEntry(520, "th", 2));
        sTable.add(new MccEntry(525, "sg", 2));
        sTable.add(new MccEntry(528, "bn", 2));
        sTable.add(new MccEntry(530, "nz", 2));
        sTable.add(new MccEntry(534, "mp", 2));
        sTable.add(new MccEntry(535, "gu", 2));
        sTable.add(new MccEntry(536, "nr", 2));
        sTable.add(new MccEntry(537, "pg", 2));
        sTable.add(new MccEntry(539, "to", 2));
        sTable.add(new MccEntry(540, "sb", 2));
        sTable.add(new MccEntry(541, "vu", 2));
        sTable.add(new MccEntry(542, "fj", 2));
        sTable.add(new MccEntry(543, "wf", 2));
        sTable.add(new MccEntry(544, "as", 2));
        sTable.add(new MccEntry(545, "ki", 2));
        sTable.add(new MccEntry(546, "nc", 2));
        sTable.add(new MccEntry(547, "pf", 2));
        sTable.add(new MccEntry(548, "ck", 2));
        sTable.add(new MccEntry(549, "ws", 2));
        sTable.add(new MccEntry(550, "fm", 2));
        sTable.add(new MccEntry(551, "mh", 2));
        sTable.add(new MccEntry(552, "pw", 2));
        sTable.add(new MccEntry(553, "tv", 2));
        sTable.add(new MccEntry(555, "nu", 2));
        sTable.add(new MccEntry(CameraActions.BURST_SHOT, "eg", 2));
        sTable.add(new MccEntry(CameraActions.ABORT_SHOT, "dz", 2));
        sTable.add(new MccEntry(CameraActions.PRE_ALLOC_BURST_SHOT, "ma", 2));
        sTable.add(new MccEntry(CameraActions.ABORT_PRE_ALLOC_BURST_SHOT, "tn", 2));
        sTable.add(new MccEntry(606, "ly", 2));
        sTable.add(new MccEntry(607, "gm", 2));
        sTable.add(new MccEntry(MotionPictureHelper.CROP_FRAME_HEIGHT_9, "sn", 2));
        sTable.add(new MccEntry(609, "mr", 2));
        sTable.add(new MccEntry(610, "ml", 2));
        sTable.add(new MccEntry(611, "gn", 2));
        sTable.add(new MccEntry(612, "ci", 2));
        sTable.add(new MccEntry(613, "bf", 2));
        sTable.add(new MccEntry(614, "ne", 2));
        sTable.add(new MccEntry(615, "tg", 2));
        sTable.add(new MccEntry(616, "bj", 2));
        sTable.add(new MccEntry(617, "mu", 2));
        sTable.add(new MccEntry(618, "lr", 2));
        sTable.add(new MccEntry(619, "sl", 2));
        sTable.add(new MccEntry(620, "gh", 2));
        sTable.add(new MccEntry(621, "ng", 2));
        sTable.add(new MccEntry(622, "td", 2));
        sTable.add(new MccEntry(623, "cf", 2));
        sTable.add(new MccEntry(624, "cm", 2));
        sTable.add(new MccEntry(625, "cv", 2));
        sTable.add(new MccEntry(626, "st", 2));
        sTable.add(new MccEntry(627, "gq", 2));
        sTable.add(new MccEntry(628, "ga", 2));
        sTable.add(new MccEntry(629, "cg", 2));
        sTable.add(new MccEntry(630, "cd", 2));
        sTable.add(new MccEntry(631, "ao", 2));
        sTable.add(new MccEntry(632, "gw", 2));
        sTable.add(new MccEntry(633, "sc", 2));
        sTable.add(new MccEntry(634, "sd", 2));
        sTable.add(new MccEntry(635, "rw", 2));
        sTable.add(new MccEntry(636, "et", 2));
        sTable.add(new MccEntry(637, "so", 2));
        sTable.add(new MccEntry(638, "dj", 2));
        sTable.add(new MccEntry(639, "ke", 2));
        sTable.add(new MccEntry(MotionPictureHelper.FRAME_WIDTH_4, "tz", 2));
        sTable.add(new MccEntry(641, "ug", 2));
        sTable.add(new MccEntry(642, "bi", 2));
        sTable.add(new MccEntry(643, "mz", 2));
        sTable.add(new MccEntry(645, "zm", 2));
        sTable.add(new MccEntry(646, "mg", 2));
        sTable.add(new MccEntry(647, "re", 2));
        sTable.add(new MccEntry(648, "zw", 2));
        sTable.add(new MccEntry(649, "na", 2));
        sTable.add(new MccEntry(650, "mw", 2));
        sTable.add(new MccEntry(651, "ls", 2));
        sTable.add(new MccEntry(652, "bw", 2));
        sTable.add(new MccEntry(653, "sz", 2));
        sTable.add(new MccEntry(654, "km", 2));
        sTable.add(new MccEntry(655, "za", 2));
        sTable.add(new MccEntry(657, "er", 2));
        sTable.add(new MccEntry(658, "sh", 2));
        sTable.add(new MccEntry(659, "ss", 2));
        sTable.add(new MccEntry(702, "bz", 2));
        sTable.add(new MccEntry(MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, "gt", 2));
        sTable.add(new MccEntry(706, "sv", 2));
        sTable.add(new MccEntry(708, "hn", 3));
        sTable.add(new MccEntry(710, "ni", 2));
        sTable.add(new MccEntry(712, "cr", 2));
        sTable.add(new MccEntry(714, "pa", 2));
        sTable.add(new MccEntry(716, "pe", 2));
        sTable.add(new MccEntry(722, "ar", 3));
        sTable.add(new MccEntry(724, "br", 2));
        sTable.add(new MccEntry(730, "cl", 2));
        sTable.add(new MccEntry(732, "co", 3));
        sTable.add(new MccEntry(734, "ve", 2));
        sTable.add(new MccEntry(736, "bo", 2));
        sTable.add(new MccEntry(738, "gy", 2));
        sTable.add(new MccEntry(740, "ec", 2));
        sTable.add(new MccEntry(742, "gf", 2));
        sTable.add(new MccEntry(744, "py", 2));
        sTable.add(new MccEntry(746, "sr", 2));
        sTable.add(new MccEntry(748, "uy", 2));
        sTable.add(new MccEntry(750, "fk", 2));
        Collections.sort(sTable);
    }

    private static Locale lookupFallback(Locale target, List<Locale> candidates) {
        Locale fallback = target;
        do {
            Locale locale = (Locale) FALLBACKS.get(fallback);
            fallback = locale;
            if (locale == null) {
                return null;
            }
        } while (!candidates.contains(fallback));
        return fallback;
    }

    private static Locale getLocaleForLanguageCountry(Context context, String language, String country) {
        if (language == null) {
            Log.d(LOG_TAG, "getLocaleForLanguageCountry: skipping no language");
            return null;
        }
        if (country == null) {
            country = "";
        }
        Locale target = new Locale(language, country);
        try {
            String locale;
            String str;
            StringBuilder stringBuilder;
            List<String> locales = new ArrayList(Arrays.asList(context.getAssets().getLocales()));
            locales.remove("ar-XB");
            locales.remove("en-XA");
            List<Locale> languageMatches = new ArrayList();
            for (String locale2 : locales) {
                Locale l = Locale.forLanguageTag(locale2.replace('_', '-'));
                if (!(l == null || "und".equals(l.getLanguage()) || l.getLanguage().isEmpty())) {
                    if (!l.getCountry().isEmpty()) {
                        if (l.getLanguage().equals(target.getLanguage())) {
                            if (l.getCountry().equals(target.getCountry())) {
                                str = LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("getLocaleForLanguageCountry: got perfect match: ");
                                stringBuilder.append(l.toLanguageTag());
                                Log.d(str, stringBuilder.toString());
                                return l;
                            }
                            languageMatches.add(l);
                        }
                    }
                }
            }
            if (languageMatches.isEmpty()) {
                str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getLocaleForLanguageCountry: no locales for language ");
                stringBuilder2.append(language);
                Log.d(str, stringBuilder2.toString());
                return null;
            }
            Locale bestMatch = lookupFallback(target, languageMatches);
            StringBuilder stringBuilder3;
            if (bestMatch != null) {
                locale2 = LOG_TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getLocaleForLanguageCountry: got a fallback match: ");
                stringBuilder3.append(bestMatch.toLanguageTag());
                Log.d(locale2, stringBuilder3.toString());
                return bestMatch;
            }
            if (!TextUtils.isEmpty(target.getCountry())) {
                LocaleStore.fillCache(context);
                if (LocaleStore.getLocaleInfo(target).isTranslated()) {
                    String str2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getLocaleForLanguageCountry: target locale is translated: ");
                    stringBuilder.append(target);
                    Log.d(str2, stringBuilder.toString());
                    return target;
                }
            }
            locale2 = LOG_TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getLocaleForLanguageCountry: got language-only match: ");
            stringBuilder3.append(language);
            Log.d(locale2, stringBuilder3.toString());
            return (Locale) languageMatches.get(0);
        } catch (Exception e) {
            Log.d(LOG_TAG, "getLocaleForLanguageCountry: exception", e);
            return null;
        }
    }

    public static Locale getLocaleFromMcc(Context context, int mcc, String simLanguage) {
        boolean hasSimLanguage = TextUtils.isEmpty(simLanguage) ^ 1;
        String language = hasSimLanguage ? simLanguage : defaultLanguageForMcc(mcc);
        String country = countryCodeForMcc(mcc);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLocaleFromMcc(");
        stringBuilder.append(language);
        stringBuilder.append(", ");
        stringBuilder.append(country);
        stringBuilder.append(", ");
        stringBuilder.append(mcc);
        Log.d(str, stringBuilder.toString());
        Locale locale = getLocaleForLanguageCountry(context, language, country);
        if (locale != null || !hasSimLanguage) {
            return locale;
        }
        language = defaultLanguageForMcc(mcc);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[retry ] getLocaleFromMcc(");
        stringBuilder2.append(language);
        stringBuilder2.append(", ");
        stringBuilder2.append(country);
        stringBuilder2.append(", ");
        stringBuilder2.append(mcc);
        Log.d(str2, stringBuilder2.toString());
        return getLocaleForLanguageCountry(context, language, country);
    }
}
