package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;
import com.morphoinc.app.panoramagp3.UnsharpStrengthPreference;

public final class AliasOptions extends Options {
    public static final int PROP_ARRAY = 512;
    public static final int PROP_ARRAY_ALTERNATE = 2048;
    public static final int PROP_ARRAY_ALT_TEXT = 4096;
    public static final int PROP_ARRAY_ORDERED = 1024;
    public static final int PROP_DIRECT = 0;

    public AliasOptions(int options) throws XMPException {
        super(options);
    }

    public boolean isSimple() {
        return getOptions() == 0;
    }

    public boolean isArray() {
        return getOption(512);
    }

    public AliasOptions setArray(boolean value) {
        setOption(512, value);
        return this;
    }

    public boolean isArrayOrdered() {
        return getOption(1024);
    }

    public AliasOptions setArrayOrdered(boolean value) {
        setOption(UnsharpStrengthPreference.DEFAULT_VALUE, value);
        return this;
    }

    public boolean isArrayAlternate() {
        return getOption(2048);
    }

    public AliasOptions setArrayAlternate(boolean value) {
        setOption(3584, value);
        return this;
    }

    public boolean isArrayAltText() {
        return getOption(4096);
    }

    public AliasOptions setArrayAltText(boolean value) {
        setOption(7680, value);
        return this;
    }

    public PropertyOptions toPropertyOptions() throws XMPException {
        return new PropertyOptions(getOptions());
    }

    /* Access modifiers changed, original: protected */
    public String defineOptionName(int option) {
        if (option == 0) {
            return "PROP_DIRECT";
        }
        if (option == 512) {
            return "ARRAY";
        }
        if (option == 1024) {
            return "ARRAY_ORDERED";
        }
        if (option == 2048) {
            return "ARRAY_ALTERNATE";
        }
        if (option != 4096) {
            return null;
        }
        return "ARRAY_ALT_TEXT";
    }

    /* Access modifiers changed, original: protected */
    public int getValidOptions() {
        return 7680;
    }
}
