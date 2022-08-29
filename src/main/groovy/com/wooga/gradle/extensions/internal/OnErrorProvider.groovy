package com.wooga.gradle.extensions.internal

import org.gradle.api.internal.provider.AbstractMinimalProvider
import org.gradle.api.provider.Provider
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.internal.UncheckedException

import javax.annotation.Nullable
import java.util.function.Consumer
import java.util.function.Function


/**
 * Provider that executes a function when an exception is thrown in any previous point of its chain.
 * @see com.wooga.gradle.extensions.ProviderExtensions#onError(org.gradle.api.provider.Provider, Function)
 * @see com.wooga.gradle.extensions.ProviderExtensions#onError(org.gradle.api.provider.Provider, Exception)
 */
class OnErrorProvider<T> extends AbstractMinimalProvider<T> {

    final AbstractMinimalProvider<T> base
    final Function<? extends Throwable, T> onError

    OnErrorProvider(AbstractMinimalProvider<T> base, Function<? extends Throwable, T> onError) {
        this.base = base
        this.onError = onError
    }

    @Override
    protected Value<? extends T> calculateOwnValue(ValueConsumer consumer) {
        try {
            return base.calculateOwnValue(consumer)
        } catch (Exception e) {
            def exception = e
            if(e instanceof UncheckedException) {
                exception = e.cause
            }
            return Value.ofNullable(onError.apply(exception))
        }
    }

    @Override
    Class<T> getType() {
        return base.getType()
    }
}

