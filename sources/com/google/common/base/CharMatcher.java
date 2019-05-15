package com.google.common.base;

import android.support.v4.internal.view.SupportMenu;
import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.BitSet;
import javax.annotation.CheckReturnValue;

@GwtCompatible(emulated = true)
@Beta
public abstract class CharMatcher implements Predicate<Character> {
    public static final CharMatcher ANY = new FastMatcher("CharMatcher.ANY") {
        public boolean matches(char c) {
            return true;
        }

        public int indexIn(CharSequence sequence) {
            return sequence.length() == 0 ? -1 : 0;
        }

        public int indexIn(CharSequence sequence, int start) {
            int length = sequence.length();
            Preconditions.checkPositionIndex(start, length);
            return start == length ? -1 : start;
        }

        public int lastIndexIn(CharSequence sequence) {
            return sequence.length() - 1;
        }

        public boolean matchesAllOf(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return true;
        }

        public boolean matchesNoneOf(CharSequence sequence) {
            return sequence.length() == 0;
        }

        public String removeFrom(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return "";
        }

        public String replaceFrom(CharSequence sequence, char replacement) {
            char[] array = new char[sequence.length()];
            Arrays.fill(array, replacement);
            return new String(array);
        }

        public String replaceFrom(CharSequence sequence, CharSequence replacement) {
            StringBuilder retval = new StringBuilder(sequence.length() * replacement.length());
            for (int i = 0; i < sequence.length(); i++) {
                retval.append(replacement);
            }
            return retval.toString();
        }

        public String collapseFrom(CharSequence sequence, char replacement) {
            return sequence.length() == 0 ? "" : String.valueOf(replacement);
        }

        public String trimFrom(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return "";
        }

        public int countIn(CharSequence sequence) {
            return sequence.length();
        }

        public CharMatcher and(CharMatcher other) {
            return (CharMatcher) Preconditions.checkNotNull(other);
        }

        public CharMatcher or(CharMatcher other) {
            Preconditions.checkNotNull(other);
            return this;
        }

        public CharMatcher negate() {
            return NONE;
        }
    };
    public static final CharMatcher ASCII = inRange(0, Ascii.MAX, "CharMatcher.ASCII");
    public static final CharMatcher BREAKING_WHITESPACE = new CharMatcher() {
        public boolean matches(char c) {
            boolean z = true;
            if (!(c == ' ' || c == 133 || c == 5760)) {
                if (c == 8199) {
                    return false;
                }
                if (!(c == 8287 || c == 12288)) {
                    switch (c) {
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                            break;
                        default:
                            switch (c) {
                                case 8232:
                                case 8233:
                                    break;
                                default:
                                    if (c < 8192 || c > 8202) {
                                        z = false;
                                    }
                                    return z;
                            }
                    }
                }
            }
            return true;
        }

        public String toString() {
            return "CharMatcher.BREAKING_WHITESPACE";
        }
    };
    public static final CharMatcher DIGIT = new RangesMatcher("CharMatcher.DIGIT", ZEROES.toCharArray(), NINES.toCharArray());
    private static final int DISTINCT_CHARS = 65536;
    public static final CharMatcher INVISIBLE = new RangesMatcher("CharMatcher.INVISIBLE", "\u0000­؀؜۝܏ ᠎   ⁦⁧⁨⁩⁪　?﻿￹￺".toCharArray(), "  ­؄؜۝܏ ᠎‏ ⁤⁦⁧⁨⁩⁯　﻿￹￻".toCharArray());
    public static final CharMatcher JAVA_DIGIT = new CharMatcher("CharMatcher.JAVA_DIGIT") {
        public boolean matches(char c) {
            return Character.isDigit(c);
        }
    };
    public static final CharMatcher JAVA_ISO_CONTROL = inRange(0, 31).or(inRange(Ascii.MAX, 159)).withToString("CharMatcher.JAVA_ISO_CONTROL");
    public static final CharMatcher JAVA_LETTER = new CharMatcher("CharMatcher.JAVA_LETTER") {
        public boolean matches(char c) {
            return Character.isLetter(c);
        }
    };
    public static final CharMatcher JAVA_LETTER_OR_DIGIT = new CharMatcher("CharMatcher.JAVA_LETTER_OR_DIGIT") {
        public boolean matches(char c) {
            return Character.isLetterOrDigit(c);
        }
    };
    public static final CharMatcher JAVA_LOWER_CASE = new CharMatcher("CharMatcher.JAVA_LOWER_CASE") {
        public boolean matches(char c) {
            return Character.isLowerCase(c);
        }
    };
    public static final CharMatcher JAVA_UPPER_CASE = new CharMatcher("CharMatcher.JAVA_UPPER_CASE") {
        public boolean matches(char c) {
            return Character.isUpperCase(c);
        }
    };
    private static final String NINES;
    public static final CharMatcher NONE = new FastMatcher("CharMatcher.NONE") {
        public boolean matches(char c) {
            return false;
        }

        public int indexIn(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return -1;
        }

        public int indexIn(CharSequence sequence, int start) {
            Preconditions.checkPositionIndex(start, sequence.length());
            return -1;
        }

        public int lastIndexIn(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return -1;
        }

        public boolean matchesAllOf(CharSequence sequence) {
            return sequence.length() == 0;
        }

        public boolean matchesNoneOf(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return true;
        }

        public String removeFrom(CharSequence sequence) {
            return sequence.toString();
        }

        public String replaceFrom(CharSequence sequence, char replacement) {
            return sequence.toString();
        }

        public String replaceFrom(CharSequence sequence, CharSequence replacement) {
            Preconditions.checkNotNull(replacement);
            return sequence.toString();
        }

        public String collapseFrom(CharSequence sequence, char replacement) {
            return sequence.toString();
        }

        public String trimFrom(CharSequence sequence) {
            return sequence.toString();
        }

        public String trimLeadingFrom(CharSequence sequence) {
            return sequence.toString();
        }

        public String trimTrailingFrom(CharSequence sequence) {
            return sequence.toString();
        }

        public int countIn(CharSequence sequence) {
            Preconditions.checkNotNull(sequence);
            return 0;
        }

        public CharMatcher and(CharMatcher other) {
            Preconditions.checkNotNull(other);
            return this;
        }

        public CharMatcher or(CharMatcher other) {
            return (CharMatcher) Preconditions.checkNotNull(other);
        }

        public CharMatcher negate() {
            return ANY;
        }
    };
    public static final CharMatcher SINGLE_WIDTH = new RangesMatcher("CharMatcher.SINGLE_WIDTH", "\u0000־א׳؀ݐ฀Ḁ℀ﭐﹰ｡".toCharArray(), "ӹ־ת״ۿݿ๿₯℺﷿﻿ￜ".toCharArray());
    public static final CharMatcher WHITESPACE = new FastMatcher("WHITESPACE") {
        public boolean matches(char c) {
            return CharMatcher.WHITESPACE_TABLE.charAt((CharMatcher.WHITESPACE_MULTIPLIER * c) >>> WHITESPACE_SHIFT) == c;
        }

        /* Access modifiers changed, original: 0000 */
        @GwtIncompatible("java.util.BitSet")
        public void setBits(BitSet table) {
            for (int i = 0; i < CharMatcher.WHITESPACE_TABLE.length(); i++) {
                table.set(CharMatcher.WHITESPACE_TABLE.charAt(i));
            }
        }
    };
    static final int WHITESPACE_MULTIPLIER = 1682554634;
    static final int WHITESPACE_SHIFT = Integer.numberOfLeadingZeros(WHITESPACE_TABLE.length() - 1);
    static final String WHITESPACE_TABLE = " 　\r   　 \u000b　   　 \t     \f 　 　　 \n 　";
    private static final String ZEROES = "0٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０";
    final String description;

    private static class And extends CharMatcher {
        final CharMatcher first;
        final CharMatcher second;

        And(CharMatcher a, CharMatcher b) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CharMatcher.and(");
            stringBuilder.append(a);
            stringBuilder.append(", ");
            stringBuilder.append(b);
            stringBuilder.append(")");
            this(a, b, stringBuilder.toString());
        }

        And(CharMatcher a, CharMatcher b, String description) {
            super(description);
            this.first = (CharMatcher) Preconditions.checkNotNull(a);
            this.second = (CharMatcher) Preconditions.checkNotNull(b);
        }

        public boolean matches(char c) {
            return this.first.matches(c) && this.second.matches(c);
        }

        /* Access modifiers changed, original: 0000 */
        @GwtIncompatible("java.util.BitSet")
        public void setBits(BitSet table) {
            BitSet tmp1 = new BitSet();
            this.first.setBits(tmp1);
            BitSet tmp2 = new BitSet();
            this.second.setBits(tmp2);
            tmp1.and(tmp2);
            table.or(tmp1);
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher withToString(String description) {
            return new And(this.first, this.second, description);
        }
    }

    static abstract class FastMatcher extends CharMatcher {
        FastMatcher() {
        }

        FastMatcher(String description) {
            super(description);
        }

        public final CharMatcher precomputed() {
            return this;
        }

        public CharMatcher negate() {
            return new NegatedFastMatcher(this);
        }
    }

    private static class NegatedMatcher extends CharMatcher {
        final CharMatcher original;

        NegatedMatcher(String toString, CharMatcher original) {
            super(toString);
            this.original = original;
        }

        NegatedMatcher(CharMatcher original) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(original);
            stringBuilder.append(".negate()");
            this(stringBuilder.toString(), original);
        }

        public boolean matches(char c) {
            return this.original.matches(c) ^ 1;
        }

        public boolean matchesAllOf(CharSequence sequence) {
            return this.original.matchesNoneOf(sequence);
        }

        public boolean matchesNoneOf(CharSequence sequence) {
            return this.original.matchesAllOf(sequence);
        }

        public int countIn(CharSequence sequence) {
            return sequence.length() - this.original.countIn(sequence);
        }

        /* Access modifiers changed, original: 0000 */
        @GwtIncompatible("java.util.BitSet")
        public void setBits(BitSet table) {
            BitSet tmp = new BitSet();
            this.original.setBits(tmp);
            tmp.flip(0, 65536);
            table.or(tmp);
        }

        public CharMatcher negate() {
            return this.original;
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher withToString(String description) {
            return new NegatedMatcher(description, this.original);
        }
    }

    private static class Or extends CharMatcher {
        final CharMatcher first;
        final CharMatcher second;

        Or(CharMatcher a, CharMatcher b, String description) {
            super(description);
            this.first = (CharMatcher) Preconditions.checkNotNull(a);
            this.second = (CharMatcher) Preconditions.checkNotNull(b);
        }

        Or(CharMatcher a, CharMatcher b) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CharMatcher.or(");
            stringBuilder.append(a);
            stringBuilder.append(", ");
            stringBuilder.append(b);
            stringBuilder.append(")");
            this(a, b, stringBuilder.toString());
        }

        /* Access modifiers changed, original: 0000 */
        @GwtIncompatible("java.util.BitSet")
        public void setBits(BitSet table) {
            this.first.setBits(table);
            this.second.setBits(table);
        }

        public boolean matches(char c) {
            return this.first.matches(c) || this.second.matches(c);
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher withToString(String description) {
            return new Or(this.first, this.second, description);
        }
    }

    private static class RangesMatcher extends CharMatcher {
        private final char[] rangeEnds;
        private final char[] rangeStarts;

        RangesMatcher(String description, char[] rangeStarts, char[] rangeEnds) {
            super(description);
            this.rangeStarts = rangeStarts;
            this.rangeEnds = rangeEnds;
            Preconditions.checkArgument(rangeStarts.length == rangeEnds.length);
            for (int i = 0; i < rangeStarts.length; i++) {
                Preconditions.checkArgument(rangeStarts[i] <= rangeEnds[i]);
                if (i + 1 < rangeStarts.length) {
                    Preconditions.checkArgument(rangeEnds[i] < rangeStarts[i + 1]);
                }
            }
        }

        public boolean matches(char c) {
            int index = Arrays.binarySearch(this.rangeStarts, c);
            boolean z = true;
            if (index >= 0) {
                return true;
            }
            int index2 = (~index) - 1;
            if (index2 < 0 || c > this.rangeEnds[index2]) {
                z = false;
            }
            return z;
        }
    }

    @GwtIncompatible("java.util.BitSet")
    private static class BitSetMatcher extends FastMatcher {
        private final BitSet table;

        /* synthetic */ BitSetMatcher(BitSet x0, String x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private BitSetMatcher(BitSet table, String description) {
            super(description);
            if (table.length() + 64 < table.size()) {
                table = (BitSet) table.clone();
            }
            this.table = table;
        }

        public boolean matches(char c) {
            return this.table.get(c);
        }

        /* Access modifiers changed, original: 0000 */
        public void setBits(BitSet bitSet) {
            bitSet.or(this.table);
        }
    }

    static final class NegatedFastMatcher extends NegatedMatcher {
        NegatedFastMatcher(CharMatcher original) {
            super(original);
        }

        NegatedFastMatcher(String toString, CharMatcher original) {
            super(toString, original);
        }

        public final CharMatcher precomputed() {
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher withToString(String description) {
            return new NegatedFastMatcher(description, this.original);
        }
    }

    public abstract boolean matches(char c);

    static {
        StringBuilder builder = new StringBuilder(ZEROES.length());
        for (int i = 0; i < ZEROES.length(); i++) {
            builder.append((char) (ZEROES.charAt(i) + 9));
        }
        NINES = builder.toString();
    }

    private static String showCharacter(char c) {
        String hex = "0123456789ABCDEF";
        char[] tmp = new char[]{'\\', 'u', 0, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            tmp[5 - i] = hex.charAt(c & 15);
            c = (char) (c >> 4);
        }
        return String.copyValueOf(tmp);
    }

    public static CharMatcher is(final char match) {
        String description = new StringBuilder();
        description.append("CharMatcher.is('");
        description.append(showCharacter(match));
        description.append("')");
        return new FastMatcher(description.toString()) {
            public boolean matches(char c) {
                return c == match;
            }

            public String replaceFrom(CharSequence sequence, char replacement) {
                return sequence.toString().replace(match, replacement);
            }

            public CharMatcher and(CharMatcher other) {
                return other.matches(match) ? this : NONE;
            }

            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? other : super.or(other);
            }

            public CharMatcher negate() {
                return CharMatcher.isNot(match);
            }

            /* Access modifiers changed, original: 0000 */
            @GwtIncompatible("java.util.BitSet")
            public void setBits(BitSet table) {
                table.set(match);
            }
        };
    }

    public static CharMatcher isNot(final char match) {
        String description = new StringBuilder();
        description.append("CharMatcher.isNot('");
        description.append(showCharacter(match));
        description.append("')");
        return new FastMatcher(description.toString()) {
            public boolean matches(char c) {
                return c != match;
            }

            public CharMatcher and(CharMatcher other) {
                return other.matches(match) ? super.and(other) : other;
            }

            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? ANY : this;
            }

            /* Access modifiers changed, original: 0000 */
            @GwtIncompatible("java.util.BitSet")
            public void setBits(BitSet table) {
                table.set(0, match);
                table.set(match + 1, 65536);
            }

            public CharMatcher negate() {
                return CharMatcher.is(match);
            }
        };
    }

    public static CharMatcher anyOf(CharSequence sequence) {
        int i = 0;
        switch (sequence.length()) {
            case 0:
                return NONE;
            case 1:
                return is(sequence.charAt(0));
            case 2:
                return isEither(sequence.charAt(0), sequence.charAt(1));
            default:
                final char[] chars = sequence.toString().toCharArray();
                Arrays.sort(chars);
                StringBuilder description = new StringBuilder("CharMatcher.anyOf(\"");
                int length = chars.length;
                while (i < length) {
                    description.append(showCharacter(chars[i]));
                    i++;
                }
                description.append("\")");
                return new CharMatcher(description.toString()) {
                    public boolean matches(char c) {
                        return Arrays.binarySearch(chars, c) >= 0;
                    }

                    /* Access modifiers changed, original: 0000 */
                    @GwtIncompatible("java.util.BitSet")
                    public void setBits(BitSet table) {
                        for (char c : chars) {
                            table.set(c);
                        }
                    }
                };
        }
    }

    private static CharMatcher isEither(final char match1, final char match2) {
        String description = new StringBuilder();
        description.append("CharMatcher.anyOf(\"");
        description.append(showCharacter(match1));
        description.append(showCharacter(match2));
        description.append("\")");
        return new FastMatcher(description.toString()) {
            public boolean matches(char c) {
                return c == match1 || c == match2;
            }

            /* Access modifiers changed, original: 0000 */
            @GwtIncompatible("java.util.BitSet")
            public void setBits(BitSet table) {
                table.set(match1);
                table.set(match2);
            }
        };
    }

    public static CharMatcher noneOf(CharSequence sequence) {
        return anyOf(sequence).negate();
    }

    public static CharMatcher inRange(char startInclusive, char endInclusive) {
        Preconditions.checkArgument(endInclusive >= startInclusive);
        String description = new StringBuilder();
        description.append("CharMatcher.inRange('");
        description.append(showCharacter(startInclusive));
        description.append("', '");
        description.append(showCharacter(endInclusive));
        description.append("')");
        return inRange(startInclusive, endInclusive, description.toString());
    }

    static CharMatcher inRange(final char startInclusive, final char endInclusive, String description) {
        return new FastMatcher(description) {
            public boolean matches(char c) {
                return startInclusive <= c && c <= endInclusive;
            }

            /* Access modifiers changed, original: 0000 */
            @GwtIncompatible("java.util.BitSet")
            public void setBits(BitSet table) {
                table.set(startInclusive, endInclusive + 1);
            }
        };
    }

    public static CharMatcher forPredicate(final Predicate<? super Character> predicate) {
        Preconditions.checkNotNull(predicate);
        if (predicate instanceof CharMatcher) {
            return (CharMatcher) predicate;
        }
        String description = new StringBuilder();
        description.append("CharMatcher.forPredicate(");
        description.append(predicate);
        description.append(")");
        return new CharMatcher(description.toString()) {
            public boolean matches(char c) {
                return predicate.apply(Character.valueOf(c));
            }

            public boolean apply(Character character) {
                return predicate.apply(Preconditions.checkNotNull(character));
            }
        };
    }

    CharMatcher(String description) {
        this.description = description;
    }

    protected CharMatcher() {
        this.description = super.toString();
    }

    public CharMatcher negate() {
        return new NegatedMatcher(this);
    }

    public CharMatcher and(CharMatcher other) {
        return new And(this, (CharMatcher) Preconditions.checkNotNull(other));
    }

    public CharMatcher or(CharMatcher other) {
        return new Or(this, (CharMatcher) Preconditions.checkNotNull(other));
    }

    public CharMatcher precomputed() {
        return Platform.precomputeCharMatcher(this);
    }

    /* Access modifiers changed, original: 0000 */
    public CharMatcher withToString(String description) {
        throw new UnsupportedOperationException();
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("java.util.BitSet")
    public CharMatcher precomputedInternal() {
        BitSet table = new BitSet();
        setBits(table);
        int totalCharacters = table.cardinality();
        if (totalCharacters * 2 <= 65536) {
            return precomputedPositive(totalCharacters, table, this.description);
        }
        String negatedDescription;
        table.flip(0, 65536);
        int negatedCharacters = 65536 - totalCharacters;
        String suffix = ".negate()";
        if (this.description.endsWith(suffix)) {
            negatedDescription = this.description.substring(0, this.description.length() - suffix.length());
        } else {
            negatedDescription = new StringBuilder();
            negatedDescription.append(this.description);
            negatedDescription.append(suffix);
            negatedDescription = negatedDescription.toString();
        }
        return new NegatedFastMatcher(toString(), precomputedPositive(negatedCharacters, table, negatedDescription));
    }

    @GwtIncompatible("java.util.BitSet")
    private static CharMatcher precomputedPositive(int totalCharacters, BitSet table, String description) {
        switch (totalCharacters) {
            case 0:
                return NONE;
            case 1:
                return is((char) table.nextSetBit(0));
            case 2:
                char c1 = (char) table.nextSetBit(0);
                return isEither(c1, (char) table.nextSetBit(c1 + 1));
            default:
                CharMatcher from;
                if (isSmall(totalCharacters, table.length())) {
                    from = SmallCharMatcher.from(table, description);
                } else {
                    from = new BitSetMatcher(table, description, null);
                }
                return from;
        }
    }

    @GwtIncompatible("SmallCharMatcher")
    private static boolean isSmall(int totalCharacters, int tableLength) {
        return totalCharacters <= 1023 && tableLength > (totalCharacters * 4) * 16;
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("java.util.BitSet")
    public void setBits(BitSet table) {
        for (int c = SupportMenu.USER_MASK; c >= 0; c--) {
            if (matches((char) c)) {
                table.set(c);
            }
        }
    }

    public boolean matchesAnyOf(CharSequence sequence) {
        return matchesNoneOf(sequence) ^ 1;
    }

    public boolean matchesAllOf(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (!matches(sequence.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesNoneOf(CharSequence sequence) {
        return indexIn(sequence) == -1;
    }

    public int indexIn(CharSequence sequence) {
        int length = sequence.length();
        for (int i = 0; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public int indexIn(CharSequence sequence, int start) {
        int length = sequence.length();
        Preconditions.checkPositionIndex(start, length);
        for (int i = start; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexIn(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public int countIn(CharSequence sequence) {
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (matches(sequence.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    @CheckReturnValue
    public String removeFrom(CharSequence sequence) {
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }
        char[] chars = string.toCharArray();
        int pos2 = pos;
        pos = 1;
        while (true) {
            pos2++;
            while (pos2 != chars.length) {
                if (matches(chars[pos2])) {
                    pos++;
                } else {
                    chars[pos2 - pos] = chars[pos2];
                    pos2++;
                }
            }
            return new String(chars, 0, pos2 - pos);
        }
    }

    @CheckReturnValue
    public String retainFrom(CharSequence sequence) {
        return negate().removeFrom(sequence);
    }

    @CheckReturnValue
    public String replaceFrom(CharSequence sequence, char replacement) {
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }
        char[] chars = string.toCharArray();
        chars[pos] = replacement;
        for (int i = pos + 1; i < chars.length; i++) {
            if (matches(chars[i])) {
                chars[i] = replacement;
            }
        }
        return new String(chars);
    }

    @CheckReturnValue
    public String replaceFrom(CharSequence sequence, CharSequence replacement) {
        int replacementLen = replacement.length();
        if (replacementLen == 0) {
            return removeFrom(sequence);
        }
        int oldpos = 0;
        if (replacementLen == 1) {
            return replaceFrom(sequence, replacement.charAt(0));
        }
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }
        int len = string.length();
        StringBuilder buf = new StringBuilder(((len * 3) / 2) + 16);
        do {
            buf.append(string, oldpos, pos);
            buf.append(replacement);
            oldpos = pos + 1;
            pos = indexIn(string, oldpos);
        } while (pos != -1);
        buf.append(string, oldpos, len);
        return buf.toString();
    }

    @CheckReturnValue
    public String trimFrom(CharSequence sequence) {
        int len = sequence.length();
        int first = 0;
        while (first < len && matches(sequence.charAt(first))) {
            first++;
        }
        int last = len - 1;
        while (last > first && matches(sequence.charAt(last))) {
            last--;
        }
        return sequence.subSequence(first, last + 1).toString();
    }

    @CheckReturnValue
    public String trimLeadingFrom(CharSequence sequence) {
        int len = sequence.length();
        for (int first = 0; first < len; first++) {
            if (!matches(sequence.charAt(first))) {
                return sequence.subSequence(first, len).toString();
            }
        }
        return "";
    }

    @CheckReturnValue
    public String trimTrailingFrom(CharSequence sequence) {
        for (int last = sequence.length() - 1; last >= 0; last--) {
            if (!matches(sequence.charAt(last))) {
                return sequence.subSequence(0, last + 1).toString();
            }
        }
        return "";
    }

    @CheckReturnValue
    public String collapseFrom(CharSequence sequence, char replacement) {
        int i;
        StringBuilder stringBuilder;
        StringBuilder builder;
        int len = sequence.length();
        int i2 = 0;
        while (true) {
            i = i2;
            if (i >= len) {
                return sequence.toString();
            }
            char c = sequence.charAt(i);
            if (matches(c)) {
                if (c != replacement || (i != len - 1 && matches(sequence.charAt(i + 1)))) {
                    stringBuilder = new StringBuilder(len);
                    stringBuilder.append(sequence.subSequence(0, i));
                    builder = stringBuilder.append(replacement);
                } else {
                    i++;
                }
            }
            i2 = i + 1;
        }
        stringBuilder = new StringBuilder(len);
        stringBuilder.append(sequence.subSequence(0, i));
        builder = stringBuilder.append(replacement);
        return finishCollapseFrom(sequence, i + 1, len, replacement, builder, true);
    }

    @CheckReturnValue
    public String trimAndCollapseFrom(CharSequence sequence, char replacement) {
        int last;
        int len = sequence.length();
        int first = 0;
        while (first < len && matches(sequence.charAt(first))) {
            first++;
        }
        int last2 = len - 1;
        while (true) {
            last = last2;
            if (last > first && matches(sequence.charAt(last))) {
                last2 = last - 1;
            } else if (first != 0 && last == len - 1) {
                return collapseFrom(sequence, replacement);
            } else {
                return finishCollapseFrom(sequence, first, last + 1, replacement, new StringBuilder((last + 1) - first), false);
            }
        }
        if (first != 0) {
        }
        return finishCollapseFrom(sequence, first, last + 1, replacement, new StringBuilder((last + 1) - first), false);
    }

    private String finishCollapseFrom(CharSequence sequence, int start, int end, char replacement, StringBuilder builder, boolean inMatchingGroup) {
        boolean inMatchingGroup2 = inMatchingGroup;
        for (int i = start; i < end; i++) {
            char c = sequence.charAt(i);
            if (!matches(c)) {
                builder.append(c);
                inMatchingGroup2 = false;
            } else if (!inMatchingGroup2) {
                builder.append(replacement);
                inMatchingGroup2 = true;
            }
        }
        return builder.toString();
    }

    @Deprecated
    public boolean apply(Character character) {
        return matches(character.charValue());
    }

    public String toString() {
        return this.description;
    }
}
