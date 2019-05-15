package com.adobe.xmp.options;

public final class ParseOptions extends Options {
    public static final int ACCEPT_LATIN_1 = 16;
    public static final int FIX_CONTROL_CHARS = 8;
    public static final int OMIT_NORMALIZATION = 32;
    public static final int REQUIRE_XMP_META = 1;
    public static final int STRICT_ALIASING = 4;

    public ParseOptions() {
        setOption(24, true);
    }

    public boolean getRequireXMPMeta() {
        return getOption(1);
    }

    public ParseOptions setRequireXMPMeta(boolean value) {
        setOption(1, value);
        return this;
    }

    public boolean getStrictAliasing() {
        return getOption(4);
    }

    public ParseOptions setStrictAliasing(boolean value) {
        setOption(4, value);
        return this;
    }

    public boolean getFixControlChars() {
        return getOption(8);
    }

    public ParseOptions setFixControlChars(boolean value) {
        setOption(8, value);
        return this;
    }

    public boolean getAcceptLatin1() {
        return getOption(16);
    }

    public ParseOptions setOmitNormalization(boolean value) {
        setOption(32, value);
        return this;
    }

    public boolean getOmitNormalization() {
        return getOption(32);
    }

    public ParseOptions setAcceptLatin1(boolean value) {
        setOption(16, value);
        return this;
    }

    /* Access modifiers changed, original: protected */
    public String defineOptionName(int option) {
        if (option == 1) {
            return "REQUIRE_XMP_META";
        }
        if (option == 4) {
            return "STRICT_ALIASING";
        }
        if (option == 8) {
            return "FIX_CONTROL_CHARS";
        }
        if (option == 16) {
            return "ACCEPT_LATIN_1";
        }
        if (option != 32) {
            return null;
        }
        return "OMIT_NORMALIZATION";
    }

    /* Access modifiers changed, original: protected */
    public int getValidOptions() {
        return 61;
    }
}
