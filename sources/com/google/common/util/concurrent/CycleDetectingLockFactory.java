package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Beta
public class CycleDetectingLockFactory {
    private static final ThreadLocal<ArrayList<LockGraphNode>> acquiredLocks = new ThreadLocal<ArrayList<LockGraphNode>>() {
        /* Access modifiers changed, original: protected */
        public ArrayList<LockGraphNode> initialValue() {
            return Lists.newArrayListWithCapacity(3);
        }
    };
    private static final ConcurrentMap<Class<? extends Enum>, Map<? extends Enum, LockGraphNode>> lockGraphNodesPerType = new MapMaker().weakKeys().makeMap();
    private static final Logger logger = Logger.getLogger(CycleDetectingLockFactory.class.getName());
    final Policy policy;

    private interface CycleDetectingLock {
        LockGraphNode getLockGraphNode();

        boolean isAcquiredByCurrentThread();
    }

    private class CycleDetectingReentrantReadLock extends ReadLock {
        final CycleDetectingReentrantReadWriteLock readWriteLock;

        CycleDetectingReentrantReadLock(CycleDetectingReentrantReadWriteLock readWriteLock) {
            super(readWriteLock);
            this.readWriteLock = readWriteLock;
        }

        public void lock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                super.lock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                super.lockInterruptibly();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public boolean tryLock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                boolean tryLock = super.tryLock();
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                boolean tryLock = super.tryLock(timeout, unit);
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public void unlock() {
            try {
                super.unlock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }
    }

    private class CycleDetectingReentrantWriteLock extends WriteLock {
        final CycleDetectingReentrantReadWriteLock readWriteLock;

        CycleDetectingReentrantWriteLock(CycleDetectingReentrantReadWriteLock readWriteLock) {
            super(readWriteLock);
            this.readWriteLock = readWriteLock;
        }

        public void lock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                super.lock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                super.lockInterruptibly();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public boolean tryLock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                boolean tryLock = super.tryLock();
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this.readWriteLock);
            try {
                boolean tryLock = super.tryLock(timeout, unit);
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }

        public void unlock() {
            try {
                super.unlock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this.readWriteLock);
            }
        }
    }

    private static class ExampleStackTrace extends IllegalStateException {
        static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];
        static Set<String> EXCLUDED_CLASS_NAMES = ImmutableSet.of(CycleDetectingLockFactory.class.getName(), ExampleStackTrace.class.getName(), LockGraphNode.class.getName());

        ExampleStackTrace(LockGraphNode node1, LockGraphNode node2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(node1.getLockName());
            stringBuilder.append(" -> ");
            stringBuilder.append(node2.getLockName());
            super(stringBuilder.toString());
            StackTraceElement[] origStackTrace = getStackTrace();
            int i = 0;
            int n = origStackTrace.length;
            while (i < n) {
                if (WithExplicitOrdering.class.getName().equals(origStackTrace[i].getClassName())) {
                    setStackTrace(EMPTY_STACK_TRACE);
                    return;
                } else if (EXCLUDED_CLASS_NAMES.contains(origStackTrace[i].getClassName())) {
                    i++;
                } else {
                    setStackTrace((StackTraceElement[]) Arrays.copyOfRange(origStackTrace, i, n));
                    return;
                }
            }
        }
    }

    private static class LockGraphNode {
        final Map<LockGraphNode, ExampleStackTrace> allowedPriorLocks = new MapMaker().weakKeys().makeMap();
        final Map<LockGraphNode, PotentialDeadlockException> disallowedPriorLocks = new MapMaker().weakKeys().makeMap();
        final String lockName;

        LockGraphNode(String lockName) {
            this.lockName = (String) Preconditions.checkNotNull(lockName);
        }

        /* Access modifiers changed, original: 0000 */
        public String getLockName() {
            return this.lockName;
        }

        /* Access modifiers changed, original: 0000 */
        public void checkAcquiredLocks(Policy policy, List<LockGraphNode> acquiredLocks) {
            int size = acquiredLocks.size();
            for (int i = 0; i < size; i++) {
                checkAcquiredLock(policy, (LockGraphNode) acquiredLocks.get(i));
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void checkAcquiredLock(Policy policy, LockGraphNode acquiredLock) {
            boolean z = this != acquiredLock;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted to acquire multiple locks with the same rank ");
            stringBuilder.append(acquiredLock.getLockName());
            Preconditions.checkState(z, stringBuilder.toString());
            if (!this.allowedPriorLocks.containsKey(acquiredLock)) {
                PotentialDeadlockException previousDeadlockException = (PotentialDeadlockException) this.disallowedPriorLocks.get(acquiredLock);
                if (previousDeadlockException != null) {
                    policy.handlePotentialDeadlock(new PotentialDeadlockException(acquiredLock, this, previousDeadlockException.getConflictingStackTrace(), null));
                    return;
                }
                ExampleStackTrace path = acquiredLock.findPathTo(this, Sets.newIdentityHashSet());
                if (path == null) {
                    this.allowedPriorLocks.put(acquiredLock, new ExampleStackTrace(acquiredLock, this));
                } else {
                    PotentialDeadlockException exception = new PotentialDeadlockException(acquiredLock, this, path, null);
                    this.disallowedPriorLocks.put(acquiredLock, exception);
                    policy.handlePotentialDeadlock(exception);
                }
            }
        }

        @Nullable
        private ExampleStackTrace findPathTo(LockGraphNode node, Set<LockGraphNode> seen) {
            if (!seen.add(this)) {
                return null;
            }
            ExampleStackTrace found = (ExampleStackTrace) this.allowedPriorLocks.get(node);
            if (found != null) {
                return found;
            }
            for (Entry<LockGraphNode, ExampleStackTrace> entry : this.allowedPriorLocks.entrySet()) {
                LockGraphNode preAcquiredLock = (LockGraphNode) entry.getKey();
                found = preAcquiredLock.findPathTo(node, seen);
                if (found != null) {
                    ExampleStackTrace path = new ExampleStackTrace(preAcquiredLock, this);
                    path.setStackTrace(((ExampleStackTrace) entry.getValue()).getStackTrace());
                    path.initCause(found);
                    return path;
                }
            }
            return null;
        }
    }

    @ThreadSafe
    @Beta
    public interface Policy {
        void handlePotentialDeadlock(PotentialDeadlockException potentialDeadlockException);
    }

    final class CycleDetectingReentrantLock extends ReentrantLock implements CycleDetectingLock {
        private final LockGraphNode lockGraphNode;

        /* synthetic */ CycleDetectingReentrantLock(CycleDetectingLockFactory x0, LockGraphNode x1, boolean x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private CycleDetectingReentrantLock(LockGraphNode lockGraphNode, boolean fair) {
            super(fair);
            this.lockGraphNode = (LockGraphNode) Preconditions.checkNotNull(lockGraphNode);
        }

        public LockGraphNode getLockGraphNode() {
            return this.lockGraphNode;
        }

        public boolean isAcquiredByCurrentThread() {
            return isHeldByCurrentThread();
        }

        public void lock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this);
            try {
                super.lock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this);
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this);
            try {
                super.lockInterruptibly();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this);
            }
        }

        public boolean tryLock() {
            CycleDetectingLockFactory.this.aboutToAcquire(this);
            try {
                boolean tryLock = super.tryLock();
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this);
            }
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            CycleDetectingLockFactory.this.aboutToAcquire(this);
            try {
                boolean tryLock = super.tryLock(timeout, unit);
                return tryLock;
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this);
            }
        }

        public void unlock() {
            try {
                super.unlock();
            } finally {
                CycleDetectingLockFactory.this.lockStateChanged(this);
            }
        }
    }

    final class CycleDetectingReentrantReadWriteLock extends ReentrantReadWriteLock implements CycleDetectingLock {
        private final LockGraphNode lockGraphNode;
        private final CycleDetectingReentrantReadLock readLock;
        private final CycleDetectingReentrantWriteLock writeLock;

        /* synthetic */ CycleDetectingReentrantReadWriteLock(CycleDetectingLockFactory x0, LockGraphNode x1, boolean x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private CycleDetectingReentrantReadWriteLock(LockGraphNode lockGraphNode, boolean fair) {
            super(fair);
            this.readLock = new CycleDetectingReentrantReadLock(this);
            this.writeLock = new CycleDetectingReentrantWriteLock(this);
            this.lockGraphNode = (LockGraphNode) Preconditions.checkNotNull(lockGraphNode);
        }

        public ReadLock readLock() {
            return this.readLock;
        }

        public WriteLock writeLock() {
            return this.writeLock;
        }

        public LockGraphNode getLockGraphNode() {
            return this.lockGraphNode;
        }

        public boolean isAcquiredByCurrentThread() {
            return isWriteLockedByCurrentThread() || getReadHoldCount() > 0;
        }
    }

    @Beta
    public enum Policies implements Policy {
        THROW {
            public void handlePotentialDeadlock(PotentialDeadlockException e) {
                throw e;
            }
        },
        WARN {
            public void handlePotentialDeadlock(PotentialDeadlockException e) {
                CycleDetectingLockFactory.logger.log(Level.SEVERE, "Detected potential deadlock", e);
            }
        },
        DISABLED {
            public void handlePotentialDeadlock(PotentialDeadlockException e) {
            }
        }
    }

    @Beta
    public static final class PotentialDeadlockException extends ExampleStackTrace {
        private final ExampleStackTrace conflictingStackTrace;

        /* synthetic */ PotentialDeadlockException(LockGraphNode x0, LockGraphNode x1, ExampleStackTrace x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private PotentialDeadlockException(LockGraphNode node1, LockGraphNode node2, ExampleStackTrace conflictingStackTrace) {
            super(node1, node2);
            this.conflictingStackTrace = conflictingStackTrace;
            initCause(conflictingStackTrace);
        }

        public ExampleStackTrace getConflictingStackTrace() {
            return this.conflictingStackTrace;
        }

        public String getMessage() {
            StringBuilder message = new StringBuilder(super.getMessage());
            for (Throwable t = this.conflictingStackTrace; t != null; t = t.getCause()) {
                message.append(", ");
                message.append(t.getMessage());
            }
            return message.toString();
        }
    }

    @Beta
    public static final class WithExplicitOrdering<E extends Enum<E>> extends CycleDetectingLockFactory {
        private final Map<E, LockGraphNode> lockGraphNodes;

        @VisibleForTesting
        WithExplicitOrdering(Policy policy, Map<E, LockGraphNode> lockGraphNodes) {
            super(policy, null);
            this.lockGraphNodes = lockGraphNodes;
        }

        public ReentrantLock newReentrantLock(E rank) {
            return newReentrantLock(rank, false);
        }

        public ReentrantLock newReentrantLock(E rank, boolean fair) {
            if (this.policy == Policies.DISABLED) {
                return new ReentrantLock(fair);
            }
            return new CycleDetectingReentrantLock(this, (LockGraphNode) this.lockGraphNodes.get(rank), fair, null);
        }

        public ReentrantReadWriteLock newReentrantReadWriteLock(E rank) {
            return newReentrantReadWriteLock(rank, false);
        }

        public ReentrantReadWriteLock newReentrantReadWriteLock(E rank, boolean fair) {
            if (this.policy == Policies.DISABLED) {
                return new ReentrantReadWriteLock(fair);
            }
            return new CycleDetectingReentrantReadWriteLock(this, (LockGraphNode) this.lockGraphNodes.get(rank), fair, null);
        }
    }

    /* synthetic */ CycleDetectingLockFactory(Policy x0, AnonymousClass1 x1) {
        this(x0);
    }

    public static CycleDetectingLockFactory newInstance(Policy policy) {
        return new CycleDetectingLockFactory(policy);
    }

    public ReentrantLock newReentrantLock(String lockName) {
        return newReentrantLock(lockName, false);
    }

    public ReentrantLock newReentrantLock(String lockName, boolean fair) {
        if (this.policy == Policies.DISABLED) {
            return new ReentrantLock(fair);
        }
        return new CycleDetectingReentrantLock(this, new LockGraphNode(lockName), fair, null);
    }

    public ReentrantReadWriteLock newReentrantReadWriteLock(String lockName) {
        return newReentrantReadWriteLock(lockName, false);
    }

    public ReentrantReadWriteLock newReentrantReadWriteLock(String lockName, boolean fair) {
        if (this.policy == Policies.DISABLED) {
            return new ReentrantReadWriteLock(fair);
        }
        return new CycleDetectingReentrantReadWriteLock(this, new LockGraphNode(lockName), fair, null);
    }

    public static <E extends Enum<E>> WithExplicitOrdering<E> newInstanceWithExplicitOrdering(Class<E> enumClass, Policy policy) {
        Preconditions.checkNotNull(enumClass);
        Preconditions.checkNotNull(policy);
        return new WithExplicitOrdering(policy, getOrCreateNodes(enumClass));
    }

    private static Map<? extends Enum, LockGraphNode> getOrCreateNodes(Class<? extends Enum> clazz) {
        Map<? extends Enum, LockGraphNode> existing = (Map) lockGraphNodesPerType.get(clazz);
        if (existing != null) {
            return existing;
        }
        Map<? extends Enum, LockGraphNode> created = createNodes(clazz);
        return (Map) MoreObjects.firstNonNull((Map) lockGraphNodesPerType.putIfAbsent(clazz, created), created);
    }

    @VisibleForTesting
    static <E extends Enum<E>> Map<E, LockGraphNode> createNodes(Class<E> clazz) {
        int i;
        EnumMap<E, LockGraphNode> map = Maps.newEnumMap((Class) clazz);
        Enum[] keys = (Enum[]) clazz.getEnumConstants();
        int numKeys = keys.length;
        ArrayList<LockGraphNode> nodes = Lists.newArrayListWithCapacity(numKeys);
        int i2 = 0;
        for (E key : keys) {
            LockGraphNode node = new LockGraphNode(getLockName(key));
            nodes.add(node);
            map.put(key, node);
        }
        for (i = 1; i < numKeys; i++) {
            ((LockGraphNode) nodes.get(i)).checkAcquiredLocks(Policies.THROW, nodes.subList(0, i));
        }
        while (true) {
            i = i2;
            if (i >= numKeys - 1) {
                return Collections.unmodifiableMap(map);
            }
            ((LockGraphNode) nodes.get(i)).checkAcquiredLocks(Policies.DISABLED, nodes.subList(i + 1, numKeys));
            i2 = i + 1;
        }
    }

    private static String getLockName(Enum<?> rank) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rank.getDeclaringClass().getSimpleName());
        stringBuilder.append(".");
        stringBuilder.append(rank.name());
        return stringBuilder.toString();
    }

    private CycleDetectingLockFactory(Policy policy) {
        this.policy = (Policy) Preconditions.checkNotNull(policy);
    }

    private void aboutToAcquire(CycleDetectingLock lock) {
        if (!lock.isAcquiredByCurrentThread()) {
            ArrayList<LockGraphNode> acquiredLockList = (ArrayList) acquiredLocks.get();
            LockGraphNode node = lock.getLockGraphNode();
            node.checkAcquiredLocks(this.policy, acquiredLockList);
            acquiredLockList.add(node);
        }
    }

    private void lockStateChanged(CycleDetectingLock lock) {
        if (!lock.isAcquiredByCurrentThread()) {
            ArrayList<LockGraphNode> acquiredLockList = (ArrayList) acquiredLocks.get();
            LockGraphNode node = lock.getLockGraphNode();
            for (int i = acquiredLockList.size() - 1; i >= 0; i--) {
                if (acquiredLockList.get(i) == node) {
                    acquiredLockList.remove(i);
                    return;
                }
            }
        }
    }
}
