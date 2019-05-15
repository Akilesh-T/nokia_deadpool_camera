package com.bumptech.glide.load.model;

import android.net.Uri;
import android.text.TextUtils;
import java.net.MalformedURLException;
import java.net.URL;

public class GlideUrl {
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    private URL safeUrl;
    private String stringUrl;
    private final URL url;

    public GlideUrl(URL url) {
        if (url != null) {
            this.url = url;
            this.stringUrl = null;
            return;
        }
        throw new IllegalArgumentException("URL must not be null!");
    }

    public GlideUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("String url must not be empty or null: ");
            stringBuilder.append(url);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.stringUrl = url;
        this.url = null;
    }

    public URL toURL() throws MalformedURLException {
        return getSafeUrl();
    }

    private URL getSafeUrl() throws MalformedURLException {
        if (this.safeUrl != null) {
            return this.safeUrl;
        }
        this.safeUrl = new URL(Uri.encode(toString(), ALLOWED_URI_CHARS));
        return this.safeUrl;
    }

    public String toString() {
        if (TextUtils.isEmpty(this.stringUrl)) {
            this.stringUrl = this.url.toString();
        }
        return this.stringUrl;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return toString().equals(o.toString());
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
