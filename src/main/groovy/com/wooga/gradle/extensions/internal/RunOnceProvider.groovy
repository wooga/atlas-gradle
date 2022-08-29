package com.wooga.gradle.extensions.internal

import org.gradle.api.Transformer
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.internal.provider.TransformBackedProvider

/**
 *{@link TransformBackedProvider} that runs the given transformation only once, no matter how many times the provider is resolved.
 * @param <OUT> Type of the output of the transformation
 * @param <IN> Type of the input of the transformation
 * @see com.wooga.gradle.extensions.ProviderExtensions#mapOnce(org.gradle.api.provider.Provider, Transformer)
 */
class RunOnceProvider<OUT, IN> extends TransformBackedProvider<OUT, IN> {

    private Value<? extends OUT> result
    private boolean ran

    RunOnceProvider(Transformer<? extends OUT, ? super IN> transformer, ProviderInternal<? extends IN> provider) {
        super(transformer, provider)
        this.ran = false
    }

    @Override
    protected Value<? extends OUT> calculateOwnValue(ValueConsumer consumer) {
        if(!result) {
            result = super.calculateOwnValue(consumer)
            ran = true
        }
        return result

    }

    @Override
    String toString() {
        return "runOnce_${super.toString()}";
    }
}
