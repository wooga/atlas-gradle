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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Provides a task with properties for configuring arguments to be used during execution.
 */
trait ArgumentsSpec extends BaseSpec {

    private Provider<List<String>> internalArguments = providers.provider { new ArrayList<String>() }

    /**
     * @return Internal arguments, which are set through a provider.
     * (The most common use being arguments generated before execution of a task)
     */
    @Internal
    private Provider<List<String>> getInternalArguments() {
        internalArguments
    }

    void setInternalArguments(Provider<List<String>> provider) {
        internalArguments = provider;
    }

    private final ListProperty<String> additionalArguments = objects.listProperty(String)

    /**
     * @return Additional arguments to be consumed by the execution of a task.
     * (The most common use case being that of users supplying them during task configuration)
     */
    @Input
    ListProperty<String> getAdditionalArguments() {
        additionalArguments
    }

    void setAdditionalArguments(Iterable<String> value) {
        additionalArguments.set(value)
    }

    void setAdditionalArguments(String value) {
        additionalArguments.set([value])
    }

    void setAdditionalArguments(Provider<? extends Iterable<String>> value) {
        additionalArguments.set(value)
    }

    void argument(String value) {
        additionalArguments.add(value)
    }

    void arguments(Iterable<String> value) {
        additionalArguments.addAll(value)
    }

    void arguments(String... value) {
        arguments(value.toList())
    }

    /**
     * @return Retrieves both {@code internalArguments} and {@code additionalArguments}, to be consumed by a task.
     */
    @Input
    Provider<List<String>> getArguments() {
        providers.provider({
            List<String> result = new ArrayList<String>()
            result.addAll(internalArguments.get())
            result.addAll(additionalArguments.get())
            result
        })
    }

    private final MapProperty<String, ?> environment = objects.mapProperty(String, Object)

    /**
     * @return Used for populating the environment to be used during task execution.
     */
    @Internal
    MapProperty<String, ?> getEnvironment() {
        environment
    }

    /**
     * Sets the task's environment (which is used during execution) from the System's environment ({@code System.env})
     */
    void setEnvironmentDefaults() {
        environment.putAll(providers.provider({ System.getenv() }))
    }

}
