package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import javax.annotation.Nullable;

@GwtCompatible
public enum CaseFormat {
    LOWER_HYPHEN(CharMatcher.is('-'), "-") {
        /* Access modifiers changed, original: 0000 */
        public String normalizeWord(String word) {
            return Ascii.toLowerCase(word);
        }

        /* Access modifiers changed, original: 0000 */
        public String convert(CaseFormat format, String s) {
            if (format == LOWER_UNDERSCORE) {
                return s.replace('-', '_');
            }
            if (format == UPPER_UNDERSCORE) {
                return Ascii.toUpperCase(s.replace('-', '_'));
            }
            return super.convert(format, s);
        }
    },
    LOWER_UNDERSCORE(CharMatcher.is('_'), "_") {
        /* Access modifiers changed, original: 0000 */
        public String normalizeWord(String word) {
            return Ascii.toLowerCase(word);
        }

        /* Access modifiers changed, original: 0000 */
        public String convert(CaseFormat format, String s) {
            if (format == LOWER_HYPHEN) {
                return s.replace('_', '-');
            }
            if (format == UPPER_UNDERSCORE) {
                return Ascii.toUpperCase(s);
            }
            return super.convert(format, s);
        }
    },
    LOWER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
        /* Access modifiers changed, original: 0000 */
        public String normalizeWord(String word) {
            return CaseFormat.firstCharOnlyToUpper(word);
        }
    },
    UPPER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
        /* Access modifiers changed, original: 0000 */
        public String normalizeWord(String word) {
            return CaseFormat.firstCharOnlyToUpper(word);
        }
    },
    UPPER_UNDERSCORE(CharMatcher.is('_'), "_") {
        /* Access modifiers changed, original: 0000 */
        public String normalizeWord(String word) {
            return Ascii.toUpperCase(word);
        }

        /* Access modifiers changed, original: 0000 */
        public String convert(CaseFormat format, String s) {
            if (format == LOWER_HYPHEN) {
                return Ascii.toLowerCase(s.replace('_', '-'));
            }
            if (format == LOWER_UNDERSCORE) {
                return Ascii.toLowerCase(s);
            }
            return super.convert(format, s);
        }
    };
    
    private final CharMatcher wordBoundary;
    private final String wordSeparator;

    private static final class StringConverter extends Converter<String, String> implements Serializable {
        private static final long serialVersionUID = 0;
        private final CaseFormat sourceFormat;
        private final CaseFormat targetFormat;

        StringConverter(CaseFormat sourceFormat, CaseFormat targetFormat) {
            this.sourceFormat = (CaseFormat) Preconditions.checkNotNull(sourceFormat);
            this.targetFormat = (CaseFormat) Preconditions.checkNotNull(targetFormat);
        }

        /* Access modifiers changed, original: protected */
        public String doForward(String s) {
            return s == null ? null : this.sourceFormat.to(this.targetFormat, s);
        }

        /* Access modifiers changed, original: protected */
        public String doBackward(String s) {
            return s == null ? null : this.targetFormat.to(this.sourceFormat, s);
        }

        public boolean equals(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof StringConverter)) {
                return false;
            }
            StringConverter that = (StringConverter) object;
            if (this.sourceFormat.equals(that.sourceFormat) && this.targetFormat.equals(that.targetFormat)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.sourceFormat.hashCode() ^ this.targetFormat.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.sourceFormat);
            stringBuilder.append(".converterTo(");
            stringBuilder.append(this.targetFormat);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    public abstract String normalizeWord(String str);

    private CaseFormat(CharMatcher wordBoundary, String wordSeparator) {
        this.wordBoundary = wordBoundary;
        this.wordSeparator = wordSeparator;
    }

    public final String to(CaseFormat format, String str) {
        Preconditions.checkNotNull(format);
        Preconditions.checkNotNull(str);
        return format == this ? str : convert(format, str);
    }

    /* Access modifiers changed, original: 0000 */
    public String convert(CaseFormat format, String s) {
        int i = 0;
        StringBuilder out = null;
        int j = -1;
        while (true) {
            int indexIn = this.wordBoundary.indexIn(s, j + 1);
            j = indexIn;
            if (indexIn == -1) {
                break;
            }
            if (i == 0) {
                out = new StringBuilder(s.length() + (4 * this.wordSeparator.length()));
                out.append(format.normalizeFirstWord(s.substring(i, j)));
            } else {
                out.append(format.normalizeWord(s.substring(i, j)));
            }
            out.append(format.wordSeparator);
            i = j + this.wordSeparator.length();
        }
        if (i == 0) {
            return format.normalizeFirstWord(s);
        }
        out.append(format.normalizeWord(s.substring(i)));
        return out.toString();
    }

    @Beta
    public Converter<String, String> converterTo(CaseFormat targetFormat) {
        return new StringConverter(this, targetFormat);
    }

    private String normalizeFirstWord(String word) {
        return this == LOWER_CAMEL ? Ascii.toLowerCase(word) : normalizeWord(word);
    }

    private static String firstCharOnlyToUpper(String word) {
        if (word.isEmpty()) {
            return word;
        }
        StringBuilder stringBuilder = new StringBuilder(word.length());
        stringBuilder.append(Ascii.toUpperCase(word.charAt(0)));
        stringBuilder.append(Ascii.toLowerCase(word.substring(1)));
        return stringBuilder.toString();
    }
}
