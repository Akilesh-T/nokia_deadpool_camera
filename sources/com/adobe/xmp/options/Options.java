package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;
import java.util.HashMap;
import java.util.Map;

public abstract class Options {
    private Map optionNames = null;
    private int options = 0;

    public abstract String defineOptionName(int i);

    public abstract int getValidOptions();

    public Options(int options) throws XMPException {
        assertOptionsValid(options);
        setOptions(options);
    }

    public void clear() {
        this.options = 0;
    }

    public boolean isExactly(int optionBits) {
        return getOptions() == optionBits;
    }

    public boolean containsAllOptions(int optionBits) {
        return (getOptions() & optionBits) == optionBits;
    }

    public boolean containsOneOf(int optionBits) {
        return (getOptions() & optionBits) != 0;
    }

    /* Access modifiers changed, original: protected */
    public boolean getOption(int optionBit) {
        return (this.options & optionBit) != 0;
    }

    public void setOption(int optionBits, boolean value) {
        this.options = value ? this.options | optionBits : this.options & (~optionBits);
    }

    public int getOptions() {
        return this.options;
    }

    public void setOptions(int options) throws XMPException {
        assertOptionsValid(options);
        this.options = options;
    }

    public boolean equals(Object obj) {
        return getOptions() == ((Options) obj).getOptions();
    }

    public int hashCode() {
        return getOptions();
    }

    public String getOptionsString() {
        if (this.options == 0) {
            return "<none>";
        }
        StringBuffer sb = new StringBuffer();
        int theBits = this.options;
        while (theBits != 0) {
            int oneLessBit = (theBits - 1) & theBits;
            sb.append(getOptionName(theBits ^ oneLessBit));
            if (oneLessBit != 0) {
                sb.append(" | ");
            }
            theBits = oneLessBit;
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(this.options));
        return stringBuilder.toString();
    }

    /* Access modifiers changed, original: protected */
    public void assertConsistency(int options) throws XMPException {
    }

    private void assertOptionsValid(int options) throws XMPException {
        int invalidOptions = (~getValidOptions()) & options;
        if (invalidOptions == 0) {
            assertConsistency(options);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The option bit(s) 0x");
        stringBuilder.append(Integer.toHexString(invalidOptions));
        stringBuilder.append(" are invalid!");
        throw new XMPException(stringBuilder.toString(), 103);
    }

    private String getOptionName(int option) {
        Map optionsNames = procureOptionNames();
        Integer key = new Integer(option);
        String result = (String) optionsNames.get(key);
        if (result != null) {
            return result;
        }
        result = defineOptionName(option);
        if (result == null) {
            return "<option name not defined>";
        }
        optionsNames.put(key, result);
        return result;
    }

    private Map procureOptionNames() {
        if (this.optionNames == null) {
            this.optionNames = new HashMap();
        }
        return this.optionNames;
    }
}
