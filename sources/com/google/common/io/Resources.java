package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

@Beta
public final class Resources {

    private static final class UrlByteSource extends ByteSource {
        private final URL url;

        /* synthetic */ UrlByteSource(URL x0, AnonymousClass1 x1) {
            this(x0);
        }

        private UrlByteSource(URL url) {
            this.url = (URL) Preconditions.checkNotNull(url);
        }

        public InputStream openStream() throws IOException {
            return this.url.openStream();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resources.asByteSource(");
            stringBuilder.append(this.url);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private Resources() {
    }

    public static ByteSource asByteSource(URL url) {
        return new UrlByteSource(url, null);
    }

    public static CharSource asCharSource(URL url, Charset charset) {
        return asByteSource(url).asCharSource(charset);
    }

    public static byte[] toByteArray(URL url) throws IOException {
        return asByteSource(url).read();
    }

    public static String toString(URL url, Charset charset) throws IOException {
        return asCharSource(url, charset).read();
    }

    public static <T> T readLines(URL url, Charset charset, LineProcessor<T> callback) throws IOException {
        return asCharSource(url, charset).readLines(callback);
    }

    public static List<String> readLines(URL url, Charset charset) throws IOException {
        return (List) readLines(url, charset, new LineProcessor<List<String>>() {
            final List<String> result = Lists.newArrayList();

            public boolean processLine(String line) {
                this.result.add(line);
                return true;
            }

            public List<String> getResult() {
                return this.result;
            }
        });
    }

    public static void copy(URL from, OutputStream to) throws IOException {
        asByteSource(from).copyTo(to);
    }

    public static URL getResource(String resourceName) {
        URL url = ((ClassLoader) MoreObjects.firstNonNull(Thread.currentThread().getContextClassLoader(), Resources.class.getClassLoader())).getResource(resourceName);
        Preconditions.checkArgument(url != null, "resource %s not found.", resourceName);
        return url;
    }

    public static URL getResource(Class<?> contextClass, String resourceName) {
        URL url = contextClass.getResource(resourceName);
        Preconditions.checkArgument(url != null, "resource %s relative to %s not found.", resourceName, contextClass.getName());
        return url;
    }
}
