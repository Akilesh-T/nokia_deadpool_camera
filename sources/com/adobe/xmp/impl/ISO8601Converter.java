package com.adobe.xmp.impl;

import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SimpleTimeZone;

public final class ISO8601Converter {
    private ISO8601Converter() {
    }

    public static XMPDateTime parse(String iso8601String) throws XMPException {
        return parse(iso8601String, new XMPDateTimeImpl());
    }

    public static XMPDateTime parse(String iso8601String, XMPDateTime binValue) throws XMPException {
        int value;
        int value2;
        ParameterAsserts.assertNotNull(iso8601String);
        ParseState input = new ParseState(iso8601String);
        boolean timeOnly = input.ch(0) == 'T' || ((input.length() >= 2 && input.ch(1) == ':') || (input.length() >= 3 && input.ch(2) == ':'));
        if (timeOnly) {
            binValue.setMonth(1);
            binValue.setDay(1);
        } else {
            if (input.ch(0) == '-') {
                input.skip();
            }
            value = input.gatherInt("Invalid year in date string", 9999);
            if (!input.hasNext() || input.ch() == '-') {
                if (input.ch(0) == '-') {
                    value = -value;
                }
                binValue.setYear(value);
                if (!input.hasNext()) {
                    return binValue;
                }
                input.skip();
                value2 = input.gatherInt("Invalid month in date string", 12);
                if (!input.hasNext() || input.ch() == '-') {
                    binValue.setMonth(value2);
                    if (!input.hasNext()) {
                        return binValue;
                    }
                    input.skip();
                    value2 = input.gatherInt("Invalid day in date string", 31);
                    if (!input.hasNext() || input.ch() == 'T') {
                        binValue.setDay(value2);
                        if (!input.hasNext()) {
                            return binValue;
                        }
                    }
                    throw new XMPException("Invalid date string, after day", 5);
                }
                throw new XMPException("Invalid date string, after month", 5);
            }
            throw new XMPException("Invalid date string, after year", 5);
        }
        if (input.ch() == 'T') {
            input.skip();
        } else if (!timeOnly) {
            throw new XMPException("Invalid date string, missing 'T' after date", 5);
        }
        value2 = input.gatherInt("Invalid hour in date string", 23);
        if (input.ch() == ':') {
            binValue.setHour(value2);
            input.skip();
            value2 = input.gatherInt("Invalid minute in date string", 59);
            if (!input.hasNext() || input.ch() == ':' || input.ch() == 'Z' || input.ch() == '+' || input.ch() == '-') {
                int digits;
                binValue.setMinute(value2);
                if (input.ch() == ':') {
                    input.skip();
                    value2 = input.gatherInt("Invalid whole seconds in date string", 59);
                    if (!input.hasNext() || input.ch() == '.' || input.ch() == 'Z' || input.ch() == '+' || input.ch() == '-') {
                        binValue.setSecond(value2);
                        if (input.ch() == '.') {
                            input.skip();
                            value = input.pos();
                            value2 = input.gatherInt("Invalid fractional seconds in date string", 999999999);
                            if (input.ch() == 'Z' || input.ch() == '+' || input.ch() == '-') {
                                digits = input.pos() - value;
                                while (digits > 9) {
                                    value2 /= 10;
                                    digits--;
                                }
                                while (digits < 9) {
                                    value2 *= 10;
                                    digits++;
                                }
                                binValue.setNanoSecond(value2);
                            } else {
                                throw new XMPException("Invalid date string, after fractional second", 5);
                            }
                        }
                    }
                    throw new XMPException("Invalid date string, after whole seconds", 5);
                }
                value = 0;
                digits = 0;
                int tzMinute = 0;
                if (input.ch() == 'Z') {
                    input.skip();
                } else if (input.hasNext()) {
                    if (input.ch() == '+') {
                        value = 1;
                    } else if (input.ch() == '-') {
                        value = -1;
                    } else {
                        throw new XMPException("Time zone must begin with 'Z', '+', or '-'", 5);
                    }
                    input.skip();
                    digits = input.gatherInt("Invalid time zone hour in date string", 23);
                    if (input.ch() == ':') {
                        input.skip();
                        tzMinute = input.gatherInt("Invalid time zone minute in date string", 59);
                    } else {
                        throw new XMPException("Invalid date string, after time zone hour", 5);
                    }
                }
                binValue.setTimeZone(new SimpleTimeZone((((digits * 3600) * 1000) + ((tzMinute * 60) * 1000)) * value, ""));
                if (!input.hasNext()) {
                    return binValue;
                }
                throw new XMPException("Invalid date string, extra chars at end", 5);
            }
            throw new XMPException("Invalid date string, after minute", 5);
        }
        throw new XMPException("Invalid date string, after hour", 5);
    }

    public static String render(XMPDateTime dateTime) {
        StringBuffer buffer = new StringBuffer();
        DecimalFormat df = new DecimalFormat("0000", new DecimalFormatSymbols(Locale.ENGLISH));
        buffer.append(df.format((long) dateTime.getYear()));
        if (dateTime.getMonth() == 0) {
            return buffer.toString();
        }
        df.applyPattern("'-'00");
        buffer.append(df.format((long) dateTime.getMonth()));
        if (dateTime.getDay() == 0) {
            return buffer.toString();
        }
        buffer.append(df.format((long) dateTime.getDay()));
        if (!(dateTime.getHour() == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0 && dateTime.getNanoSecond() == 0 && (dateTime.getTimeZone() == null || dateTime.getTimeZone().getRawOffset() == 0))) {
            buffer.append('T');
            df.applyPattern("00");
            buffer.append(df.format((long) dateTime.getHour()));
            buffer.append(':');
            buffer.append(df.format((long) dateTime.getMinute()));
            if (!(dateTime.getSecond() == 0 && dateTime.getNanoSecond() == 0)) {
                double seconds = ((double) dateTime.getSecond()) + (((double) dateTime.getNanoSecond()) / 1.0E9d);
                df.applyPattern(":00.#########");
                buffer.append(df.format(seconds));
            }
            if (dateTime.getTimeZone() != null) {
                int offset = dateTime.getTimeZone().getOffset(dateTime.getCalendar().getTimeInMillis());
                if (offset == 0) {
                    buffer.append('Z');
                } else {
                    int thours = offset / 3600000;
                    int tminutes = Math.abs((offset % 3600000) / 60000);
                    df.applyPattern("+00;-00");
                    buffer.append(df.format((long) thours));
                    df.applyPattern(":00");
                    buffer.append(df.format((long) tminutes));
                }
            }
        }
        return buffer.toString();
    }
}
