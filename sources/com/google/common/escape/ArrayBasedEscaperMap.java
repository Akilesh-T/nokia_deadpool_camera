package com.google.common.escape;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;

@GwtCompatible
@Beta
public final class ArrayBasedEscaperMap {
    private static final char[][] EMPTY_REPLACEMENT_ARRAY = ((char[][]) Array.newInstance(char.class, new int[]{0, 0}));
    private final char[][] replacementArray;

    public static ArrayBasedEscaperMap create(Map<Character, String> replacements) {
        return new ArrayBasedEscaperMap(createReplacementArray(replacements));
    }

    private ArrayBasedEscaperMap(char[][] replacementArray) {
        this.replacementArray = replacementArray;
    }

    /* Access modifiers changed, original: 0000 */
    public char[][] getReplacementArray() {
        return this.replacementArray;
    }

    @VisibleForTesting
    static char[][] createReplacementArray(Map<Character, String> map) {
        Preconditions.checkNotNull(map);
        if (map.isEmpty()) {
            return EMPTY_REPLACEMENT_ARRAY;
        }
        char[][] replacements = new char[(((Character) Collections.max(map.keySet())).charValue() + 1)][];
        for (Character c : map.keySet()) {
            char c2 = c.charValue();
            replacements[c2] = ((String) map.get(Character.valueOf(c2))).toCharArray();
        }
        return replacements;
    }
}
