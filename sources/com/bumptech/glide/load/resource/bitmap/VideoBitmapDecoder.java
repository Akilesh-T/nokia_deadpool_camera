package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.io.IOException;

public class VideoBitmapDecoder implements BitmapDecoder<ParcelFileDescriptor> {
    private static final MediaMetadataRetrieverFactory DEFAULT_FACTORY = new MediaMetadataRetrieverFactory();
    private MediaMetadataRetrieverFactory factory;

    static class MediaMetadataRetrieverFactory {
        MediaMetadataRetrieverFactory() {
        }

        public MediaMetadataRetriever build() {
            return new MediaMetadataRetriever();
        }
    }

    public VideoBitmapDecoder() {
        this(DEFAULT_FACTORY);
    }

    VideoBitmapDecoder(MediaMetadataRetrieverFactory factory) {
        this.factory = factory;
    }

    public Bitmap decode(ParcelFileDescriptor resource, BitmapPool bitmapPool, int outWidth, int outHeight, DecodeFormat decodeFormat) throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = this.factory.build();
        mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
        Bitmap result = mediaMetadataRetriever.getFrameAtTime();
        mediaMetadataRetriever.release();
        resource.close();
        return result;
    }

    public String getId() {
        return "VideoBitmapDecoder.com.bumptech.glide.load.resource.bitmap";
    }
}
