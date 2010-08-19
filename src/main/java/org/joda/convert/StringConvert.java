/*
 *  Copyright 2010 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.convert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Converter to and from a {@code String}.
 * <p>
 * Support is provided for conversions based on the {@link StringConverter} interface
 * or the {@link ToString} and {@link FromString} annotations.
 * <p>
 * StringConvertor is thread-safe with concurrent caches.
 */
public final class StringConvert {

    /** Cache of toString methods. */
    public static final StringConvert INSTANCE = new StringConvert();

    /** Cache of converters. */
    private final ConcurrentMap<Class<?>, StringConverter<?>> registered = new ConcurrentHashMap<Class<?>, StringConverter<?>>();

    /**
     * Restricted constructor.
     */
    private StringConvert() {
        for (JDKStringConverter conv : JDKStringConverter.values()) {
            registered.put(conv.getType(), conv);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Converts the specified object to a {@code String}.
     * @param object  the object to convert, null returns null
     * @return the converted string, may be null
     * @throws RuntimeException (or subclass) if unable to convert
     */
    @SuppressWarnings("unchecked")
    public <T> String convertToString(T object) {
        if (object == null) {
            return null;
        }
        Class<T> cls = (Class<T>) object.getClass();
        StringConverter<T> conv = findConverter(cls);
        return conv.convertToString(object);
    }

    /**
     * Converts the specified object from a {@code String}.
     * @param object  the object to convert, null returns null
     * @return the converted string, may be null
     * @throws RuntimeException (or subclass) if unable to convert
     */
    public <T> T convertFromString(Class<T> cls, String str) {
        if (str == null) {
            return null;
        }
        StringConverter<T> conv = findConverter(cls);
        return conv.convertFromString(str);
    }

    /**
     * Finds a suitable converter for the type.
     * @param cls  the class to convert, not null
     * @return the converter, not null
     * @throws RuntimeException (or subclass) if no converter found
     */
    @SuppressWarnings("unchecked")
    private <T> StringConverter<T> findConverter(final Class<T> cls) {
        StringConverter<T> conv = (StringConverter<T>) registered.get(cls);
        if (conv == null) {
            conv = findAnnotationConverter(cls);
            if (conv == null) {
                throw new IllegalStateException("No registered converter found: " + cls);
            }
            registered.putIfAbsent(cls, conv);
        }
        return conv;
    }

    /**
     * Finds the conversion method.
     * @param cls  the class to find a method for, not null
     * @return the method to call, null means use {@code toString}
     */
    private <T> StringConverter<T> findAnnotationConverter(final Class<T> cls) {
        Method toString = findToStringMethod(cls);
        Constructor<T> con = findFromStringConstructor(cls);
        Method fromString = findFromStringMethod(cls);
        if (con != null) {
            if (fromString != null) {
                throw new IllegalStateException("Both method and constructor are annotated with @FromString");
            } else {
                return new MethodConstructorStringConverter<T>(cls, toString, con);
            }
        } else {
            if (fromString != null) {
                return new MethodsStringConverter<T>(cls, toString, fromString);
            } else {
                throw new IllegalStateException("No method or constructor found annotated with @FromString");
            }
        }
    }

    /**
     * Finds the conversion method.
     * @param cls  the class to find a method for, not null
     * @return the method to call, null means use {@code toString}
     */
    private Method findToStringMethod(Class<?> cls) {
        Class<?> loopCls = cls;
        while (loopCls != Object.class) {
            Method[] methods = loopCls.getDeclaredMethods();
            for (Method method : methods) {
                ToString toString = method.getAnnotation(ToString.class);
                if (toString != null) {
                    // TODO: check no other methods with annotation
                    return method;
                }
            }
            loopCls = cls.getSuperclass();
        }
        throw new IllegalStateException("No method found annotated with @ToString");
    }

    /**
     * Finds the conversion method.
     * @param cls  the class to find a method for, not null
     * @return the method to call, null means use {@code toString}
     */
    private <T> Constructor<T> findFromStringConstructor(Class<T> cls) {
        try {
            Constructor<T> con = cls.getDeclaredConstructor(String.class);
            FromString fromString = con.getAnnotation(FromString.class);
            return fromString != null ? con : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Finds the conversion method.
     * @param cls  the class to find a method for, not null
     * @return the method to call, null means use {@code toString}
     */
    private Method findFromStringMethod(Class<?> cls) {
        Class<?> loopCls = cls;
        while (loopCls != Object.class) {
            Method[] methods = loopCls.getDeclaredMethods();
            for (Method method : methods) {
                FromString fromString = method.getAnnotation(FromString.class);
                if (fromString != null) {
                    // TODO: check no other methods with annotation
                    return method;
                }
            }
            loopCls = cls.getSuperclass();
        }
        return null;
    }

    //-----------------------------------------------------------------------
    /**
     * Registers a converter for a specific type.
     * The converter will be used for subclasses unless overidden.
     * @param cls  the class to register a converter for, not null
     * @param converter  the String converter, not null
     */
    public <T> void register(final Class<T> cls, StringConverter<T> converter) {
        if (cls == null ) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("StringConverter must not be null");
        }
        StringConverter<?> old = registered.putIfAbsent(cls, converter);
        if (old != null) {
            throw new IllegalStateException("Converter already registered for class: " + cls);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a simple string representation of the object.
     * 
     * @return the string representation, never null
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}