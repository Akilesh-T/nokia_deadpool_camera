package com.adobe.xmp.options;

public final class IteratorOptions extends Options {
    public static final int INCLUDE_ALIASES = 2048;
    public static final int JUST_CHILDREN = 256;
    public static final int JUST_LEAFNAME = 1024;
    public static final int JUST_LEAFNODES = 512;
    public static final int OMIT_QUALIFIERS = 4096;

    public boolean isJustChildren() {
        return getOption(256);
    }

    public boolean isJustLeafname() {
        return getOption(1024);
    }

    public boolean isJustLeafnodes() {
        return getOption(512);
    }

    public boolean isOmitQualifiers() {
        return getOption(4096);
    }

    public IteratorOptions setJustChildren(boolean value) {
        setOption(256, value);
        return this;
    }

    public IteratorOptions setJustLeafname(boolean value) {
        setOption(1024, value);
        return this;
    }

    public IteratorOptions setJustLeafnodes(boolean value) {
        setOption(512, value);
        return this;
    }

    public IteratorOptions setOmitQualifiers(boolean value) {
        setOption(4096, value);
        return this;
    }

    /* Access modifiers changed, original: protected */
    public String defineOptionName(int option) {
        if (option == 256) {
            return "JUST_CHILDREN";
        }
        if (option == 512) {
            return "JUST_LEAFNODES";
        }
        if (option == 1024) {
            return "JUST_LEAFNAME";
        }
        if (option != 4096) {
            return null;
        }
        return "OMIT_QUALIFIERS";
    }

    /* Access modifiers changed, original: protected */
    public int getValidOptions() {
        return 5888;
    }
}
