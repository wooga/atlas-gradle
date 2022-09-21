package com.wooga.gradle.extensions

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.logging.Logger
import spock.lang.Unroll

class ProviderExtensionsSpec extends ProjectSpec {

    def "points provider name on missing value"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider { null as String }.named(providerName)
        when:
        provider.get()
        then:
        def e = thrown(MissingValueException)
        e.message.contains("Cannot query the value of $providerName because it has no value available.")
    }

    def "points provider name on missing value on derived providers"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider { null as String }.named(providerName)
        when:
        provider.map { "something else" }.get()
        then:
        def e = thrown(MissingValueException)
        e.message.contains("The value of this provider is derived from: $providerName")
    }


    def "points provider name on missing value on zipped providers"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider { null as String }.named(providerName)
        def otherProvider = project.provider { "value" }
        def zippedProvider = provider.zip(otherProvider) { null as String }

        when:
        zippedProvider.get()

        then:
        //zip like to be different from the other provider functions
        def e = thrown(InvalidUserDataException)
        e.message.contains("Provider has no value: provider[$providerName]")
    }

    @Unroll
    def "executes onError block on exception"() {
        given:
        def provider = project.provider { throw baseException }.onError {
            Exception e -> throw new IllegalArgumentException(message, e)
        }

        when:
        providerResolvingOp(provider)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "my error"
        e.cause == baseException

        where:
        providerResolvingOp << [
                { p -> p.get() },
                { p -> p.orNull },
                { p -> p.getOrElse("otherValue") },
                { p -> p.map { "otherValue" }.get() },
        ]
        baseException = new Exception("base exception")
        message = "my error"
    }

    def "executes onError block on exception in zipped provider"() {
        given:
        def provider = project.provider { throw baseException }.onError {
            Exception e -> throw new IllegalArgumentException(message, e)
        }

        when:
        provider.zip(project.provider { "other" }, {}).get()

        then:
        def e = thrown(IllegalArgumentException) //topmost exception is org.gradle.internal.UncheckedException
        e.message == "my error"
        e.cause == baseException

        where:
        baseException = new Exception("base exception")
        message = "my error"
    }

    @Unroll
    def "throws onError exception on exception"() {
        given:
        def provider = project.provider { throw baseException }
                .onError(new IllegalArgumentException(message, baseException))

        when:
        providerResolvingOp(provider)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "my error"
        e.cause == baseException

        where:
        providerResolvingOp << [
                { p -> p.get() },
                { p -> p.orNull },
                { p -> p.getOrElse("otherValue") },
                { p -> p.map { "otherValue" }.get() },
        ]
        baseException = new Exception("base exception")
        message = "my error"
    }

    def "runs mapOnce function only once on multiple provider executions"() {
        given:
        def execCounter = 0
        def provider = project.provider { "a" }.mapOnce { execCounter++ }
        when:
        provider.get()
        provider.get()
        then:
        execCounter == 1
    }

    def "mapOnce function always returns same value as the first run"() {
        given:
        def execCounter = 0
        def provider = project.provider { "a" }.mapOnce { ++execCounter }
        def otherProvider = project.provider { "a" }.map { ++execCounter }

        when:
        def result = provider.get()
        def secondResult = provider.get()
        def otherResult = otherProvider.get()
        def otherSecondResult = otherProvider.get()

        then:
        result == secondResult
        otherResult != otherSecondResult
    }

    @Unroll("logs #level-level #situation message once on provider resolution")
    def "logs message once on provider resolution"() {
        given:
        def logger = Mock(Logger)
        def provider = project.provider { value }
        switch (level) {
            case "warn": provider = provider.logWarn(logger, message); break
            case "info": provider = provider.logInfo(logger, message); break
            case "debug": provider = provider.logDebug(logger, message); break
            case "error": provider = provider.logError(logger, message); break
        }

        when:
        provider.get()
        provider.get()

        then:
        switch (level) {
            case "warn": 1 * logger.warn(expected); break
            case "info": 1 * logger.info(expected); break
            case "debug": 1 * logger.debug(expected); break
            case "error": 1 * logger.error(expected); break
        }

        where:
        level   | message             | value   | expected
        "warn"  | "msg"               | "value" | message
        "warn"  | { it -> "$it msg" } | "value" | "$value msg"
        "info"  | "msg"               | "value" | message
        "info"  | { it -> "$it msg" } | "value" | "$value msg"
        "debug" | "msg"               | "value" | message
        "debug" | { it -> "$it msg" } | "value" | "$value msg"
        "error" | "msg"               | "value" | message
        "error" | { it -> "$it msg" } | "value" | "$value msg"
        situation = message instanceof String ? "static" : "enclosed"
    }

    def "error level logger can log exception once"() {
        given:
        def logger = Mock(Logger)
        def provider = project.provider {""}.logError(logger, message, exception)

        when:
        provider.get()
        provider.get()

        then:
        1 * logger.error(message, exception)

        where:
        message  = "an error happened"
        exception = new Exception("My exception")
    }
}
