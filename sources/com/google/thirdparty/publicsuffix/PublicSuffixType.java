package com.google.thirdparty.publicsuffix;

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
enum PublicSuffixType {
    PRIVATE(':', ','),
    ICANN('!', '?');
    
    private final char innerNodeCode;
    private final char leafNodeCode;

    private PublicSuffixType(char innerNodeCode, char leafNodeCode) {
        this.innerNodeCode = innerNodeCode;
        this.leafNodeCode = leafNodeCode;
    }

    /* Access modifiers changed, original: 0000 */
    public char getLeafNodeCode() {
        return this.leafNodeCode;
    }

    /* Access modifiers changed, original: 0000 */
    public char getInnerNodeCode() {
        return this.innerNodeCode;
    }

    static PublicSuffixType fromCode(char code) {
        for (PublicSuffixType value : values()) {
            if (value.getInnerNodeCode() == code || value.getLeafNodeCode() == code) {
                return value;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No enum corresponding to given code: ");
        stringBuilder.append(code);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static PublicSuffixType fromIsPrivate(boolean isPrivate) {
        return isPrivate ? PRIVATE : ICANN;
    }
}
