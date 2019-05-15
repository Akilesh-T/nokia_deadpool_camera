package com.google.common.escape;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@GwtCompatible
@Beta
public final class Escapers {
    private static final Escaper NULL_ESCAPER = new CharEscaper() {
        public String escape(String string) {
            return (String) Preconditions.checkNotNull(string);
        }

        /* Access modifiers changed, original: protected */
        public char[] escape(char c) {
            return null;
        }
    };

    @Beta
    public static final class Builder {
        private final Map<Character, String> replacementMap;
        private char safeMax;
        private char safeMin;
        private String unsafeReplacement;

        /* synthetic */ Builder(AnonymousClass1 x0) {
            this();
        }

        private Builder() {
            this.replacementMap = new HashMap();
            this.safeMin = 0;
            this.safeMax = 65535;
            this.unsafeReplacement = null;
        }

        public Builder setSafeRange(char safeMin, char safeMax) {
            this.safeMin = safeMin;
            this.safeMax = safeMax;
            return this;
        }

        public Builder setUnsafeReplacement(@Nullable String unsafeReplacement) {
            this.unsafeReplacement = unsafeReplacement;
            return this;
        }

        public Builder addEscape(char c, String replacement) {
            Preconditions.checkNotNull(replacement);
            this.replacementMap.put(Character.valueOf(c), replacement);
            return this;
        }

        public Escaper build() {
            return new ArrayBasedCharEscaper(this.replacementMap, this.safeMin, this.safeMax) {
                private final char[] replacementChars;

                /* Access modifiers changed, original: protected */
                public char[] escapeUnsafe(char c) {
                    return this.replacementChars;
                }
            };
        }
    }

    private Escapers() {
    }

    public static Escaper nullEscaper() {
        return NULL_ESCAPER;
    }

    public static Builder builder() {
        return new Builder();
    }

    static UnicodeEscaper asUnicodeEscaper(Escaper escaper) {
        Preconditions.checkNotNull(escaper);
        if (escaper instanceof UnicodeEscaper) {
            return (UnicodeEscaper) escaper;
        }
        if (escaper instanceof CharEscaper) {
            return wrap((CharEscaper) escaper);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot create a UnicodeEscaper from: ");
        stringBuilder.append(escaper.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static String computeReplacement(CharEscaper escaper, char c) {
        return stringOrNull(escaper.escape(c));
    }

    public static String computeReplacement(UnicodeEscaper escaper, int cp) {
        return stringOrNull(escaper.escape(cp));
    }

    private static String stringOrNull(char[] in) {
        return in == null ? null : new String(in);
    }

    private static UnicodeEscaper wrap(final CharEscaper escaper) {
        return new UnicodeEscaper() {
            /* Access modifiers changed, original: protected */
            public char[] escape(int cp) {
                if (cp < 65536) {
                    return escaper.escape((char) cp);
                }
                char[] surrogateChars = new char[2];
                int n = 0;
                Character.toChars(cp, surrogateChars, 0);
                char[] hiChars = escaper.escape(surrogateChars[0]);
                char[] loChars = escaper.escape(surrogateChars[1]);
                if (hiChars == null && loChars == null) {
                    return 0;
                }
                int hiCount = hiChars != null ? hiChars.length : 1;
                char[] output = new char[(hiCount + (loChars != null ? loChars.length : 1))];
                if (hiChars != null) {
                    for (int n2 = 0; n2 < hiChars.length; n2++) {
                        output[n2] = hiChars[n2];
                    }
                } else {
                    output[0] = surrogateChars[0];
                }
                if (loChars != null) {
                    while (n < loChars.length) {
                        output[hiCount + n] = loChars[n];
                        n++;
                    }
                } else {
                    output[hiCount] = surrogateChars[1];
                }
                return output;
            }
        };
    }
}
