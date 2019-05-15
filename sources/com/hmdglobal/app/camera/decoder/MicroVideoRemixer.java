package com.hmdglobal.app.camera.decoder;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.encoder.MicroVideoMuxer;
import com.hmdglobal.app.camera.encoder.Muxer;
import com.hmdglobal.app.camera.encoder.Muxer.MuxerBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Pattern;

public class MicroVideoRemixer extends Remixer {
    private static final String AUDIO_MIME_TYPE = "audio";
    private static final int BUF_CAP = 4147200;
    private static final int INVALID_TRACK = -1;
    private static final String VIDEO_MIME_TYPE = "video";
    private Tag TAG = new Tag("MicroVideoRemixer");
    private int mAudioTrack = -1;
    private MediaExtractor mExtractor;
    private Muxer mMuxer;
    private int mOrientation = 0;
    private String mOutputPath;
    private Paths mPaths;
    private long mTimeStamp = 0;
    private int mVideoTrack = -1;

    private static class Paths {
        private int mIndicator = 0;
        private List<String> mPaths;

        public Paths(List<String> paths) {
            this.mPaths = paths;
        }

        public String getAvailablePath() {
            if (this.mPaths == null || this.mPaths.size() == 0 || this.mIndicator >= this.mPaths.size()) {
                return null;
            }
            List list = this.mPaths;
            int i = this.mIndicator;
            this.mIndicator = i + 1;
            return (String) list.get(i);
        }

        public void reset() {
            this.mIndicator = 0;
        }
    }

    private class StitcherTask extends AsyncTask<Void, Void, Void> {
        private StitcherTask() {
        }

        /* Access modifiers changed, original: protected|varargs */
        public Void doInBackground(Void... params) {
            Log.w(MicroVideoRemixer.this.TAG, "decoding video");
            MicroVideoRemixer.this.doDecoding(Track.VIDEO);
            Log.w(MicroVideoRemixer.this.TAG, "decoding audio");
            MicroVideoRemixer.this.mPaths.reset();
            MicroVideoRemixer.this.doDecoding(Track.AUDIO);
            MicroVideoRemixer.this.mMuxer.stopMuxer();
            if (MicroVideoRemixer.this.mListener != null) {
                MicroVideoRemixer.this.mListener.onRemixDone();
            }
            return null;
        }
    }

    private enum Track {
        VIDEO,
        AUDIO
    }

    public void prepareForRemixer(String outputPath, List<String> paths) {
        this.mPaths = new Paths(paths);
        this.mOutputPath = outputPath;
    }

    private void initExtractor(Track type) {
        updateExtractorSource(type);
        if (this.mMuxer == null) {
            prepareMuxer();
        }
    }

    private boolean updateExtractorSource(Track type) {
        String path = this.mPaths.getAvailablePath();
        if (path == null) {
            return false;
        }
        if (this.mExtractor != null) {
            this.mExtractor.release();
            this.mExtractor = null;
        }
        try {
            this.mExtractor = new MediaExtractor();
            this.mExtractor.setDataSource(path);
            selectTrack(type);
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateExtractorSource for ");
            stringBuilder.append(type.name());
            Log.w(tag, stringBuilder.toString());
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Source not invalidate");
        }
    }

    private MediaFormat selectTrack(Track type) {
        int tractCount = this.mExtractor.getTrackCount();
        for (int i = 0; i < tractCount; i++) {
            MediaFormat format = this.mExtractor.getTrackFormat(i);
            String mime = format.getString("mime");
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mime type is ");
            stringBuilder.append(mime);
            Log.w(tag, stringBuilder.toString());
            if (isMIMEMatch(mime, type == Track.VIDEO ? VIDEO_MIME_TYPE : AUDIO_MIME_TYPE)) {
                this.mExtractor.selectTrack(i);
                Tag tag2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("expect track is ");
                stringBuilder2.append(i);
                Log.w(tag2, stringBuilder2.toString());
                return format;
            }
        }
        return null;
    }

    private MediaFormat getTrackFromat(Track type) {
        int tractCount = this.mExtractor.getTrackCount();
        for (int i = 0; i < tractCount; i++) {
            MediaFormat format = this.mExtractor.getTrackFormat(i);
            String mime = format.getString("mime");
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mime type is ");
            stringBuilder.append(mime);
            Log.w(tag, stringBuilder.toString());
            if (isMIMEMatch(mime, type == Track.VIDEO ? VIDEO_MIME_TYPE : AUDIO_MIME_TYPE)) {
                Tag tag2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("expect track is ");
                stringBuilder2.append(i);
                Log.w(tag2, stringBuilder2.toString());
                return format;
            }
        }
        return null;
    }

    private boolean isMIMEMatch(String mime, String pattern) {
        return Pattern.compile(pattern).matcher(mime).find();
    }

    public void startRemix() {
        new StitcherTask().execute(new Void[0]);
    }

    public void setDisplayOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    private void prepareMuxer() {
        this.mMuxer = new MicroVideoMuxer();
        try {
            this.mMuxer.prepareMuxer(this.mOutputPath);
        } catch (IOException e) {
            Log.w(this.TAG, "muxer output path invalidate");
        }
        if (this.mVideoTrack == -1 || this.mAudioTrack == -1) {
            MediaFormat videoFormat = getTrackFromat(Track.VIDEO);
            MediaFormat audioFormat = getTrackFromat(Track.AUDIO);
            this.mVideoTrack = this.mMuxer.addTrack(videoFormat);
            this.mAudioTrack = this.mMuxer.addTrack(audioFormat);
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("muxer orientation is ");
            stringBuilder.append(this.mOrientation);
            Log.w(tag, stringBuilder.toString());
            this.mMuxer.setOrientationHint(this.mOrientation);
            this.mMuxer.startMuxer();
        }
    }

    private void doDecoding(Track type) {
        initExtractor(type);
        ByteBuffer inputBuffer = ByteBuffer.allocate(BUF_CAP);
        long currentBaseTimestamp = 0;
        while (true) {
            int size;
            inputBuffer.clear();
            int size2 = this.mExtractor.readSampleData(inputBuffer, 0);
            if (size2 > 0 && this.mExtractor.getSampleFlags() != 4) {
                size = size2;
            } else if (updateExtractorSource(type)) {
                long currentBaseTimestamp2 = this.mTimeStamp + (1000000 / 30);
                size = this.mExtractor.readSampleData(inputBuffer, 0);
                currentBaseTimestamp = currentBaseTimestamp2;
            } else {
                Track track = type;
                this.mVideoTrack = -1;
                this.mTimeStamp = 0;
                return;
            }
            updateTimestamp(currentBaseTimestamp);
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTimestamp is ");
            stringBuilder.append(this.mTimeStamp);
            Log.w(tag, stringBuilder.toString());
            BufferInfo bufferInfo = new BufferInfo();
            bufferInfo.set(0, size, this.mTimeStamp, this.mExtractor.getSampleFlags());
            this.mMuxer.writeData(new MuxerBuffer(type == Track.VIDEO ? this.mVideoTrack : this.mAudioTrack, inputBuffer, bufferInfo));
            this.mExtractor.advance();
        }
    }

    private void updateTimestamp(long currentBaseTimestamp) {
        if (this.mTimeStamp == 0) {
            this.mTimeStamp = this.mExtractor.getSampleTime();
        } else if (this.mTimeStamp <= this.mExtractor.getSampleTime()) {
            this.mTimeStamp = this.mExtractor.getSampleTime();
        } else {
            this.mTimeStamp = this.mExtractor.getSampleTime() + currentBaseTimestamp;
        }
    }

    public void releaseRemixer() {
        if (this.mExtractor != null) {
            this.mExtractor.release();
            this.mExtractor = null;
        }
    }
}
