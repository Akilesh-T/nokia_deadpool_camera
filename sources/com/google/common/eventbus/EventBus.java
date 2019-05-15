package com.google.common.eventbus;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Beta
public class EventBus {
    private static final LoadingCache<Class<?>, Set<Class<?>>> flattenHierarchyCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>, Set<Class<?>>>() {
        public Set<Class<?>> load(Class<?> concreteClass) {
            return TypeToken.of((Class) concreteClass).getTypes().rawTypes();
        }
    });
    private final ThreadLocal<Queue<EventWithSubscriber>> eventsToDispatch;
    private final SubscriberFindingStrategy finder;
    private final ThreadLocal<Boolean> isDispatching;
    private SubscriberExceptionHandler subscriberExceptionHandler;
    private final SetMultimap<Class<?>, EventSubscriber> subscribersByType;
    private final ReadWriteLock subscribersByTypeLock;

    static class EventWithSubscriber {
        final Object event;
        final EventSubscriber subscriber;

        public EventWithSubscriber(Object event, EventSubscriber subscriber) {
            this.event = Preconditions.checkNotNull(event);
            this.subscriber = (EventSubscriber) Preconditions.checkNotNull(subscriber);
        }
    }

    private static final class LoggingSubscriberExceptionHandler implements SubscriberExceptionHandler {
        private final Logger logger;

        public LoggingSubscriberExceptionHandler(String identifier) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(EventBus.class.getName());
            stringBuilder.append(".");
            stringBuilder.append((String) Preconditions.checkNotNull(identifier));
            this.logger = Logger.getLogger(stringBuilder.toString());
        }

        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            Logger logger = this.logger;
            Level level = Level.SEVERE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not dispatch event: ");
            stringBuilder.append(context.getSubscriber());
            stringBuilder.append(" to ");
            stringBuilder.append(context.getSubscriberMethod());
            logger.log(level, stringBuilder.toString(), exception.getCause());
        }
    }

    public EventBus() {
        this("default");
    }

    public EventBus(String identifier) {
        this(new LoggingSubscriberExceptionHandler(identifier));
    }

    public EventBus(SubscriberExceptionHandler subscriberExceptionHandler) {
        this.subscribersByType = HashMultimap.create();
        this.subscribersByTypeLock = new ReentrantReadWriteLock();
        this.finder = new AnnotatedSubscriberFinder();
        this.eventsToDispatch = new ThreadLocal<Queue<EventWithSubscriber>>() {
            /* Access modifiers changed, original: protected */
            public Queue<EventWithSubscriber> initialValue() {
                return new LinkedList();
            }
        };
        this.isDispatching = new ThreadLocal<Boolean>() {
            /* Access modifiers changed, original: protected */
            public Boolean initialValue() {
                return Boolean.valueOf(false);
            }
        };
        this.subscriberExceptionHandler = (SubscriberExceptionHandler) Preconditions.checkNotNull(subscriberExceptionHandler);
    }

    public void register(Object object) {
        Multimap<Class<?>, EventSubscriber> methodsInListener = this.finder.findAllSubscribers(object);
        this.subscribersByTypeLock.writeLock().lock();
        try {
            this.subscribersByType.putAll(methodsInListener);
        } finally {
            this.subscribersByTypeLock.writeLock().unlock();
        }
    }

    public void unregister(Object object) {
        for (Entry<Class<?>, Collection<EventSubscriber>> entry : this.finder.findAllSubscribers(object).asMap().entrySet()) {
            Class<?> eventType = (Class) entry.getKey();
            Collection<EventSubscriber> eventMethodsInListener = (Collection) entry.getValue();
            this.subscribersByTypeLock.writeLock().lock();
            try {
                Set<EventSubscriber> currentSubscribers = this.subscribersByType.get(eventType);
                if (currentSubscribers.containsAll(eventMethodsInListener)) {
                    currentSubscribers.removeAll(eventMethodsInListener);
                    this.subscribersByTypeLock.writeLock().unlock();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("missing event subscriber for an annotated method. Is ");
                    stringBuilder.append(object);
                    stringBuilder.append(" registered?");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th) {
                this.subscribersByTypeLock.writeLock().unlock();
                throw th;
            }
        }
    }

    public void post(Object event) {
        boolean dispatched = false;
        for (Class<?> eventType : flattenHierarchy(event.getClass())) {
            this.subscribersByTypeLock.readLock().lock();
            try {
                Set<EventSubscriber> wrappers = this.subscribersByType.get(eventType);
                if (!wrappers.isEmpty()) {
                    dispatched = true;
                    for (EventSubscriber wrapper : wrappers) {
                        enqueueEvent(event, wrapper);
                    }
                }
                this.subscribersByTypeLock.readLock().unlock();
            } catch (Throwable th) {
                this.subscribersByTypeLock.readLock().unlock();
            }
        }
        if (!(dispatched || (event instanceof DeadEvent))) {
            post(new DeadEvent(this, event));
        }
        dispatchQueuedEvents();
    }

    /* Access modifiers changed, original: 0000 */
    public void enqueueEvent(Object event, EventSubscriber subscriber) {
        ((Queue) this.eventsToDispatch.get()).offer(new EventWithSubscriber(event, subscriber));
    }

    /* Access modifiers changed, original: 0000 */
    public void dispatchQueuedEvents() {
        if (!((Boolean) this.isDispatching.get()).booleanValue()) {
            this.isDispatching.set(Boolean.valueOf(true));
            try {
                Queue<EventWithSubscriber> events = (Queue) this.eventsToDispatch.get();
                while (true) {
                    EventWithSubscriber eventWithSubscriber = (EventWithSubscriber) events.poll();
                    EventWithSubscriber eventWithSubscriber2 = eventWithSubscriber;
                    if (eventWithSubscriber == null) {
                        break;
                    }
                    dispatch(eventWithSubscriber2.event, eventWithSubscriber2.subscriber);
                }
            } finally {
                this.isDispatching.remove();
                this.eventsToDispatch.remove();
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void dispatch(Object event, EventSubscriber wrapper) {
        try {
            wrapper.handleEvent(event);
        } catch (InvocationTargetException e) {
            this.subscriberExceptionHandler.handleException(e.getCause(), new SubscriberExceptionContext(this, event, wrapper.getSubscriber(), wrapper.getMethod()));
        } catch (Throwable t) {
            Logger.getLogger(EventBus.class.getName()).log(Level.SEVERE, String.format("Exception %s thrown while handling exception: %s", new Object[]{t, e.getCause()}), t);
        }
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
        try {
            return (Set) flattenHierarchyCache.getUnchecked(concreteClass);
        } catch (UncheckedExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }
}
