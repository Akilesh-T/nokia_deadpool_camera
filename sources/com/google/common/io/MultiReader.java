package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import javax.annotation.Nullable;

class MultiReader extends Reader {
    private Reader current;
    private final Iterator<? extends CharSource> it;

    MultiReader(Iterator<? extends CharSource> readers) throws IOException {
        this.it = readers;
        advance();
    }

    private void advance() throws IOException {
        close();
        if (this.it.hasNext()) {
            this.current = ((CharSource) this.it.next()).openStream();
        }
    }

    public int read(@Nullable char[] cbuf, int off, int len) throws IOException {
        if (this.current == null) {
            return -1;
        }
        int result = this.current.read(cbuf, off, len);
        if (result != -1) {
            return result;
        }
        advance();
        return read(cbuf, off, len);
    }

    public long skip(long n) throws IOException {
        Preconditions.checkArgument(n >= 0, "n is negative");
        if (n > 0) {
            while (this.current != null) {
                long result = this.current.skip(n);
                if (result > 0) {
                    return result;
                }
                advance();
            }
        }
        return 0;
    }

    public boolean ready() throws IOException {
        return this.current != null && this.current.ready();
    }

    public void close() throws IOException {
        if (this.current != null) {
            try {
                this.current.close();
            } finally {
                this.current = null;
            }
        }
    }
}
