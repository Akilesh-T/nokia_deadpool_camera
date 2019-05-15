package com.google.common.hash;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

final class MessageDigestHashFunction extends AbstractStreamingHashFunction implements Serializable {
    private final int bytes;
    private final MessageDigest prototype;
    private final boolean supportsClone;
    private final String toString;

    private static final class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        private final String algorithmName;
        private final int bytes;
        private final String toString;

        private SerializedForm(String algorithmName, int bytes, String toString) {
            this.algorithmName = algorithmName;
            this.bytes = bytes;
            this.toString = toString;
        }

        private Object readResolve() {
            return new MessageDigestHashFunction(this.algorithmName, this.bytes, this.toString);
        }
    }

    private static final class MessageDigestHasher extends AbstractByteHasher {
        private final int bytes;
        private final MessageDigest digest;
        private boolean done;

        private MessageDigestHasher(MessageDigest digest, int bytes) {
            this.digest = digest;
            this.bytes = bytes;
        }

        /* Access modifiers changed, original: protected */
        public void update(byte b) {
            checkNotDone();
            this.digest.update(b);
        }

        /* Access modifiers changed, original: protected */
        public void update(byte[] b) {
            checkNotDone();
            this.digest.update(b);
        }

        /* Access modifiers changed, original: protected */
        public void update(byte[] b, int off, int len) {
            checkNotDone();
            this.digest.update(b, off, len);
        }

        private void checkNotDone() {
            Preconditions.checkState(this.done ^ 1, "Cannot re-use a Hasher after calling hash() on it");
        }

        public HashCode hash() {
            checkNotDone();
            this.done = true;
            if (this.bytes == this.digest.getDigestLength()) {
                return HashCode.fromBytesNoCopy(this.digest.digest());
            }
            return HashCode.fromBytesNoCopy(Arrays.copyOf(this.digest.digest(), this.bytes));
        }
    }

    MessageDigestHashFunction(String algorithmName, String toString) {
        this.prototype = getMessageDigest(algorithmName);
        this.bytes = this.prototype.getDigestLength();
        this.toString = (String) Preconditions.checkNotNull(toString);
        this.supportsClone = supportsClone();
    }

    MessageDigestHashFunction(String algorithmName, int bytes, String toString) {
        this.toString = (String) Preconditions.checkNotNull(toString);
        this.prototype = getMessageDigest(algorithmName);
        boolean z = bytes >= 4 && bytes <= this.prototype.getDigestLength();
        Preconditions.checkArgument(z, "bytes (%s) must be >= 4 and < %s", Integer.valueOf(bytes), Integer.valueOf(maxLength));
        this.bytes = bytes;
        this.supportsClone = supportsClone();
    }

    private boolean supportsClone() {
        try {
            this.prototype.clone();
            return true;
        } catch (CloneNotSupportedException e) {
            return false;
        }
    }

    public int bits() {
        return this.bytes * 8;
    }

    public String toString() {
        return this.toString;
    }

    private static MessageDigest getMessageDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public Hasher newHasher() {
        if (this.supportsClone) {
            try {
                return new MessageDigestHasher((MessageDigest) this.prototype.clone(), this.bytes);
            } catch (CloneNotSupportedException e) {
            }
        }
        return new MessageDigestHasher(getMessageDigest(this.prototype.getAlgorithm()), this.bytes);
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new SerializedForm(this.prototype.getAlgorithm(), this.bytes, this.toString);
    }
}
