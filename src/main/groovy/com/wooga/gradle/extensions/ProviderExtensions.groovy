package com.wooga.gradle.extensions

import org.gradle.api.internal.provider.AbstractMinimalProvider
import org.gradle.api.provider.Provider

import java.util.function.Function

class ProviderExtensions {

    static <T> Provider<T> named(final Provider<T> self, String name) {
        return new NamedProvider<T>(self as AbstractMinimalProvider<T>, name)
    }

    static <T> Provider<T> onError(final Provider<T> self, Function<? extends Throwable, T> onError) {
        return new OnErrorProvider<T>(self as AbstractMinimalProvider<T>, onError)
    }

    static <T> Provider<T> onError(final Provider<T> self, Exception toThrow) {
        return self.onError {throw toThrow }
    }
}