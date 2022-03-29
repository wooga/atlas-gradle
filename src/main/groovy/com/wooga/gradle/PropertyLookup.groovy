/*
 * Copyright 2021 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wooga.gradle


import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

import javax.naming.spi.ObjectFactory
import java.util.concurrent.Callable
import java.util.function.Function


/**
 * An utility object that is used for easily querying the system environment
 * or a project's properties.
 */
class PropertyLookup {

    //------------------------------------------------------------------------/
    // Properties
    //------------------------------------------------------------------------/
    /**
     * Provided environment keys
     */
    final List<String> environmentKeys
    /**
     * Provided property keys
     */
    final List<String> propertyKeys
    /**
     * If it can't find the value from either environment or property map, it will return this one
     */
    private Object defaultValue

    /**
     * @return The default value for this property
     */
    Object getDefaultValue() {
        extractValue(defaultValue)
    }

    /**
     * Sets the default value for this property
     */
    void setDefaultValue(Object value) {
        defaultValue = value
    }

    /**
     * If set, a prefix to apply to all keys during lookup
     */
    String prefix = ""

    //------------------------------------------------------------------------/
    // Constructors
    //------------------------------------------------------------------------/
    /**
     * A lookup that has multiple environment and property keys
     */
    PropertyLookup(List<String> environmentKeys, List<String> propertyKeys, Object defaultValue) {
        this.environmentKeys = environmentKeys
        this.propertyKeys = propertyKeys
        this.defaultValue = defaultValue
    }

    /**
     * A lookup with multiple environment keys and a single property key
     */
    PropertyLookup(List<String> environmentKeys, String propertyKey, Object defaultValue) {
        this(environmentKeys, [propertyKey], defaultValue)
    }

    /**
     * A lookup with multiple property keys and a single environment key
     */
    PropertyLookup(String environmentKey, List<String> propertyKeys, Object defaultValue) {
        this([environmentKey], propertyKeys, defaultValue)
    }

    /**
     * A lookup with a single property key and an environment key
     */
    PropertyLookup(String environmentKey, String propertyKey, Object defaultValue) {
        this([environmentKey], [propertyKey], defaultValue)
    }

    /**
     * A special-case lookup that just returns the default value
     */
    PropertyLookup(Object defaultValue) {
        this([], [], defaultValue)
    }

    /**
     * A property lookup that generates the environment key from the given property key,
     * using a set convention
     */
    static PropertyLookup WithEnvironmentKeyFromProperty(String propertyKey, Object defaultValue) {
        String envKey = PropertyUtils.envNameFromProperty(propertyKey)
        return new PropertyLookup(envKey, propertyKey, defaultValue)
    }

    //------------------------------------------------------------------------/
    // Values
    //------------------------------------------------------------------------/
    /**
     * First, if a 'properties' map is provided (such as through a gradle.properties file), it will look for it there by key
     * Second, it will look in the environment (either provided or the one from the System) by a key
     * If it still hasn't been found, will return a default value (provided during construction)
     * 1
     * @return The value of this property, by first looking it up in a hierarchy.
     *
     */
    Object getValue(Map<String, ?> properties, Map<String, ?> environment = null, Object defaultValue = null) {

        // First, we look among properties
        if (properties != null) {
            for (key in propertyKeys) {
                if (properties.containsKey(key)) {
                    return extractValue(properties.get("${prefix}${key}".toString()))
                }
            }
        }

        // Second, among the environment
        environment = environment ?: System.getenv()
        for (key in environmentKeys) {
            if (environment.containsKey(key)) {
                return extractValue(environment.get("${prefix}${key}".toString()))
            }
        }

        // If provided, return the given default value
        if (defaultValue != null){
            return defaultValue
        }

        // Fallback to the provided default value
        getDefaultValue()
    }

    private static Object extractValue(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof Closure) {
            value = value.call()
        } else if (value instanceof Callable) {
            value = ((Callable) value).call()
        }
        value
    }

    Boolean getValueAsBoolean(Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        def rawValue = getValue(properties, env, defaultValue)
        if (rawValue) {
            rawValue = rawValue.toString().toLowerCase()
            rawValue = (rawValue == "1" || rawValue == "yes") ? "true" : rawValue
            return Boolean.valueOf(rawValue)
        }
        return false
    }

    Integer getValueAsInteger(Map<String, ?> properties, Map<String, ?> environment = null, Object defaultValue = null) {
        Integer.parseInt(getValueAsString(properties, environment, defaultValue))
    }

    String getValueAsString(Map<String, ?> properties, Map<String, ?> environment = null, Object defaultValue = null) {
        getValue(properties, environment, defaultValue) as String
    }

    //------------------------------------------------------------------------/
    // Providers
    //------------------------------------------------------------------------/
    /**
     * @return A provider which returns an {@code Object}
     */
    Provider<Object> getObjectValueProvider(ProviderFactory factory, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        factory.provider({
            getValue(properties, env, defaultValue)
        })
    }

    /**
     * @return A provider which returns an {@code Object}
     */
    Provider<Object> getObjectValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getObjectValueProvider(project.providers, project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns a {@code String}
     */
    Provider<String> getStringValueProvider(ProviderFactory factory, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        factory.provider({
            getValueAsString(properties, env, defaultValue)
        })
    }

    /**
     * @return A provider which returns a {@code String}
     */
    Provider<String> getStringValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getStringValueProvider(project.getProviders(), project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns a {@code Boolean}
     */
    Provider<Boolean> getBooleanValueProvider(ProviderFactory factory, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        factory.provider({
            getValueAsBoolean(properties, env, defaultValue)
        })
    }

    /**
     * @return A provider which returns a {@code Boolean}
     */
    Provider<Boolean> getBooleanValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getBooleanValueProvider(project.getProviders(), project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns an {@code Integer}
     */
    Provider<Integer> getIntegerValueProvider(ProviderFactory factory, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        factory.provider({
            getValueAsInteger(properties, env, defaultValue)
        })
    }

    /**
     * @return A provider which returns an {@code Integer}
     */
    Provider<Integer> getIntegerValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getIntegerValueProvider(project.getProviders(), project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns a {@code RegularFile}
     */
    Provider<RegularFile> getFileValueProvider(ProviderFactory factory, ProjectLayout layout, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        layout.buildDirectory.file(
            factory.provider({
                getValueAsString(properties, env, defaultValue)
            })
        )
    }

    /**
     * @return A provider which returns a {@code RegularFile}
     */
    Provider<RegularFile> getFileValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getFileValueProvider(project.providers, project.layout, project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns a {@code Directory}
     */
    Provider<Directory> getDirectoryValueProvider(ProviderFactory factory, ProjectLayout layout, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        layout.buildDirectory.dir(
            factory.provider({
                getValueAsString(properties, env, defaultValue)
            })
        )
    }

    /**
     * @return A provider which returns a {@code Directory}
     */
    Provider<Directory> getDirectoryValueProvider(Project project, Object defaultValue = null) {
        project.provider({
            getDirectoryValueProvider(project.providers, project.layout, project.properties, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns an object of type {@code T} based on the given closure
     */
    public <T> Provider<T> getValueProvider(ProviderFactory factory, Map<String, ?> properties, Function<String, T> parseFunc, Map<String, ?> env = null, Object defaultValue = null) {
        factory.provider({
            def rawValue = getValue(properties, env, defaultValue).toString()
            def value = parseFunc.apply(rawValue)
            value
        }) as Provider<T>
    }

    /**
     * @return A provider which returns an object of type {@code T} based on the given closure
     */
    public <T> Provider<T> getValueProvider(Project project, Function<String, T> parseFunc, Object defaultValue = null) {
        project.provider({
            getValueProvider(project.providers, project.properties, parseFunc, System.getenv(), defaultValue)
        }).flatMap({ it })
    }

    /**
     * @return A provider which returns an enum of type {#code T}, casting from string when needed
     */
    public <T> Provider<T> getEnumValueProvider(Project project, Class<T> enumClass, Object defaultValue = null) {
        if (!enumClass.enum){
            throw new Exception("${enumClass} is not an enumeration type!")
        }

        getObjectValueProvider(project, defaultValue).map({
            def enumValue = enumClass.invokeMethod("valueOf", it.toString())
            enumValue
        })  as Provider<T>
    }
}
