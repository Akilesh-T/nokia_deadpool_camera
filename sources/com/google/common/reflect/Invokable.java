package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import javax.annotation.Nullable;

@Beta
public abstract class Invokable<T, R> extends Element implements GenericDeclaration {

    static class ConstructorInvokable<T> extends Invokable<T, T> {
        final Constructor<?> constructor;

        ConstructorInvokable(Constructor<?> constructor) {
            super(constructor);
            this.constructor = constructor;
        }

        /* Access modifiers changed, original: final */
        public final Object invokeInternal(@Nullable Object receiver, Object[] args) throws InvocationTargetException, IllegalAccessException {
            try {
                return this.constructor.newInstance(args);
            } catch (InstantiationException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.constructor);
                stringBuilder.append(" failed.");
                throw new RuntimeException(stringBuilder.toString(), e);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public Type getGenericReturnType() {
            Class<?> declaringClass = getDeclaringClass();
            TypeVariable<?>[] typeParams = declaringClass.getTypeParameters();
            if (typeParams.length > 0) {
                return Types.newParameterizedType(declaringClass, typeParams);
            }
            return declaringClass;
        }

        /* Access modifiers changed, original: 0000 */
        public Type[] getGenericParameterTypes() {
            Type[] types = this.constructor.getGenericParameterTypes();
            if (types.length > 0 && mayNeedHiddenThis()) {
                Class<?>[] rawParamTypes = this.constructor.getParameterTypes();
                if (types.length == rawParamTypes.length && rawParamTypes[0] == getDeclaringClass().getEnclosingClass()) {
                    return (Type[]) Arrays.copyOfRange(types, 1, types.length);
                }
            }
            return types;
        }

        /* Access modifiers changed, original: 0000 */
        public Type[] getGenericExceptionTypes() {
            return this.constructor.getGenericExceptionTypes();
        }

        /* Access modifiers changed, original: final */
        public final Annotation[][] getParameterAnnotations() {
            return this.constructor.getParameterAnnotations();
        }

        public final TypeVariable<?>[] getTypeParameters() {
            TypeVariable<?>[] declaredByClass = getDeclaringClass().getTypeParameters();
            TypeVariable<?>[] declaredByConstructor = this.constructor.getTypeParameters();
            TypeVariable<?>[] result = new TypeVariable[(declaredByClass.length + declaredByConstructor.length)];
            System.arraycopy(declaredByClass, 0, result, 0, declaredByClass.length);
            System.arraycopy(declaredByConstructor, 0, result, declaredByClass.length, declaredByConstructor.length);
            return result;
        }

        public final boolean isOverridable() {
            return false;
        }

        public final boolean isVarArgs() {
            return this.constructor.isVarArgs();
        }

        private boolean mayNeedHiddenThis() {
            Class<?> declaringClass = this.constructor.getDeclaringClass();
            boolean z = true;
            if (declaringClass.getEnclosingConstructor() != null) {
                return true;
            }
            Method enclosingMethod = declaringClass.getEnclosingMethod();
            if (enclosingMethod != null) {
                return 1 ^ Modifier.isStatic(enclosingMethod.getModifiers());
            }
            if (declaringClass.getEnclosingClass() == null || Modifier.isStatic(declaringClass.getModifiers())) {
                z = false;
            }
            return z;
        }
    }

    static class MethodInvokable<T> extends Invokable<T, Object> {
        final Method method;

        MethodInvokable(Method method) {
            super(method);
            this.method = method;
        }

        /* Access modifiers changed, original: final */
        public final Object invokeInternal(@Nullable Object receiver, Object[] args) throws InvocationTargetException, IllegalAccessException {
            return this.method.invoke(receiver, args);
        }

        /* Access modifiers changed, original: 0000 */
        public Type getGenericReturnType() {
            return this.method.getGenericReturnType();
        }

        /* Access modifiers changed, original: 0000 */
        public Type[] getGenericParameterTypes() {
            return this.method.getGenericParameterTypes();
        }

        /* Access modifiers changed, original: 0000 */
        public Type[] getGenericExceptionTypes() {
            return this.method.getGenericExceptionTypes();
        }

        /* Access modifiers changed, original: final */
        public final Annotation[][] getParameterAnnotations() {
            return this.method.getParameterAnnotations();
        }

        public final TypeVariable<?>[] getTypeParameters() {
            return this.method.getTypeParameters();
        }

        public final boolean isOverridable() {
            return (isFinal() || isPrivate() || isStatic() || Modifier.isFinal(getDeclaringClass().getModifiers())) ? false : true;
        }

        public final boolean isVarArgs() {
            return this.method.isVarArgs();
        }
    }

    public abstract Type[] getGenericExceptionTypes();

    public abstract Type[] getGenericParameterTypes();

    public abstract Type getGenericReturnType();

    public abstract Annotation[][] getParameterAnnotations();

    public abstract Object invokeInternal(@Nullable Object obj, Object[] objArr) throws InvocationTargetException, IllegalAccessException;

    public abstract boolean isOverridable();

    public abstract boolean isVarArgs();

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    <M extends AccessibleObject & Member> Invokable(M member) {
        super(member);
    }

    public static Invokable<?, Object> from(Method method) {
        return new MethodInvokable(method);
    }

    public static <T> Invokable<T, T> from(Constructor<T> constructor) {
        return new ConstructorInvokable(constructor);
    }

    public final R invoke(@Nullable T receiver, Object... args) throws InvocationTargetException, IllegalAccessException {
        return invokeInternal(receiver, (Object[]) Preconditions.checkNotNull(args));
    }

    public final TypeToken<? extends R> getReturnType() {
        return TypeToken.of(getGenericReturnType());
    }

    public final ImmutableList<Parameter> getParameters() {
        Type[] parameterTypes = getGenericParameterTypes();
        Annotation[][] annotations = getParameterAnnotations();
        Builder<Parameter> builder = ImmutableList.builder();
        for (int i = 0; i < parameterTypes.length; i++) {
            builder.add(new Parameter(this, i, TypeToken.of(parameterTypes[i]), annotations[i]));
        }
        return builder.build();
    }

    public final ImmutableList<TypeToken<? extends Throwable>> getExceptionTypes() {
        Builder<TypeToken<? extends Throwable>> builder = ImmutableList.builder();
        for (Type type : getGenericExceptionTypes()) {
            builder.add(TypeToken.of(type));
        }
        return builder.build();
    }

    public final <R1 extends R> Invokable<T, R1> returning(Class<R1> returnType) {
        return returning(TypeToken.of((Class) returnType));
    }

    public final <R1 extends R> Invokable<T, R1> returning(TypeToken<R1> returnType) {
        if (returnType.isAssignableFrom(getReturnType())) {
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invokable is known to return ");
        stringBuilder.append(getReturnType());
        stringBuilder.append(", not ");
        stringBuilder.append(returnType);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public final Class<? super T> getDeclaringClass() {
        return super.getDeclaringClass();
    }

    public TypeToken<T> getOwnerType() {
        return TypeToken.of(getDeclaringClass());
    }
}
