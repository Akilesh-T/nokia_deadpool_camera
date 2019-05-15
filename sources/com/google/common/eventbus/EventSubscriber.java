package com.google.common.eventbus;

import com.google.common.base.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

class EventSubscriber {
    private final Method method;
    private final Object target;

    EventSubscriber(Object target, Method method) {
        Preconditions.checkNotNull(target, "EventSubscriber target cannot be null.");
        Preconditions.checkNotNull(method, "EventSubscriber method cannot be null.");
        this.target = target;
        this.method = method;
        method.setAccessible(true);
    }

    public void handleEvent(Object event) throws InvocationTargetException {
        StringBuilder stringBuilder;
        Preconditions.checkNotNull(event);
        try {
            this.method.invoke(this.target, new Object[]{event});
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Method rejected target/argument: ");
            stringBuilder.append(event);
            throw new Error(stringBuilder.toString(), e);
        } catch (IllegalAccessException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Method became inaccessible: ");
            stringBuilder.append(event);
            throw new Error(stringBuilder.toString(), e2);
        } catch (InvocationTargetException e3) {
            if (e3.getCause() instanceof Error) {
                throw ((Error) e3.getCause());
            }
            throw e3;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[wrapper ");
        stringBuilder.append(this.method);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return ((this.method.hashCode() + 31) * 31) + System.identityHashCode(this.target);
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = false;
        if (!(obj instanceof EventSubscriber)) {
            return false;
        }
        EventSubscriber that = (EventSubscriber) obj;
        if (this.target == that.target && this.method.equals(that.method)) {
            z = true;
        }
        return z;
    }

    public Object getSubscriber() {
        return this.target;
    }

    public Method getMethod() {
        return this.method;
    }
}
