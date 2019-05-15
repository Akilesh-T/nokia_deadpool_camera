package com.bumptech.glide.disklrucache;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class DiskLruCache implements Closeable {
    static final long ANY_SEQUENCE_NUMBER = -1;
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_BACKUP = "journal.bkp";
    static final String JOURNAL_FILE_TEMP = "journal.tmp";
    static final String MAGIC = "libcore.io.DiskLruCache";
    private static final String READ = "READ";
    private static final String REMOVE = "REMOVE";
    static final String VERSION_1 = "1";
    private final int appVersion;
    private final Callable<Void> cleanupCallable = new Callable<Void>() {
        /* JADX WARNING: Missing block: B:11:0x0027, code skipped:
            return null;
     */
        public java.lang.Void call() throws java.lang.Exception {
            /*
            r4 = this;
            r0 = com.bumptech.glide.disklrucache.DiskLruCache.this;
            monitor-enter(r0);
            r1 = com.bumptech.glide.disklrucache.DiskLruCache.this;	 Catch:{ all -> 0x0028 }
            r1 = r1.journalWriter;	 Catch:{ all -> 0x0028 }
            r2 = 0;
            if (r1 != 0) goto L_0x000e;
        L_0x000c:
            monitor-exit(r0);	 Catch:{ all -> 0x0028 }
            return r2;
        L_0x000e:
            r1 = com.bumptech.glide.disklrucache.DiskLruCache.this;	 Catch:{ all -> 0x0028 }
            r1.trimToSize();	 Catch:{ all -> 0x0028 }
            r1 = com.bumptech.glide.disklrucache.DiskLruCache.this;	 Catch:{ all -> 0x0028 }
            r1 = r1.journalRebuildRequired();	 Catch:{ all -> 0x0028 }
            if (r1 == 0) goto L_0x0026;
        L_0x001b:
            r1 = com.bumptech.glide.disklrucache.DiskLruCache.this;	 Catch:{ all -> 0x0028 }
            r1.rebuildJournal();	 Catch:{ all -> 0x0028 }
            r1 = com.bumptech.glide.disklrucache.DiskLruCache.this;	 Catch:{ all -> 0x0028 }
            r3 = 0;
            r1.redundantOpCount = r3;	 Catch:{ all -> 0x0028 }
        L_0x0026:
            monitor-exit(r0);	 Catch:{ all -> 0x0028 }
            return r2;
        L_0x0028:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x0028 }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.disklrucache.DiskLruCache$AnonymousClass1.call():java.lang.Void");
        }
    };
    private final File directory;
    final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());
    private final File journalFile;
    private final File journalFileBackup;
    private final File journalFileTmp;
    private Writer journalWriter;
    private final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap(0, 0.75f, true);
    private long maxSize;
    private long nextSequenceNumber = 0;
    private int redundantOpCount;
    private long size = 0;
    private final int valueCount;

    public final class Editor {
        private boolean committed;
        private final Entry entry;
        private final boolean[] written;

        /* synthetic */ Editor(DiskLruCache x0, Entry x1, AnonymousClass1 x2) {
            this(x1);
        }

        private Editor(Entry entry) {
            this.entry = entry;
            this.written = entry.readable ? null : new boolean[DiskLruCache.this.valueCount];
        }

        private InputStream newInputStream(int index) throws IOException {
            synchronized (DiskLruCache.this) {
                if (this.entry.currentEditor != this) {
                    throw new IllegalStateException();
                } else if (this.entry.readable) {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(this.entry.getCleanFile(index));
                        return fileInputStream;
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }

        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? DiskLruCache.inputStreamToString(in) : null;
        }

        public File getFile(int index) throws IOException {
            File dirtyFile;
            synchronized (DiskLruCache.this) {
                if (this.entry.currentEditor == this) {
                    if (!this.entry.readable) {
                        this.written[index] = true;
                    }
                    dirtyFile = this.entry.getDirtyFile(index);
                    if (!DiskLruCache.this.directory.exists()) {
                        DiskLruCache.this.directory.mkdirs();
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
            return dirtyFile;
        }

        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(getFile(index)), Util.UTF_8);
                writer.write(value);
            } finally {
                Util.closeQuietly(writer);
            }
        }

        public void commit() throws IOException {
            DiskLruCache.this.completeEdit(this, true);
            this.committed = true;
        }

        public void abort() throws IOException {
            DiskLruCache.this.completeEdit(this, false);
        }

        public void abortUnlessCommitted() {
            if (!this.committed) {
                try {
                    abort();
                } catch (IOException e) {
                }
            }
        }
    }

    private final class Entry {
        File[] cleanFiles;
        private Editor currentEditor;
        File[] dirtyFiles;
        private final String key;
        private final long[] lengths;
        private boolean readable;
        private long sequenceNumber;

        /* synthetic */ Entry(DiskLruCache x0, String x1, AnonymousClass1 x2) {
            this(x1);
        }

        private Entry(String key) {
            this.key = key;
            this.lengths = new long[DiskLruCache.this.valueCount];
            this.cleanFiles = new File[DiskLruCache.this.valueCount];
            this.dirtyFiles = new File[DiskLruCache.this.valueCount];
            StringBuilder fileBuilder = new StringBuilder(key).append('.');
            int truncateTo = fileBuilder.length();
            for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
                fileBuilder.append(i);
                this.cleanFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
                fileBuilder.append(".tmp");
                this.dirtyFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
                fileBuilder.setLength(truncateTo);
            }
        }

        public String getLengths() throws IOException {
            StringBuilder result = new StringBuilder();
            for (long size : this.lengths) {
                result.append(' ');
                result.append(size);
            }
            return result.toString();
        }

        private void setLengths(String[] strings) throws IOException {
            if (strings.length == DiskLruCache.this.valueCount) {
                int i = 0;
                while (i < strings.length) {
                    try {
                        this.lengths[i] = Long.parseLong(strings[i]);
                        i++;
                    } catch (NumberFormatException e) {
                        throw invalidLengths(strings);
                    }
                }
                return;
            }
            throw invalidLengths(strings);
        }

        private IOException invalidLengths(String[] strings) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected journal line: ");
            stringBuilder.append(Arrays.toString(strings));
            throw new IOException(stringBuilder.toString());
        }

        public File getCleanFile(int i) {
            return this.cleanFiles[i];
        }

        public File getDirtyFile(int i) {
            return this.dirtyFiles[i];
        }
    }

    public final class Value {
        private final File[] files;
        private final String key;
        private final long[] lengths;
        private final long sequenceNumber;

        /* synthetic */ Value(DiskLruCache x0, String x1, long x2, File[] x3, long[] x4, AnonymousClass1 x5) {
            this(x1, x2, x3, x4);
        }

        private Value(String key, long sequenceNumber, File[] files, long[] lengths) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.files = files;
            this.lengths = lengths;
        }

        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(this.key, this.sequenceNumber);
        }

        public File getFile(int index) {
            return this.files[index];
        }

        public String getString(int index) throws IOException {
            return DiskLruCache.inputStreamToString(new FileInputStream(this.files[index]));
        }

        public long getLength(int index) {
            return this.lengths[index];
        }
    }

    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        File file = directory;
        this.directory = file;
        this.appVersion = appVersion;
        this.journalFile = new File(file, JOURNAL_FILE);
        this.journalFileTmp = new File(file, JOURNAL_FILE_TEMP);
        this.journalFileBackup = new File(file, JOURNAL_FILE_BACKUP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }

    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        } else if (valueCount > 0) {
            File backupFile = new File(directory, JOURNAL_FILE_BACKUP);
            if (backupFile.exists()) {
                File journalFile = new File(directory, JOURNAL_FILE);
                if (journalFile.exists()) {
                    backupFile.delete();
                } else {
                    renameTo(backupFile, journalFile, false);
                }
            }
            DiskLruCache diskLruCache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
            if (diskLruCache.journalFile.exists()) {
                try {
                    diskLruCache.readJournal();
                    diskLruCache.processJournal();
                    return diskLruCache;
                } catch (IOException journalIsCorrupt) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DiskLruCache ");
                    stringBuilder.append(directory);
                    stringBuilder.append(" is corrupt: ");
                    stringBuilder.append(journalIsCorrupt.getMessage());
                    stringBuilder.append(", removing");
                    printStream.println(stringBuilder.toString());
                    diskLruCache.delete();
                }
            }
            directory.mkdirs();
            DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
            cache.rebuildJournal();
            return cache;
        } else {
            throw new IllegalArgumentException("valueCount <= 0");
        }
    }

    private void readJournal() throws IOException {
        StrictLineReader reader = new StrictLineReader(new FileInputStream(this.journalFile), Util.US_ASCII);
        int lineCount;
        try {
            String magic = reader.readLine();
            String version = reader.readLine();
            String appVersionString = reader.readLine();
            String valueCountString = reader.readLine();
            String blank = reader.readLine();
            if (MAGIC.equals(magic) && "1".equals(version) && Integer.toString(this.appVersion).equals(appVersionString) && Integer.toString(this.valueCount).equals(valueCountString) && "".equals(blank)) {
                lineCount = 0;
                while (true) {
                    readJournalLine(reader.readLine());
                    lineCount++;
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected journal header: [");
                stringBuilder.append(magic);
                stringBuilder.append(", ");
                stringBuilder.append(version);
                stringBuilder.append(", ");
                stringBuilder.append(valueCountString);
                stringBuilder.append(", ");
                stringBuilder.append(blank);
                stringBuilder.append("]");
                throw new IOException(stringBuilder.toString());
            }
        } catch (EOFException e) {
            this.redundantOpCount = lineCount - this.lruEntries.size();
            if (reader.hasUnterminatedLine()) {
                rebuildJournal();
            } else {
                this.journalWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.journalFile, true), Util.US_ASCII));
            }
            Util.closeQuietly(reader);
        } catch (Throwable th) {
            Util.closeQuietly(reader);
        }
    }

    private void readJournalLine(String line) throws IOException {
        int firstSpace = line.indexOf(32);
        if (firstSpace != -1) {
            String key;
            int keyBegin = firstSpace + 1;
            int secondSpace = line.indexOf(32, keyBegin);
            if (secondSpace == -1) {
                key = line.substring(keyBegin);
                if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
                    this.lruEntries.remove(key);
                    return;
                }
            }
            key = line.substring(keyBegin, secondSpace);
            Entry entry = (Entry) this.lruEntries.get(key);
            if (entry == null) {
                entry = new Entry(this, key, null);
                this.lruEntries.put(key, entry);
            }
            if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
                String[] parts = line.substring(secondSpace + 1).split(" ");
                entry.readable = true;
                entry.currentEditor = null;
                entry.setLengths(parts);
            } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
                entry.currentEditor = new Editor(this, entry, null);
            } else if (!(secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected journal line: ");
                stringBuilder.append(line);
                throw new IOException(stringBuilder.toString());
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("unexpected journal line: ");
        stringBuilder2.append(line);
        throw new IOException(stringBuilder2.toString());
    }

    private void processJournal() throws IOException {
        deleteIfExists(this.journalFileTmp);
        Iterator<Entry> i = this.lruEntries.values().iterator();
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
            int t = 0;
            int t2;
            if (entry.currentEditor == null) {
                while (true) {
                    t2 = t;
                    if (t2 >= this.valueCount) {
                        break;
                    }
                    this.size += entry.lengths[t2];
                    t = t2 + 1;
                }
            } else {
                entry.currentEditor = null;
                while (true) {
                    t2 = t;
                    if (t2 >= this.valueCount) {
                        break;
                    }
                    deleteIfExists(entry.getCleanFile(t2));
                    deleteIfExists(entry.getDirtyFile(t2));
                    t = t2 + 1;
                }
                i.remove();
            }
        }
    }

    private synchronized void rebuildJournal() throws IOException {
        if (this.journalWriter != null) {
            this.journalWriter.close();
        }
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.journalFileTmp), Util.US_ASCII));
        try {
            writer.write(MAGIC);
            writer.write("\n");
            writer.write("1");
            writer.write("\n");
            writer.write(Integer.toString(this.appVersion));
            writer.write("\n");
            writer.write(Integer.toString(this.valueCount));
            writer.write("\n");
            writer.write("\n");
            for (Entry entry : this.lruEntries.values()) {
                StringBuilder stringBuilder;
                if (entry.currentEditor != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DIRTY ");
                    stringBuilder.append(entry.key);
                    stringBuilder.append(10);
                    writer.write(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("CLEAN ");
                    stringBuilder.append(entry.key);
                    stringBuilder.append(entry.getLengths());
                    stringBuilder.append(10);
                    writer.write(stringBuilder.toString());
                }
            }
            if (this.journalFile.exists()) {
                renameTo(this.journalFile, this.journalFileBackup, true);
            }
            renameTo(this.journalFileTmp, this.journalFile, false);
            this.journalFileBackup.delete();
            this.journalWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.journalFile, true), Util.US_ASCII));
        } finally {
            writer.close();
        }
    }

    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    private static void renameTo(File from, File to, boolean deleteDestination) throws IOException {
        if (deleteDestination) {
            deleteIfExists(to);
        }
        if (!from.renameTo(to)) {
            throw new IOException();
        }
    }

    public synchronized Value get(String key) throws IOException {
        checkNotClosed();
        Entry entry = (Entry) this.lruEntries.get(key);
        if (entry == null) {
            return null;
        }
        if (!entry.readable) {
            return null;
        }
        for (File file : entry.cleanFiles) {
            if (!file.exists()) {
                return null;
            }
        }
        this.redundantOpCount++;
        this.journalWriter.append(READ);
        this.journalWriter.append(' ');
        this.journalWriter.append(key);
        this.journalWriter.append(10);
        if (journalRebuildRequired()) {
            this.executorService.submit(this.cleanupCallable);
        }
        return new Value(this, key, entry.sequenceNumber, entry.cleanFiles, entry.lengths, null);
    }

    public Editor edit(String key) throws IOException {
        return edit(key, -1);
    }

    /* JADX WARNING: Missing block: B:9:0x001e, code skipped:
            return null;
     */
    private synchronized com.bumptech.glide.disklrucache.DiskLruCache.Editor edit(java.lang.String r6, long r7) throws java.io.IOException {
        /*
        r5 = this;
        monitor-enter(r5);
        r5.checkNotClosed();	 Catch:{ all -> 0x005e }
        r0 = r5.lruEntries;	 Catch:{ all -> 0x005e }
        r0 = r0.get(r6);	 Catch:{ all -> 0x005e }
        r0 = (com.bumptech.glide.disklrucache.DiskLruCache.Entry) r0;	 Catch:{ all -> 0x005e }
        r1 = -1;
        r1 = (r7 > r1 ? 1 : (r7 == r1 ? 0 : -1));
        r2 = 0;
        if (r1 == 0) goto L_0x001f;
    L_0x0013:
        if (r0 == 0) goto L_0x001d;
    L_0x0015:
        r3 = r0.sequenceNumber;	 Catch:{ all -> 0x005e }
        r1 = (r3 > r7 ? 1 : (r3 == r7 ? 0 : -1));
        if (r1 == 0) goto L_0x001f;
    L_0x001d:
        monitor-exit(r5);
        return r2;
    L_0x001f:
        if (r0 != 0) goto L_0x002d;
    L_0x0021:
        r1 = new com.bumptech.glide.disklrucache.DiskLruCache$Entry;	 Catch:{ all -> 0x005e }
        r1.<init>(r5, r6, r2);	 Catch:{ all -> 0x005e }
        r0 = r1;
        r1 = r5.lruEntries;	 Catch:{ all -> 0x005e }
        r1.put(r6, r0);	 Catch:{ all -> 0x005e }
        goto L_0x0035;
    L_0x002d:
        r1 = r0.currentEditor;	 Catch:{ all -> 0x005e }
        if (r1 == 0) goto L_0x0035;
    L_0x0033:
        monitor-exit(r5);
        return r2;
    L_0x0035:
        r1 = new com.bumptech.glide.disklrucache.DiskLruCache$Editor;	 Catch:{ all -> 0x005e }
        r1.<init>(r5, r0, r2);	 Catch:{ all -> 0x005e }
        r0.currentEditor = r1;	 Catch:{ all -> 0x005e }
        r2 = r5.journalWriter;	 Catch:{ all -> 0x005e }
        r3 = "DIRTY";
        r2.append(r3);	 Catch:{ all -> 0x005e }
        r2 = r5.journalWriter;	 Catch:{ all -> 0x005e }
        r3 = 32;
        r2.append(r3);	 Catch:{ all -> 0x005e }
        r2 = r5.journalWriter;	 Catch:{ all -> 0x005e }
        r2.append(r6);	 Catch:{ all -> 0x005e }
        r2 = r5.journalWriter;	 Catch:{ all -> 0x005e }
        r3 = 10;
        r2.append(r3);	 Catch:{ all -> 0x005e }
        r2 = r5.journalWriter;	 Catch:{ all -> 0x005e }
        r2.flush();	 Catch:{ all -> 0x005e }
        monitor-exit(r5);
        return r1;
    L_0x005e:
        r6 = move-exception;
        monitor-exit(r5);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.disklrucache.DiskLruCache.edit(java.lang.String, long):com.bumptech.glide.disklrucache.DiskLruCache$Editor");
    }

    public File getDirectory() {
        return this.directory;
    }

    public synchronized long getMaxSize() {
        return this.maxSize;
    }

    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        this.executorService.submit(this.cleanupCallable);
    }

    public synchronized long size() {
        return this.size;
    }

    /* JADX WARNING: Missing block: B:44:0x0108, code skipped:
            return;
     */
    private synchronized void completeEdit(com.bumptech.glide.disklrucache.DiskLruCache.Editor r11, boolean r12) throws java.io.IOException {
        /*
        r10 = this;
        monitor-enter(r10);
        r0 = r11.entry;	 Catch:{ all -> 0x010f }
        r1 = r0.currentEditor;	 Catch:{ all -> 0x010f }
        if (r1 != r11) goto L_0x0109;
    L_0x000b:
        r1 = 0;
        if (r12 == 0) goto L_0x004d;
    L_0x000e:
        r2 = r0.readable;	 Catch:{ all -> 0x010f }
        if (r2 != 0) goto L_0x004d;
    L_0x0014:
        r2 = r1;
    L_0x0015:
        r3 = r10.valueCount;	 Catch:{ all -> 0x010f }
        if (r2 >= r3) goto L_0x004d;
    L_0x0019:
        r3 = r11.written;	 Catch:{ all -> 0x010f }
        r3 = r3[r2];	 Catch:{ all -> 0x010f }
        if (r3 == 0) goto L_0x0033;
    L_0x0021:
        r3 = r0.getDirtyFile(r2);	 Catch:{ all -> 0x010f }
        r3 = r3.exists();	 Catch:{ all -> 0x010f }
        if (r3 != 0) goto L_0x0030;
    L_0x002b:
        r11.abort();	 Catch:{ all -> 0x010f }
        monitor-exit(r10);
        return;
    L_0x0030:
        r2 = r2 + 1;
        goto L_0x0015;
    L_0x0033:
        r11.abort();	 Catch:{ all -> 0x010f }
        r1 = new java.lang.IllegalStateException;	 Catch:{ all -> 0x010f }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x010f }
        r3.<init>();	 Catch:{ all -> 0x010f }
        r4 = "Newly created entry didn't create value for index ";
        r3.append(r4);	 Catch:{ all -> 0x010f }
        r3.append(r2);	 Catch:{ all -> 0x010f }
        r3 = r3.toString();	 Catch:{ all -> 0x010f }
        r1.<init>(r3);	 Catch:{ all -> 0x010f }
        throw r1;	 Catch:{ all -> 0x010f }
    L_0x004e:
        r2 = r10.valueCount;	 Catch:{ all -> 0x010f }
        if (r1 >= r2) goto L_0x0082;
    L_0x0052:
        r2 = r0.getDirtyFile(r1);	 Catch:{ all -> 0x010f }
        if (r12 == 0) goto L_0x007c;
    L_0x0058:
        r3 = r2.exists();	 Catch:{ all -> 0x010f }
        if (r3 == 0) goto L_0x007f;
    L_0x005e:
        r3 = r0.getCleanFile(r1);	 Catch:{ all -> 0x010f }
        r2.renameTo(r3);	 Catch:{ all -> 0x010f }
        r4 = r0.lengths;	 Catch:{ all -> 0x010f }
        r4 = r4[r1];	 Catch:{ all -> 0x010f }
        r6 = r3.length();	 Catch:{ all -> 0x010f }
        r8 = r0.lengths;	 Catch:{ all -> 0x010f }
        r8[r1] = r6;	 Catch:{ all -> 0x010f }
        r8 = r10.size;	 Catch:{ all -> 0x010f }
        r8 = r8 - r4;
        r8 = r8 + r6;
        r10.size = r8;	 Catch:{ all -> 0x010f }
        goto L_0x007f;
    L_0x007c:
        deleteIfExists(r2);	 Catch:{ all -> 0x010f }
    L_0x007f:
        r1 = r1 + 1;
        goto L_0x004e;
    L_0x0082:
        r1 = r10.redundantOpCount;	 Catch:{ all -> 0x010f }
        r2 = 1;
        r1 = r1 + r2;
        r10.redundantOpCount = r1;	 Catch:{ all -> 0x010f }
        r1 = 0;
        r0.currentEditor = r1;	 Catch:{ all -> 0x010f }
        r1 = r0.readable;	 Catch:{ all -> 0x010f }
        r1 = r1 | r12;
        r3 = 10;
        r4 = 32;
        if (r1 == 0) goto L_0x00ca;
    L_0x0097:
        r0.readable = r2;	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r2 = "CLEAN";
        r1.append(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r1.append(r4);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r2 = r0.key;	 Catch:{ all -> 0x010f }
        r1.append(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r2 = r0.getLengths();	 Catch:{ all -> 0x010f }
        r1.append(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r1.append(r3);	 Catch:{ all -> 0x010f }
        if (r12 == 0) goto L_0x00ed;
    L_0x00bf:
        r1 = r10.nextSequenceNumber;	 Catch:{ all -> 0x010f }
        r3 = 1;
        r3 = r3 + r1;
        r10.nextSequenceNumber = r3;	 Catch:{ all -> 0x010f }
        r0.sequenceNumber = r1;	 Catch:{ all -> 0x010f }
        goto L_0x00ed;
    L_0x00ca:
        r1 = r10.lruEntries;	 Catch:{ all -> 0x010f }
        r2 = r0.key;	 Catch:{ all -> 0x010f }
        r1.remove(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r2 = "REMOVE";
        r1.append(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r1.append(r4);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r2 = r0.key;	 Catch:{ all -> 0x010f }
        r1.append(r2);	 Catch:{ all -> 0x010f }
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r1.append(r3);	 Catch:{ all -> 0x010f }
    L_0x00ed:
        r1 = r10.journalWriter;	 Catch:{ all -> 0x010f }
        r1.flush();	 Catch:{ all -> 0x010f }
        r1 = r10.size;	 Catch:{ all -> 0x010f }
        r3 = r10.maxSize;	 Catch:{ all -> 0x010f }
        r1 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1));
        if (r1 > 0) goto L_0x0100;
    L_0x00fa:
        r1 = r10.journalRebuildRequired();	 Catch:{ all -> 0x010f }
        if (r1 == 0) goto L_0x0107;
    L_0x0100:
        r1 = r10.executorService;	 Catch:{ all -> 0x010f }
        r2 = r10.cleanupCallable;	 Catch:{ all -> 0x010f }
        r1.submit(r2);	 Catch:{ all -> 0x010f }
    L_0x0107:
        monitor-exit(r10);
        return;
    L_0x0109:
        r1 = new java.lang.IllegalStateException;	 Catch:{ all -> 0x010f }
        r1.<init>();	 Catch:{ all -> 0x010f }
        throw r1;	 Catch:{ all -> 0x010f }
    L_0x010f:
        r11 = move-exception;
        monitor-exit(r10);
        throw r11;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.disklrucache.DiskLruCache.completeEdit(com.bumptech.glide.disklrucache.DiskLruCache$Editor, boolean):void");
    }

    private boolean journalRebuildRequired() {
        return this.redundantOpCount >= 2000 && this.redundantOpCount >= this.lruEntries.size();
    }

    /* JADX WARNING: Missing block: B:22:0x008d, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:24:0x008f, code skipped:
            return false;
     */
    public synchronized boolean remove(java.lang.String r8) throws java.io.IOException {
        /*
        r7 = this;
        monitor-enter(r7);
        r7.checkNotClosed();	 Catch:{ all -> 0x0090 }
        r0 = r7.lruEntries;	 Catch:{ all -> 0x0090 }
        r0 = r0.get(r8);	 Catch:{ all -> 0x0090 }
        r0 = (com.bumptech.glide.disklrucache.DiskLruCache.Entry) r0;	 Catch:{ all -> 0x0090 }
        r1 = 0;
        if (r0 == 0) goto L_0x008e;
    L_0x000f:
        r2 = r0.currentEditor;	 Catch:{ all -> 0x0090 }
        if (r2 == 0) goto L_0x0017;
    L_0x0015:
        goto L_0x008e;
    L_0x0018:
        r2 = r7.valueCount;	 Catch:{ all -> 0x0090 }
        if (r1 >= r2) goto L_0x005a;
    L_0x001c:
        r2 = r0.getCleanFile(r1);	 Catch:{ all -> 0x0090 }
        r3 = r2.exists();	 Catch:{ all -> 0x0090 }
        if (r3 == 0) goto L_0x0044;
    L_0x0026:
        r3 = r2.delete();	 Catch:{ all -> 0x0090 }
        if (r3 == 0) goto L_0x002d;
    L_0x002c:
        goto L_0x0044;
    L_0x002d:
        r3 = new java.io.IOException;	 Catch:{ all -> 0x0090 }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0090 }
        r4.<init>();	 Catch:{ all -> 0x0090 }
        r5 = "failed to delete ";
        r4.append(r5);	 Catch:{ all -> 0x0090 }
        r4.append(r2);	 Catch:{ all -> 0x0090 }
        r4 = r4.toString();	 Catch:{ all -> 0x0090 }
        r3.<init>(r4);	 Catch:{ all -> 0x0090 }
        throw r3;	 Catch:{ all -> 0x0090 }
    L_0x0044:
        r3 = r7.size;	 Catch:{ all -> 0x0090 }
        r5 = r0.lengths;	 Catch:{ all -> 0x0090 }
        r5 = r5[r1];	 Catch:{ all -> 0x0090 }
        r3 = r3 - r5;
        r7.size = r3;	 Catch:{ all -> 0x0090 }
        r3 = r0.lengths;	 Catch:{ all -> 0x0090 }
        r4 = 0;
        r3[r1] = r4;	 Catch:{ all -> 0x0090 }
        r1 = r1 + 1;
        goto L_0x0018;
    L_0x005a:
        r1 = r7.redundantOpCount;	 Catch:{ all -> 0x0090 }
        r2 = 1;
        r1 = r1 + r2;
        r7.redundantOpCount = r1;	 Catch:{ all -> 0x0090 }
        r1 = r7.journalWriter;	 Catch:{ all -> 0x0090 }
        r3 = "REMOVE";
        r1.append(r3);	 Catch:{ all -> 0x0090 }
        r1 = r7.journalWriter;	 Catch:{ all -> 0x0090 }
        r3 = 32;
        r1.append(r3);	 Catch:{ all -> 0x0090 }
        r1 = r7.journalWriter;	 Catch:{ all -> 0x0090 }
        r1.append(r8);	 Catch:{ all -> 0x0090 }
        r1 = r7.journalWriter;	 Catch:{ all -> 0x0090 }
        r3 = 10;
        r1.append(r3);	 Catch:{ all -> 0x0090 }
        r1 = r7.lruEntries;	 Catch:{ all -> 0x0090 }
        r1.remove(r8);	 Catch:{ all -> 0x0090 }
        r1 = r7.journalRebuildRequired();	 Catch:{ all -> 0x0090 }
        if (r1 == 0) goto L_0x008c;
    L_0x0085:
        r1 = r7.executorService;	 Catch:{ all -> 0x0090 }
        r3 = r7.cleanupCallable;	 Catch:{ all -> 0x0090 }
        r1.submit(r3);	 Catch:{ all -> 0x0090 }
    L_0x008c:
        monitor-exit(r7);
        return r2;
    L_0x008e:
        monitor-exit(r7);
        return r1;
    L_0x0090:
        r8 = move-exception;
        monitor-exit(r7);
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.disklrucache.DiskLruCache.remove(java.lang.String):boolean");
    }

    public synchronized boolean isClosed() {
        return this.journalWriter == null;
    }

    private void checkNotClosed() {
        if (this.journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    public synchronized void flush() throws IOException {
        checkNotClosed();
        trimToSize();
        this.journalWriter.flush();
    }

    public synchronized void close() throws IOException {
        if (this.journalWriter != null) {
            Iterator it = new ArrayList(this.lruEntries.values()).iterator();
            while (it.hasNext()) {
                Entry entry = (Entry) it.next();
                if (entry.currentEditor != null) {
                    entry.currentEditor.abort();
                }
            }
            trimToSize();
            this.journalWriter.close();
            this.journalWriter = null;
        }
    }

    private void trimToSize() throws IOException {
        while (this.size > this.maxSize) {
            remove((String) ((java.util.Map.Entry) this.lruEntries.entrySet().iterator().next()).getKey());
        }
    }

    public void delete() throws IOException {
        close();
        Util.deleteContents(this.directory);
    }

    private static String inputStreamToString(InputStream in) throws IOException {
        return Util.readFully(new InputStreamReader(in, Util.UTF_8));
    }
}
