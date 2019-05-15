package com.google.android.apps.photos.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface Configuration {
    public static final String BADGE = "badge";
    public static final String EDIT = "edit";
    public static final String INTERACT = "interact";
    public static final String LAUNCH = "launch";
    public static final Set<String> VALID_CONFIGURATIONS = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{BADGE, EDIT, INTERACT, "launch"})));
}
