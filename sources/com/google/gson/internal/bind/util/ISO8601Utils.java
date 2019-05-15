package com.google.gson.internal.bind.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class ISO8601Utils {
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);
    private static final String UTC_ID = "UTC";

    public static String format(Date date) {
        return format(date, false, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis) {
        return format(date, millis, TIMEZONE_UTC);
    }

    public static String format(Date date, boolean millis, TimeZone tz) {
        Calendar calendar = new GregorianCalendar(tz, Locale.US);
        calendar.setTime(date);
        StringBuilder formatted = new StringBuilder(("yyyy-MM-ddThh:mm:ss".length() + (millis ? ".sss".length() : 0)) + (tz.getRawOffset() == 0 ? "Z" : "+hh:mm").length());
        padInt(formatted, calendar.get(1), "yyyy".length());
        char c = '-';
        formatted.append('-');
        padInt(formatted, calendar.get(2) + 1, "MM".length());
        formatted.append('-');
        padInt(formatted, calendar.get(5), "dd".length());
        formatted.append('T');
        padInt(formatted, calendar.get(11), "hh".length());
        formatted.append(':');
        padInt(formatted, calendar.get(12), "mm".length());
        formatted.append(':');
        padInt(formatted, calendar.get(13), "ss".length());
        if (millis) {
            formatted.append('.');
            padInt(formatted, calendar.get(14), "sss".length());
        }
        int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            int hours = Math.abs((offset / 60000) / 60);
            int minutes = Math.abs((offset / 60000) % 60);
            if (offset >= 0) {
                c = '+';
            }
            formatted.append(c);
            padInt(formatted, hours, "hh".length());
            formatted.append(':');
            padInt(formatted, minutes, "mm".length());
        } else {
            formatted.append('Z');
        }
        return formatted.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0219  */
    public static java.util.Date parse(java.lang.String r26, java.text.ParsePosition r27) throws java.text.ParseException {
        /*
        r1 = r26;
        r2 = r27;
        r3 = 0;
        r4 = r3;
        r0 = r27.getIndex();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r5 = r0 + 4;
        r0 = parseInt(r1, r0, r5);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r6 = 45;
        r7 = checkOffset(r1, r5, r6);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r7 == 0) goto L_0x001a;
    L_0x0018:
        r5 = r5 + 1;
    L_0x001a:
        r7 = r5 + 2;
        r5 = parseInt(r1, r5, r7);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r8 = checkOffset(r1, r7, r6);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r8 == 0) goto L_0x0028;
    L_0x0026:
        r7 = r7 + 1;
    L_0x0028:
        r8 = r7 + 2;
        r7 = parseInt(r1, r7, r8);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r9 = 0;
        r10 = 0;
        r11 = 0;
        r12 = 0;
        r13 = 84;
        r13 = checkOffset(r1, r8, r13);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r13 != 0) goto L_0x005e;
    L_0x003a:
        r14 = r26.length();	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r14 > r8) goto L_0x005e;
    L_0x0040:
        r6 = new java.util.GregorianCalendar;	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r14 = r5 + -1;
        r6.<init>(r0, r14, r7);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r2.setIndex(r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r14 = r6.getTime();	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        return r14;
    L_0x004f:
        r0 = move-exception;
        r22 = r4;
        goto L_0x0208;
    L_0x0054:
        r0 = move-exception;
        r22 = r4;
        goto L_0x020d;
    L_0x0059:
        r0 = move-exception;
        r22 = r4;
        goto L_0x0212;
    L_0x005e:
        r14 = 43;
        r15 = 90;
        if (r13 == 0) goto L_0x00d5;
    L_0x0064:
        r8 = r8 + 1;
        r3 = r8 + 2;
        r8 = parseInt(r1, r8, r3);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r9 = r8;
        r8 = 58;
        r17 = checkOffset(r1, r3, r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r17 == 0) goto L_0x0077;
    L_0x0075:
        r3 = r3 + 1;
    L_0x0077:
        r6 = r3 + 2;
        r3 = parseInt(r1, r3, r6);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r10 = r3;
        r3 = checkOffset(r1, r6, r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r3 == 0) goto L_0x0086;
    L_0x0084:
        r6 = r6 + 1;
    L_0x0086:
        r8 = r6;
        r3 = r26.length();	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r3 <= r8) goto L_0x00d5;
    L_0x008d:
        r3 = r1.charAt(r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r3 == r15) goto L_0x00d5;
    L_0x0093:
        if (r3 == r14) goto L_0x00d5;
    L_0x0095:
        r6 = 45;
        if (r3 == r6) goto L_0x00d5;
    L_0x0099:
        r6 = r8 + 2;
        r8 = parseInt(r1, r8, r6);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r11 = 59;
        if (r8 <= r11) goto L_0x00a9;
    L_0x00a3:
        r11 = 63;
        if (r8 >= r11) goto L_0x00a9;
    L_0x00a7:
        r8 = 59;
    L_0x00a9:
        r11 = r8;
        r8 = 46;
        r8 = checkOffset(r1, r6, r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        if (r8 == 0) goto L_0x00d4;
    L_0x00b2:
        r6 = r6 + 1;
        r8 = r6 + 1;
        r8 = indexOfNonDigit(r1, r8);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r14 = r6 + 3;
        r14 = java.lang.Math.min(r8, r14);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r17 = parseInt(r1, r6, r14);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r18 = r14 - r6;
        switch(r18) {
            case 1: goto L_0x00cf;
            case 2: goto L_0x00cc;
            default: goto L_0x00c9;
        };
    L_0x00c9:
        r12 = r17;
        goto L_0x00d2;
    L_0x00cc:
        r12 = r17 * 10;
        goto L_0x00d2;
    L_0x00cf:
        r12 = r17 * 100;
        goto L_0x00d5;
    L_0x00d4:
        r8 = r6;
    L_0x00d5:
        r3 = r26.length();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r3 <= r8) goto L_0x01f5;
    L_0x00db:
        r3 = 0;
        r6 = r1.charAt(r8);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14 = 1;
        if (r6 != r15) goto L_0x00ed;
    L_0x00e3:
        r15 = TIMEZONE_UTC;	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r3 = r15;
        r8 = r8 + r14;
        r22 = r4;
        r23 = r6;
        goto L_0x01c2;
    L_0x00ed:
        r15 = 43;
        if (r6 == r15) goto L_0x0116;
    L_0x00f1:
        r15 = 45;
        if (r6 != r15) goto L_0x00f8;
    L_0x00f5:
        r19 = r3;
        goto L_0x0118;
    L_0x00f8:
        r14 = new java.lang.IndexOutOfBoundsException;	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r15 = new java.lang.StringBuilder;	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r15.<init>();	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r19 = r3;
        r3 = "Invalid time zone indicator '";
        r15.append(r3);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r15.append(r6);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r3 = "'";
        r15.append(r3);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r3 = r15.toString();	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        r14.<init>(r3);	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
        throw r14;	 Catch:{ IndexOutOfBoundsException -> 0x0059, NumberFormatException -> 0x0054, IllegalArgumentException -> 0x004f }
    L_0x0116:
        r19 = r3;
    L_0x0118:
        r3 = r1.substring(r8);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r15 = r3.length();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14 = 5;
        if (r15 < r14) goto L_0x0125;
    L_0x0123:
        r14 = r3;
        goto L_0x0136;
    L_0x0125:
        r14 = new java.lang.StringBuilder;	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14.<init>();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14.append(r3);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r15 = "00";
        r14.append(r15);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14 = r14.toString();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
    L_0x0136:
        r3 = r14;
        r14 = r3.length();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r8 = r8 + r14;
        r14 = "+0000";
        r14 = r14.equals(r3);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r14 != 0) goto L_0x01b9;
    L_0x0144:
        r14 = "+00:00";
        r14 = r14.equals(r3);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r14 == 0) goto L_0x0154;
    L_0x014c:
        r21 = r3;
        r22 = r4;
        r23 = r6;
        goto L_0x01bf;
    L_0x0154:
        r14 = new java.lang.StringBuilder;	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14.<init>();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r15 = "GMT";
        r14.append(r15);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14.append(r3);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r14 = r14.toString();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r15 = java.util.TimeZone.getTimeZone(r14);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r17 = r15.getID();	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        r20 = r17;
        r21 = r3;
        r3 = r20;
        r17 = r3.equals(r14);	 Catch:{ IndexOutOfBoundsException -> 0x020f, NumberFormatException -> 0x020a, IllegalArgumentException -> 0x0205 }
        if (r17 != 0) goto L_0x01b3;
    L_0x0179:
        r22 = r4;
        r4 = ":";
        r23 = r6;
        r6 = "";
        r4 = r3.replace(r4, r6);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = r4.equals(r14);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        if (r6 == 0) goto L_0x018c;
    L_0x018b:
        goto L_0x01b7;
    L_0x018c:
        r6 = new java.lang.IndexOutOfBoundsException;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r24 = r3;
        r3 = new java.lang.StringBuilder;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r3.<init>();	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r25 = r4;
        r4 = "Mismatching time zone indicator: ";
        r3.append(r4);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r3.append(r14);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r4 = " given, resolves to ";
        r3.append(r4);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r4 = r15.getID();	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r3.append(r4);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r3 = r3.toString();	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6.<init>(r3);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        throw r6;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
    L_0x01b3:
        r22 = r4;
        r23 = r6;
    L_0x01b7:
        r3 = r15;
        goto L_0x01c1;
    L_0x01b9:
        r21 = r3;
        r22 = r4;
        r23 = r6;
    L_0x01bf:
        r3 = TIMEZONE_UTC;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
    L_0x01c2:
        r4 = new java.util.GregorianCalendar;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r4.<init>(r3);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 0;
        r4.setLenient(r6);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 1;
        r4.set(r6, r0);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = r5 + -1;
        r14 = 2;
        r4.set(r14, r6);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 5;
        r4.set(r6, r7);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 11;
        r4.set(r6, r9);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 12;
        r4.set(r6, r10);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 13;
        r4.set(r6, r11);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = 14;
        r4.set(r6, r12);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r2.setIndex(r8);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r6 = r4.getTime();	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        return r6;
    L_0x01f5:
        r22 = r4;
        r3 = new java.lang.IllegalArgumentException;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        r4 = "No time zone indicator";
        r3.<init>(r4);	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
        throw r3;	 Catch:{ IndexOutOfBoundsException -> 0x0203, NumberFormatException -> 0x0201, IllegalArgumentException | IndexOutOfBoundsException | NumberFormatException -> 0x01ff }
    L_0x01ff:
        r0 = move-exception;
        goto L_0x0208;
    L_0x0201:
        r0 = move-exception;
        goto L_0x020d;
    L_0x0203:
        r0 = move-exception;
        goto L_0x0212;
    L_0x0205:
        r0 = move-exception;
        r22 = r4;
        goto L_0x0214;
    L_0x020a:
        r0 = move-exception;
        r22 = r4;
        goto L_0x0213;
    L_0x020f:
        r0 = move-exception;
        r22 = r4;
    L_0x0214:
        if (r1 != 0) goto L_0x0219;
    L_0x0216:
        r16 = 0;
        goto L_0x0231;
    L_0x0219:
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = 34;
        r3.append(r4);
        r3.append(r1);
        r4 = "'";
        r3.append(r4);
        r3 = r3.toString();
        r16 = r3;
    L_0x0231:
        r3 = r16;
        r4 = r0.getMessage();
        if (r4 == 0) goto L_0x023f;
    L_0x0239:
        r5 = r4.isEmpty();
        if (r5 == 0) goto L_0x025d;
    L_0x023f:
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "(";
        r5.append(r6);
        r6 = r0.getClass();
        r6 = r6.getName();
        r5.append(r6);
        r6 = ")";
        r5.append(r6);
        r4 = r5.toString();
    L_0x025d:
        r5 = new java.text.ParseException;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Failed to parse date [";
        r6.append(r7);
        r6.append(r3);
        r7 = "]: ";
        r6.append(r7);
        r6.append(r4);
        r6 = r6.toString();
        r7 = r27.getIndex();
        r5.<init>(r6, r7);
        r5.initCause(r0);
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.gson.internal.bind.util.ISO8601Utils.parse(java.lang.String, java.text.ParsePosition):java.util.Date");
    }

    private static boolean checkOffset(String value, int offset, char expected) {
        return offset < value.length() && value.charAt(offset) == expected;
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x003c  */
    private static int parseInt(java.lang.String r6, int r7, int r8) throws java.lang.NumberFormatException {
        /*
        if (r7 < 0) goto L_0x0069;
    L_0x0002:
        r0 = r6.length();
        if (r8 > r0) goto L_0x0069;
    L_0x0008:
        if (r7 > r8) goto L_0x0069;
    L_0x000a:
        r0 = r7;
        r1 = 0;
        r2 = 10;
        if (r0 >= r8) goto L_0x003a;
    L_0x0010:
        r3 = r0 + 1;
        r0 = r6.charAt(r0);
        r0 = java.lang.Character.digit(r0, r2);
        if (r0 < 0) goto L_0x001f;
    L_0x001c:
        r1 = -r0;
    L_0x001d:
        r0 = r3;
        goto L_0x003a;
    L_0x001f:
        r2 = new java.lang.NumberFormatException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Invalid number: ";
        r4.append(r5);
        r5 = r6.substring(r7, r8);
        r4.append(r5);
        r4 = r4.toString();
        r2.<init>(r4);
        throw r2;
    L_0x003a:
        if (r0 >= r8) goto L_0x0067;
    L_0x003c:
        r3 = r0 + 1;
        r0 = r6.charAt(r0);
        r0 = java.lang.Character.digit(r0, r2);
        if (r0 < 0) goto L_0x004c;
    L_0x0048:
        r1 = r1 * 10;
        r1 = r1 - r0;
        goto L_0x001d;
    L_0x004c:
        r2 = new java.lang.NumberFormatException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Invalid number: ";
        r4.append(r5);
        r5 = r6.substring(r7, r8);
        r4.append(r5);
        r4 = r4.toString();
        r2.<init>(r4);
        throw r2;
    L_0x0067:
        r2 = -r1;
        return r2;
    L_0x0069:
        r0 = new java.lang.NumberFormatException;
        r0.<init>(r6);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.gson.internal.bind.util.ISO8601Utils.parseInt(java.lang.String, int, int):int");
    }

    private static void padInt(StringBuilder buffer, int value, int length) {
        String strValue = Integer.toString(value);
        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);
    }

    private static int indexOfNonDigit(String string, int offset) {
        for (int i = offset; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return i;
            }
        }
        return string.length();
    }
}
