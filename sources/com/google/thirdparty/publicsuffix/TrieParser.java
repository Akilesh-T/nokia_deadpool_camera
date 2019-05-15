package com.google.thirdparty.publicsuffix;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import java.util.List;

@GwtCompatible
class TrieParser {
    private static final Joiner PREFIX_JOINER = Joiner.on("");

    TrieParser() {
    }

    static ImmutableMap<String, PublicSuffixType> parseTrie(CharSequence encoded) {
        Builder<String, PublicSuffixType> builder = ImmutableMap.builder();
        int encodedLen = encoded.length();
        int idx = 0;
        while (idx < encodedLen) {
            idx += doParseTrieToBuilder(Lists.newLinkedList(), encoded.subSequence(idx, encodedLen), builder);
        }
        return builder.build();
    }

    private static int doParseTrieToBuilder(List<CharSequence> stack, CharSequence encoded, Builder<String, PublicSuffixType> builder) {
        int encodedLen = encoded.length();
        int idx = 0;
        char c = 0;
        while (idx < encodedLen) {
            c = encoded.charAt(idx);
            if (c == '&' || c == '?' || c == '!' || c == ':' || c == ',') {
                break;
            }
            idx++;
        }
        stack.add(0, reverse(encoded.subSequence(0, idx)));
        if (c == '!' || c == '?' || c == ':' || c == ',') {
            String domain = PREFIX_JOINER.join((Iterable) stack);
            if (domain.length() > 0) {
                builder.put(domain, PublicSuffixType.fromCode(c));
            }
        }
        idx++;
        if (c != '?' && c != ',') {
            while (idx < encodedLen) {
                idx += doParseTrieToBuilder(stack, encoded.subSequence(idx, encodedLen), builder);
                if (encoded.charAt(idx) != '?') {
                    if (encoded.charAt(idx) == ',') {
                    }
                }
                idx++;
                break;
            }
        }
        stack.remove(0);
        return idx;
    }

    private static CharSequence reverse(CharSequence s) {
        int length = s.length();
        int i = 1;
        if (length <= 1) {
            return s;
        }
        char[] buffer = new char[length];
        buffer[0] = s.charAt(length - 1);
        while (i < length) {
            buffer[i] = s.charAt((length - 1) - i);
            if (Character.isSurrogatePair(buffer[i], buffer[i - 1])) {
                swap(buffer, i - 1, i);
            }
            i++;
        }
        return new String(buffer);
    }

    private static void swap(char[] buffer, int f, int s) {
        char tmp = buffer[f];
        buffer[f] = buffer[s];
        buffer[s] = tmp;
    }
}
