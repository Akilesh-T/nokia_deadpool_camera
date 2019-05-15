package com.bumptech.glide.load.engine;

import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCache.Writer;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.util.LogTime;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class DecodeJob<A, T, Z> {
    private static final FileOpener DEFAULT_FILE_OPENER = new FileOpener();
    private static final String TAG = "DecodeJob";
    private final DiskCache diskCache;
    private final DiskCacheStrategy diskCacheStrategy;
    private final DataFetcher<A> fetcher;
    private final FileOpener fileOpener;
    private final int height;
    private volatile boolean isCancelled;
    private final DataLoadProvider<A, T> loadProvider;
    private final Priority priority;
    private final EngineKey resultKey;
    private final ResourceTranscoder<T, Z> transcoder;
    private final Transformation<T> transformation;
    private final int width;

    static class FileOpener {
        FileOpener() {
        }

        public OutputStream open(File file) throws FileNotFoundException {
            return new BufferedOutputStream(new FileOutputStream(file));
        }
    }

    class SourceWriter<DataType> implements Writer {
        private final DataType data;
        private final Encoder<DataType> encoder;

        public SourceWriter(Encoder<DataType> encoder, DataType data) {
            this.encoder = encoder;
            this.data = data;
        }

        public boolean write(File file) {
            boolean success = false;
            OutputStream os = null;
            try {
                os = DecodeJob.this.fileOpener.open(file);
                success = this.encoder.encode(this.data, os);
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            } catch (FileNotFoundException e2) {
                if (Log.isLoggable(DecodeJob.TAG, 3)) {
                    Log.d(DecodeJob.TAG, "Failed to find file to write to disk cache", e2);
                }
                if (os != null) {
                    os.close();
                }
            } catch (Throwable th) {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e3) {
                    }
                }
            }
            return success;
        }
    }

    public DecodeJob(EngineKey resultKey, int width, int height, DataFetcher<A> fetcher, DataLoadProvider<A, T> loadProvider, Transformation<T> transformation, ResourceTranscoder<T, Z> transcoder, DiskCache diskCache, DiskCacheStrategy diskCacheStrategy, Priority priority) {
        this(resultKey, width, height, fetcher, loadProvider, transformation, transcoder, diskCache, diskCacheStrategy, priority, DEFAULT_FILE_OPENER);
    }

    DecodeJob(EngineKey resultKey, int width, int height, DataFetcher<A> fetcher, DataLoadProvider<A, T> loadProvider, Transformation<T> transformation, ResourceTranscoder<T, Z> transcoder, DiskCache diskCache, DiskCacheStrategy diskCacheStrategy, Priority priority, FileOpener fileOpener) {
        this.resultKey = resultKey;
        this.width = width;
        this.height = height;
        this.fetcher = fetcher;
        this.loadProvider = loadProvider;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.diskCacheStrategy = diskCacheStrategy;
        this.diskCache = diskCache;
        this.priority = priority;
        this.fileOpener = fileOpener;
    }

    public Resource<Z> decodeResultFromCache() throws Exception {
        if (!this.diskCacheStrategy.cacheResult()) {
            return null;
        }
        long startTime = LogTime.getLogTime();
        Resource<T> transformed = loadFromCache(this.resultKey);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Decoded transformed from cache", startTime);
        }
        startTime = LogTime.getLogTime();
        Resource<Z> result = transcode(transformed);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Transcoded transformed from cache", startTime);
        }
        return result;
    }

    public Resource<Z> decodeSourceFromCache() throws Exception {
        if (!this.diskCacheStrategy.cacheSource()) {
            return null;
        }
        long startTime = LogTime.getLogTime();
        Resource<T> decoded = loadFromCache(this.resultKey.getOriginalKey());
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Decoded source from cache", startTime);
        }
        return transformEncodeAndTranscode(decoded);
    }

    public Resource<Z> decodeFromSource() throws Exception {
        return transformEncodeAndTranscode(decodeSource());
    }

    public void cancel() {
        this.fetcher.cancel();
        this.isCancelled = true;
    }

    private Resource<Z> transformEncodeAndTranscode(Resource<T> decoded) {
        long startTime = LogTime.getLogTime();
        Resource<T> transformed = transform(decoded);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Transformed resource from source", startTime);
        }
        writeTransformedToCache(transformed);
        startTime = LogTime.getLogTime();
        Resource<Z> result = transcode(transformed);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Transcoded transformed from source", startTime);
        }
        return result;
    }

    private void writeTransformedToCache(Resource<T> transformed) {
        if (transformed != null && this.diskCacheStrategy.cacheResult()) {
            long startTime = LogTime.getLogTime();
            this.diskCache.put(this.resultKey, new SourceWriter(this.loadProvider.getEncoder(), transformed));
            if (Log.isLoggable(TAG, 2)) {
                logWithTimeAndKey("Wrote transformed from source to cache", startTime);
            }
        }
    }

    private Resource<T> decodeSource() throws Exception {
        Resource<T> decoded = null;
        try {
            long startTime = LogTime.getLogTime();
            A data = this.fetcher.loadData(this.priority);
            if (Log.isLoggable(TAG, 2)) {
                logWithTimeAndKey("Fetched data", startTime);
            }
            if (this.isCancelled) {
                return null;
            }
            Resource<T> decoded2 = decodeFromSourceData(data);
            this.fetcher.cleanup();
            return decoded2;
        } finally {
            this.fetcher.cleanup();
        }
    }

    private Resource<T> decodeFromSourceData(A data) throws IOException {
        if (this.diskCacheStrategy.cacheSource()) {
            return cacheAndDecodeSourceData(data);
        }
        long startTime = LogTime.getLogTime();
        Resource<T> decoded = this.loadProvider.getSourceDecoder().decode(data, this.width, this.height);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Decoded from source", startTime);
        }
        return decoded;
    }

    private Resource<T> cacheAndDecodeSourceData(A data) throws IOException {
        long startTime = LogTime.getLogTime();
        this.diskCache.put(this.resultKey.getOriginalKey(), new SourceWriter(this.loadProvider.getSourceEncoder(), data));
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Wrote source to cache", startTime);
        }
        startTime = LogTime.getLogTime();
        Resource<T> result = loadFromCache(this.resultKey.getOriginalKey());
        if (Log.isLoggable(TAG, 2) && result != null) {
            logWithTimeAndKey("Decoded source from cache", startTime);
        }
        return result;
    }

    private Resource<T> loadFromCache(Key key) throws IOException {
        File cacheFile = this.diskCache.get(key);
        Resource<T> result = null;
        if (cacheFile == null) {
            return null;
        }
        try {
            result = this.loadProvider.getCacheDecoder().decode(cacheFile, this.width, this.height);
            return result;
        } finally {
            if (result == null) {
                this.diskCache.delete(key);
            }
        }
    }

    private Resource<T> transform(Resource<T> decoded) {
        if (decoded == null) {
            return null;
        }
        Resource<T> transformed = this.transformation.transform(decoded, this.width, this.height);
        if (!decoded.equals(transformed)) {
            decoded.recycle();
        }
        return transformed;
    }

    private Resource<Z> transcode(Resource<T> transformed) {
        if (transformed == null) {
            return null;
        }
        return this.transcoder.transcode(transformed);
    }

    private void logWithTimeAndKey(String message, long startTime) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(" in ");
        stringBuilder.append(LogTime.getElapsedMillis(startTime));
        stringBuilder.append(this.resultKey);
        Log.v(str, stringBuilder.toString());
    }
}
