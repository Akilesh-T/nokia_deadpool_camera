package com.android.ex.camera2.portability;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.Iterator;
import java.util.LinkedList;

class HistoryHandler extends Handler {
    private static final int MAX_HISTORY_SIZE = 400;
    final LinkedList<Integer> mMsgHistory = new LinkedList();

    HistoryHandler(Looper looper) {
        super(looper);
        this.mMsgHistory.offerLast(Integer.valueOf(-1));
    }

    /* Access modifiers changed, original: 0000 */
    public Integer getCurrentMessage() {
        return (Integer) this.mMsgHistory.peekLast();
    }

    /* Access modifiers changed, original: 0000 */
    public String generateHistoryString(int cameraId) {
        String info = new String("HIST");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(info);
        stringBuilder.append("_ID");
        stringBuilder.append(cameraId);
        info = stringBuilder.toString();
        Iterator it = this.mMsgHistory.iterator();
        while (it.hasNext()) {
            Integer msg = (Integer) it.next();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(info);
            stringBuilder2.append('_');
            stringBuilder2.append(msg.toString());
            info = stringBuilder2.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(info);
        stringBuilder.append("_HEND");
        return stringBuilder.toString();
    }

    public void handleMessage(Message msg) {
        this.mMsgHistory.offerLast(Integer.valueOf(msg.what));
        while (this.mMsgHistory.size() > 400) {
            this.mMsgHistory.pollFirst();
        }
    }
}
