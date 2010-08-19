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

/**
 * Interface defining conversion to a {@code String}.
 * <p>
 * ToStringConverter is an interface and must be implemented with care.
 * Implementations must be immutable and thread-safe.
 */
public interface ToStringConverter<T> {

    /**
     * Converts the specified object to a {@code String}.
     * @param object  the object to convert, not null
     * @return the converted string, may be null but generally not
     */
    public String convertToString(T object);

}