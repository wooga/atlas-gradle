package com.wooga.gradle.extensions

import com.wooga.gradle.extensions.internal.NamedProvider
import com.wooga.gradle.extensions.internal.OnErrorProvider
import com.wooga.gradle.extensions.internal.RunOnceProvider
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.internal.provider.AbstractMinimalProvider
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider

import java.util.function.Function

class ProviderExtensions {

    static Logger DEFAULT_LOGGER = Logging.getLogger(Project)

    /**
     * Sets a name to a provider. The name is shown on exceptions and in toString(), so its useful for error tracking.
     * @param self
     * @param name - Desired provider name
     * @return {@link com.wooga.gradle.extensions.internal.NamedProvider} named after the given name
     */
    static <T> Provider<T> named(final Provider<T> self, String name) {
        return new NamedProvider<T>(self as AbstractMinimalProvider<T>, name)
    }

    /**
     * Runs the given transformation only once, no matter how many times the provider is resolved.
     * Useful for 'non-pure' operations like logging within the provider chain.
     * After that, the value returned will be always the same as the first.
     * @param self
     * @param transformation - Operation to be executed only once.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the given transformation.
     */
    static <T, S> Provider<S> mapOnce(final Provider<T> self, Transformer<? extends S, ? super T> transformer) {
        return new RunOnceProvider(transformer, self as ProviderInternal)
    }

    /**
     * Runs the given function when the provider throws an exception on value resolution.
     * DO NOT capture exception thrown due to not value present.
     * @param self
     * @param onError - exception -> <T> function processing the thrown exception.
     * @return {@link com.wooga.gradle.extensions.internal.OnErrorProvider}.
     */
    static <T> Provider<T> onError(final Provider<T> self, Function<? extends Throwable, T> onError) {
        return new OnErrorProvider<T>(self as AbstractMinimalProvider<T>, onError)
    }

    /**
     * Throws the given exception when the provider throws during value resolution.
     * Same rules as {@link ProviderExtensions#onError(Provider, Function)} applies.
     * DO NOT capture exception thrown due to not value present.
     * @param self
     * @param toThrow - exception to be thrown
     * @return {@link com.wooga.gradle.extensions.internal.OnErrorProvider}.
     */
    static <T> Provider<T> onError(final Provider<T> self, Exception toThrow) {
        return self.onError {throw toThrow }
    }

    /**
     * Runs a log operation over the given logger. Closure delegate is set to given logger to make easier to run desired log operation.
     * For instance:
     * <pre>
     * provider.log{ value -> info("my value is $value!"} }
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param logOp - {@code T -> Void} {@link groovy.lang.Closure<Void>} with logging operation to run. Has logger as delegate.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     */
    static <T> Provider<T> log(final Provider<T> self, Logger logger = DEFAULT_LOGGER, @DelegatesTo(Logger) Closure<Void> logOp) {
        logOp.setDelegate(logger)
        return self.mapOnce {T it ->
            logOp(it)
            return it
        }
    }
    /**
     * Convenience function that print warn-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }.
     * <pre>
     * provider.logWarn{value -> "my value is $value!"}
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param warnMsg {@code T -> String} {@link groovy.lang.Closure<String>} returning string to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#log(Provider, Logger, Closure)
     */
    static <T> Provider<T> logWarn(final Provider<T> self, Logger logger = DEFAULT_LOGGER, Function <T, String > warnMsg) {
        self.log(logger) {
            T it -> warn(warnMsg.apply(it))
        }
    }
    /**
     * Convenience function that print warn-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }, but using static strings.
     * <pre>
     * provider.logWarn("please don't do this")
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param message String to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#logWarn(Provider, Logger, Closure)
     */
    static <T> Provider<T> logWarn(final Provider<T> self, Logger logger = DEFAULT_LOGGER, String message) {
        self.logWarn(logger) { _ -> message }
    }
    /**
     * Convenience function that runs info-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }.
     * <pre>
     * provider.logWarn{value -> "my value is $value!"}
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param infoMsg {@code T -> String} {@link groovy.lang.Closure<String>} returning string to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#log(Provider, Logger, Closure)
     */
    static <T> Provider<T> logInfo(final Provider<T> self, Logger logger = DEFAULT_LOGGER, Function <T, String > infoMsg) {
        self.log(logger) { T it -> info(infoMsg.apply(it)) }
    }

    /**
     * Convenience function that print info-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }, but using static strings.
     * <pre>
     * provider.logInfo("I'm informing you!")
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param message String to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#logInfo(Provider, Logger, Closure)
     */
    static <T> Provider<T> logInfo(final Provider<T> self, Logger logger = DEFAULT_LOGGER, String message) {
        self.logInfo(logger) { _ -> message }
    }
    /**
     * Convenience function that runs error-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }.
     * <pre>
     * provider.logWarn{value -> "my value is $value!"}
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param errMsg {@code T -> String} {@link groovy.lang.Closure<String>} returning string to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#log(Provider, Logger, Closure)
     */
    static <T> Provider<T> logError(final Provider<T> self, Logger logger = DEFAULT_LOGGER, Function <T, String > errMsg) {
        self.log(logger) { T it -> error(errMsg.apply(it)) }
    }

    /**
     * Convenience function that print error-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }, but using static strings,
     * and with potential to log exceptions.
     * <pre>
     * provider.logError("Beep boop I failed!")
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param message String to be logged.
     * @param exception Show exception + stacktrace log together with message if given.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#logError(Provider, Logger, Closure)
     */
    static <T> Provider<T> logError(final Provider<T> self, Logger logger = DEFAULT_LOGGER, String message, Exception e = null) {
        self.log(logger) { _ ->
            if(e){
                error(message, e)
            } else {
                error(message)
            }
        }
    }
    /**
     * Convenience function that print debug-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }.
     * <pre>
     * provider.logWarn{value -> "debugging: $value!"}
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param debugMsg {@code T -> String} {@link groovy.lang.Closure<String>} returning string to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#log(Provider, Logger, Closure)
     */
    static <T> Provider<T> logDebug(final Provider<T> self, Logger logger = DEFAULT_LOGGER, Function <T, String > debugMsg) {
        self.log(logger) {
            T it -> debug(debugMsg.apply(it))
        }
    }
    /**
     * Convenience function that print debug-level logs using {@link ProviderExtensions#log(Provider, Logger, Closure) }, but with static strings.
     * <pre>
     * provider.logWarn("debugging")
     * </pre>
     * @param self
     * @param logger - {@link org.gradle.api.logging.Logger} to be attached to the operation closure. Defaults to Logging.getLogger(Project).
     * @param message String to be logged.
     * @return {@link com.wooga.gradle.extensions.internal.RunOnceProvider} with the logging operation.
     * @see ProviderExtensions#logDebug(Provider, Logger, Closure)
     */
    static <T> Provider<T> logDebug(final Provider<T> self, Logger logger = DEFAULT_LOGGER, String message) {
        self.logDebug(logger) { _ -> message }
    }
}
