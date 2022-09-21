package com.wooga.gradle.extensions.internal

import org.gradle.api.internal.provider.AbstractMinimalProvider
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.provider.Provider
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName

import javax.annotation.Nullable


/**
 * Provider that has a name. Useful for error tracking.
 * @see com.wooga.gradle.extensions.ProviderExtensions#named(org.gradle.api.provider.Provider, String)
 */
class NamedProvider<T> extends AbstractMinimalProvider<T> {

    final AbstractMinimalProvider<T> base
    final String name

    NamedProvider(AbstractMinimalProvider<T> base, String name) {
        this.base = base
        this.name = name
    }

    @Nullable
    protected DisplayName getDeclaredDisplayName() {
        return Describables.of(name);
    }

    protected DisplayName getTypedDisplayName() {
        return getDeclaredDisplayName();
    }

    @Override
    protected Value<? extends T> calculateOwnValue(ValueConsumer consumer) {
        return base.calculateOwnValue(consumer)
    }

    @Override
    Class<T> getType() {
        return base.getType()
    }

    @Override
    String toString() {
        return "provider[$name]($base)"
    }
}

