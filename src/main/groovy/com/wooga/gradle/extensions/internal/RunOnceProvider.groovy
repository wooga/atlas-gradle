package com.wooga.gradle.extensions.internal

import org.gradle.api.Transformer
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.internal.provider.TransformBackedProvider

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
