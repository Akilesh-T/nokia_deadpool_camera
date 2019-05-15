package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class RequestTracker {
    private boolean isPaused;
    private final Set<Request> requests = Collections.newSetFromMap(new WeakHashMap());

    public void runRequest(Request request) {
        this.requests.add(request);
        if (!this.isPaused) {
            request.begin();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void addRequest(Request request) {
        this.requests.add(request);
    }

    public void removeRequest(Request request) {
        this.requests.remove(request);
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public void pauseRequests() {
        this.isPaused = true;
        for (Request request : this.requests) {
            if (request.isRunning()) {
                request.pause();
            }
        }
    }

    public void resumeRequests() {
        this.isPaused = false;
        for (Request request : this.requests) {
            if (!(request.isComplete() || request.isCancelled() || request.isRunning())) {
                request.begin();
            }
        }
    }

    public void clearRequests() {
        for (Request request : this.requests) {
            request.clear();
        }
    }

    public void restartRequests() {
        for (Request request : this.requests) {
            if (!(request.isComplete() || request.isCancelled())) {
                request.pause();
                if (!this.isPaused) {
                    request.begin();
                }
            }
        }
    }
}
