package com.hmdglobal.app.camera.beauty.cameragl;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RenderMessageBean {
    private static volatile RenderMessageBean instance = new RenderMessageBean();
    public long height;
    private HashMap<String, TraceInfo> mTraceTimeMap = new HashMap();
    private Executor singleThread = Executors.newFixedThreadPool(4);
    public long width;

    private static class TraceInfo {
        private TraceInfo() {
        }

        /* synthetic */ TraceInfo(AnonymousClass1 x0) {
            this();
        }
    }

    private static class FPSTraceInfo extends TraceInfo {
        long averageTime;
        long count;
        long fps;
        long startTime;
        String tag;

        private FPSTraceInfo() {
            super();
            this.startTime = System.currentTimeMillis();
        }

        /* synthetic */ FPSTraceInfo(AnonymousClass1 x0) {
            this();
        }

        private void update() {
            this.count++;
            if (this.count == 20) {
                long oldTime = this.startTime;
                this.startTime = System.currentTimeMillis();
                long interval = this.startTime - oldTime;
                this.fps = (this.count * 1000) / interval;
                this.averageTime = interval / this.count;
                this.count = 0;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.tag);
            sb.append(".");
            sb.append("fps :");
            sb.append(this.fps);
            return sb.toString();
        }
    }

    private static class TimeTraceInfo extends TraceInfo {
        long averageTime;
        long count;
        long spendTime;
        long startTime;
        String tag;

        private TimeTraceInfo() {
            super();
        }

        /* synthetic */ TimeTraceInfo(AnonymousClass1 x0) {
            this();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.tag);
            sb.append(".");
            sb.append("average :");
            sb.append(this.averageTime);
            return sb.toString();
        }
    }

    public static RenderMessageBean getInstance() {
        return instance;
    }

    private synchronized void sendMessage() {
    }

    private synchronized String getInfo() {
        StringBuilder sb;
        sb = new StringBuilder();
        String normalInfo = new StringBuilder();
        normalInfo.append("resolution=");
        normalInfo.append(this.width);
        normalInfo.append("*");
        normalInfo.append(this.height);
        normalInfo.append("\n");
        sb.append(normalInfo.toString());
        sb.append("\n");
        for (Entry<String, TraceInfo> entry : this.mTraceTimeMap.entrySet()) {
            sb.append(((TraceInfo) entry.getValue()).toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void traceFps() {
        traceFps(getMethodName());
    }

    private String getMethodName() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }

    public synchronized void traceFps(final String tag) {
        this.singleThread.execute(new Runnable() {
            public void run() {
                HashMap access$000 = RenderMessageBean.this.mTraceTimeMap;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(tag);
                stringBuilder.append(".fps");
                TraceInfo traceInfo = (TraceInfo) access$000.get(stringBuilder.toString());
                if (traceInfo == null) {
                    traceInfo = new FPSTraceInfo();
                    HashMap access$0002 = RenderMessageBean.this.mTraceTimeMap;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(tag);
                    stringBuilder2.append(".fps");
                    access$0002.put(stringBuilder2.toString(), traceInfo);
                }
                FPSTraceInfo FPSTraceInfo = (FPSTraceInfo) traceInfo;
                FPSTraceInfo.tag = tag;
                FPSTraceInfo.update();
                RenderMessageBean.this.sendMessage();
            }
        });
    }

    public synchronized void traceStart(final String tag) {
        this.singleThread.execute(new Runnable() {
            public void run() {
                HashMap access$000 = RenderMessageBean.this.mTraceTimeMap;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(tag);
                stringBuilder.append(".interval");
                TraceInfo traceInfo = (TraceInfo) access$000.get(stringBuilder.toString());
                if (traceInfo == null) {
                    traceInfo = new TimeTraceInfo();
                    HashMap access$0002 = RenderMessageBean.this.mTraceTimeMap;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(tag);
                    stringBuilder2.append(".interval");
                    access$0002.put(stringBuilder2.toString(), traceInfo);
                }
                TimeTraceInfo info = (TimeTraceInfo) traceInfo;
                info.tag = tag;
                info.startTime = System.currentTimeMillis();
            }
        });
    }

    public synchronized void traceEnd(final String tag) {
        this.singleThread.execute(new Runnable() {
            public void run() {
                HashMap access$000 = RenderMessageBean.this.mTraceTimeMap;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(tag);
                stringBuilder.append(".interval");
                TraceInfo traceInfo = (TraceInfo) access$000.get(stringBuilder.toString());
                if (traceInfo == null) {
                    traceInfo = new TimeTraceInfo();
                    HashMap access$0002 = RenderMessageBean.this.mTraceTimeMap;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(tag);
                    stringBuilder2.append(".interval");
                    access$0002.put(stringBuilder2.toString(), traceInfo);
                }
                TimeTraceInfo info = (TimeTraceInfo) traceInfo;
                info.tag = tag;
                info.spendTime += System.currentTimeMillis() - info.startTime;
                info.count++;
                if (info.count == 20) {
                    info.averageTime = info.spendTime / info.count;
                    info.spendTime = 0;
                    info.count = 0;
                }
                RenderMessageBean.this.sendMessage();
            }
        });
    }
}
