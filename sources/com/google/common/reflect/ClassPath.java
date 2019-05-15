package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@Beta
public final class ClassPath {
    private static final String CLASS_FILE_NAME_EXTENSION = ".class";
    private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(" ").omitEmptyStrings();
    private static final Predicate<ClassInfo> IS_TOP_LEVEL = new Predicate<ClassInfo>() {
        public boolean apply(ClassInfo info) {
            return info.className.indexOf(36) == -1;
        }
    };
    private static final Logger logger = Logger.getLogger(ClassPath.class.getName());
    private final ImmutableSet<ResourceInfo> resources;

    @Beta
    public static class ResourceInfo {
        final ClassLoader loader;
        private final String resourceName;

        static ResourceInfo of(String resourceName, ClassLoader loader) {
            if (resourceName.endsWith(ClassPath.CLASS_FILE_NAME_EXTENSION)) {
                return new ClassInfo(resourceName, loader);
            }
            return new ResourceInfo(resourceName, loader);
        }

        ResourceInfo(String resourceName, ClassLoader loader) {
            this.resourceName = (String) Preconditions.checkNotNull(resourceName);
            this.loader = (ClassLoader) Preconditions.checkNotNull(loader);
        }

        public final URL url() {
            return (URL) Preconditions.checkNotNull(this.loader.getResource(this.resourceName), "Failed to load resource: %s", this.resourceName);
        }

        public final String getResourceName() {
            return this.resourceName;
        }

        public int hashCode() {
            return this.resourceName.hashCode();
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof ResourceInfo)) {
                return false;
            }
            ResourceInfo that = (ResourceInfo) obj;
            if (this.resourceName.equals(that.resourceName) && this.loader == that.loader) {
                z = true;
            }
            return z;
        }

        public String toString() {
            return this.resourceName;
        }
    }

    @VisibleForTesting
    static final class Scanner {
        private final Builder<ResourceInfo> resources = new Builder(Ordering.usingToString());
        private final Set<URI> scannedUris = Sets.newHashSet();

        Scanner() {
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSortedSet<ResourceInfo> getResources() {
            return this.resources.build();
        }

        /* Access modifiers changed, original: 0000 */
        public void scan(URI uri, ClassLoader classloader) throws IOException {
            if (uri.getScheme().equals("file") && this.scannedUris.add(uri)) {
                scanFrom(new File(uri), classloader);
            }
        }

        /* Access modifiers changed, original: 0000 */
        @VisibleForTesting
        public void scanFrom(File file, ClassLoader classloader) throws IOException {
            if (file.exists()) {
                if (file.isDirectory()) {
                    scanDirectory(file, classloader);
                } else {
                    scanJar(file, classloader);
                }
            }
        }

        private void scanDirectory(File directory, ClassLoader classloader) throws IOException {
            scanDirectory(directory, classloader, "", ImmutableSet.of());
        }

        private void scanDirectory(File directory, ClassLoader classloader, String packagePrefix, ImmutableSet<File> ancestors) throws IOException {
            Object canonical = directory.getCanonicalFile();
            if (!ancestors.contains(canonical)) {
                File[] files = directory.listFiles();
                if (files == null) {
                    Logger access$100 = ClassPath.logger;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot read directory ");
                    stringBuilder.append(directory);
                    access$100.warning(stringBuilder.toString());
                    return;
                }
                ImmutableSet<File> newAncestors = ImmutableSet.builder().addAll((Iterable) ancestors).add(canonical).build();
                for (File f : files) {
                    String name = f.getName();
                    if (f.isDirectory()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(packagePrefix);
                        stringBuilder2.append(name);
                        stringBuilder2.append("/");
                        scanDirectory(f, classloader, stringBuilder2.toString(), newAncestors);
                    } else {
                        String resourceName = new StringBuilder();
                        resourceName.append(packagePrefix);
                        resourceName.append(name);
                        resourceName = resourceName.toString();
                        if (!resourceName.equals("META-INF/MANIFEST.MF")) {
                            this.resources.add(ResourceInfo.of(resourceName, classloader));
                        }
                    }
                }
            }
        }

        private void scanJar(File file, ClassLoader classloader) throws IOException {
            try {
                JarFile jarFile = new JarFile(file);
                try {
                    UnmodifiableIterator it = getClassPathFromManifest(file, jarFile.getManifest()).iterator();
                    while (it.hasNext()) {
                        scan((URI) it.next(), classloader);
                    }
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        if (!entry.isDirectory()) {
                            if (!entry.getName().equals("META-INF/MANIFEST.MF")) {
                                this.resources.add(ResourceInfo.of(entry.getName(), classloader));
                            }
                        }
                    }
                } finally {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
            }
        }

        @VisibleForTesting
        static ImmutableSet<URI> getClassPathFromManifest(File jarFile, @Nullable Manifest manifest) {
            if (manifest == null) {
                return ImmutableSet.of();
            }
            ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
            String classpathAttribute = manifest.getMainAttributes().getValue(Name.CLASS_PATH.toString());
            if (classpathAttribute != null) {
                for (String path : ClassPath.CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
                    try {
                        builder.add(getClassPathEntry(jarFile, path));
                    } catch (URISyntaxException e) {
                        Logger access$100 = ClassPath.logger;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid Class-Path entry: ");
                        stringBuilder.append(path);
                        access$100.warning(stringBuilder.toString());
                    }
                }
            }
            return builder.build();
        }

        @VisibleForTesting
        static URI getClassPathEntry(File jarFile, String path) throws URISyntaxException {
            URI uri = new URI(path);
            if (uri.isAbsolute()) {
                return uri;
            }
            return new File(jarFile.getParentFile(), path.replace('/', File.separatorChar)).toURI();
        }
    }

    @Beta
    public static final class ClassInfo extends ResourceInfo {
        private final String className;

        ClassInfo(String resourceName, ClassLoader loader) {
            super(resourceName, loader);
            this.className = ClassPath.getClassName(resourceName);
        }

        public String getPackageName() {
            return Reflection.getPackageName(this.className);
        }

        public String getSimpleName() {
            int lastDollarSign = this.className.lastIndexOf(36);
            if (lastDollarSign != -1) {
                return CharMatcher.DIGIT.trimLeadingFrom(this.className.substring(lastDollarSign + 1));
            }
            String packageName = getPackageName();
            if (packageName.isEmpty()) {
                return this.className;
            }
            return this.className.substring(packageName.length() + 1);
        }

        public String getName() {
            return this.className;
        }

        public Class<?> load() {
            try {
                return this.loader.loadClass(this.className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        public String toString() {
            return this.className;
        }
    }

    private ClassPath(ImmutableSet<ResourceInfo> resources) {
        this.resources = resources;
    }

    public static ClassPath from(ClassLoader classloader) throws IOException {
        Scanner scanner = new Scanner();
        UnmodifiableIterator it = getClassPathEntries(classloader).entrySet().iterator();
        while (it.hasNext()) {
            Entry<URI, ClassLoader> entry = (Entry) it.next();
            scanner.scan((URI) entry.getKey(), (ClassLoader) entry.getValue());
        }
        return new ClassPath(scanner.getResources());
    }

    public ImmutableSet<ResourceInfo> getResources() {
        return this.resources;
    }

    public ImmutableSet<ClassInfo> getAllClasses() {
        return FluentIterable.from(this.resources).filter(ClassInfo.class).toSet();
    }

    public ImmutableSet<ClassInfo> getTopLevelClasses() {
        return FluentIterable.from(this.resources).filter(ClassInfo.class).filter(IS_TOP_LEVEL).toSet();
    }

    public ImmutableSet<ClassInfo> getTopLevelClasses(String packageName) {
        Preconditions.checkNotNull(packageName);
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        UnmodifiableIterator it = getTopLevelClasses().iterator();
        while (it.hasNext()) {
            Object classInfo = (ClassInfo) it.next();
            if (classInfo.getPackageName().equals(packageName)) {
                builder.add(classInfo);
            }
        }
        return builder.build();
    }

    public ImmutableSet<ClassInfo> getTopLevelClassesRecursive(String packageName) {
        Preconditions.checkNotNull(packageName);
        String packagePrefix = new StringBuilder();
        packagePrefix.append(packageName);
        packagePrefix.append('.');
        packagePrefix = packagePrefix.toString();
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        UnmodifiableIterator it = getTopLevelClasses().iterator();
        while (it.hasNext()) {
            Object classInfo = (ClassInfo) it.next();
            if (classInfo.getName().startsWith(packagePrefix)) {
                builder.add(classInfo);
            }
        }
        return builder.build();
    }

    @VisibleForTesting
    static ImmutableMap<URI, ClassLoader> getClassPathEntries(ClassLoader classloader) {
        LinkedHashMap<URI, ClassLoader> entries = Maps.newLinkedHashMap();
        ClassLoader parent = classloader.getParent();
        if (parent != null) {
            entries.putAll(getClassPathEntries(parent));
        }
        if (classloader instanceof URLClassLoader) {
            URL[] uRLs = ((URLClassLoader) classloader).getURLs();
            int length = uRLs.length;
            int i = 0;
            while (i < length) {
                try {
                    URI uri = uRLs[i].toURI();
                    if (!entries.containsKey(uri)) {
                        entries.put(uri, classloader);
                    }
                    i++;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return ImmutableMap.copyOf(entries);
    }

    @VisibleForTesting
    static String getClassName(String filename) {
        return filename.substring(0, filename.length() - CLASS_FILE_NAME_EXTENSION.length()).replace('/', '.');
    }
}
