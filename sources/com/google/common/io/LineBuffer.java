package com.google.common.io;

import java.io.IOException;

abstract class LineBuffer {
    private StringBuilder line = new StringBuilder();
    private boolean sawReturn;

    public abstract void handleLine(String str, String str2) throws IOException;

    LineBuffer() {
    }

    /* Access modifiers changed, original: protected */
    public void add(char[] cbuf, int off, int len) throws IOException {
        int pos = off;
        if (this.sawReturn && len > 0) {
            if (finishLine(cbuf[pos] == 10)) {
                pos++;
            }
        }
        int start = pos;
        int end = off + len;
        while (pos < end) {
            char c = cbuf[pos];
            if (c == 10) {
                this.line.append(cbuf, start, pos - start);
                finishLine(true);
                start = pos + 1;
            } else if (c == 13) {
                this.line.append(cbuf, start, pos - start);
                this.sawReturn = true;
                if (pos + 1 < end) {
                    if (finishLine(cbuf[pos + 1] == 10)) {
                        pos++;
                    }
                }
                start = pos + 1;
            }
            pos++;
        }
        this.line.append(cbuf, start, (off + len) - start);
    }

    private boolean finishLine(boolean sawNewline) throws IOException {
        String stringBuilder = this.line.toString();
        String str = this.sawReturn ? sawNewline ? "\r\n" : "\r" : sawNewline ? "\n" : "";
        handleLine(stringBuilder, str);
        this.line = new StringBuilder();
        this.sawReturn = false;
        return sawNewline;
    }

    /* Access modifiers changed, original: protected */
    public void finish() throws IOException {
        if (this.sawReturn || this.line.length() > 0) {
            finishLine(false);
        }
    }
}
