package com.wooga.gradle.extensions

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.provider.MissingValueException
import spock.lang.Unroll

class ProviderExtensionsSpec extends ProjectSpec {

    def "points provider name on missing value"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider{null as String}.named(providerName)
        when:
        provider.get()
        then:
        def e = thrown(MissingValueException)
        e.message.contains("Cannot query the value of $providerName because it has no value available.")
    }

    def "points provider name on missing value on derived providers"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider{null as String}.named(providerName)
        when:
        provider.map{"something else"}.get()
        then:
        def e = thrown(MissingValueException)
        e.message.contains("The value of this provider is derived from: $providerName")
    }


    def "points provider name on missing value on zipped providers"() {
        given:
        def providerName = "my provider name"
        def provider = project.provider{null as String}.named(providerName)
        def otherProvider = project.provider{"value"}
        def zippedProvider = provider.zip(otherProvider){null as String}

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
        def provider = project.provider{throw baseException}.onError {
            Exception e -> throw new IllegalArgumentException(message, e)
        }

        when:
        providerResolvingOp(provider)

        then:
        def e = thrown(IllegalArgumentException) //topmost exception is org.gradle.internal.UncheckedException
        e.message == "my error"
        e.cause == baseException

        where:
        providerResolvingOp << [
                { p -> p.get()},
                { p -> p.orNull},
                { p -> p.getOrElse("otherValue")},
                { p -> p.map{"otherValue"}.get()},
        ]
        baseException = new Exception("base exception")
        message = "my error"
    }

    def "executes onError block on exception in zip"() {
        given:
        def provider = project.provider{throw baseException}.onError {
            Exception e -> throw new IllegalArgumentException(message, e)
        }

        when:
        provider.zip(project.provider{"other"}, {}).get()

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
        def provider = project.provider{throw baseException}
                .onError(new IllegalArgumentException(message, baseException))

        when:
        providerResolvingOp(provider)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "my error"
        e.cause == baseException

        where:
        providerResolvingOp << [
                { p -> p.get()},
                { p -> p.orNull},
                { p -> p.getOrElse("otherValue")},
                { p -> p.map{"otherValue"}.get()},
        ]
        baseException = new Exception("base exception")
        message = "my error"
    }

}
