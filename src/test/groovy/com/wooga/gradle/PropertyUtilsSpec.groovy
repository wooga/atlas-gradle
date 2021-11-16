package com.wooga.gradle

import spock.lang.Specification
import spock.lang.Unroll

class PropertyUtilsSpec extends Specification {

    def "environment name is converted from extension and property name"() {
        when:
        def environmentName = PropertyUtils.envNameFromProperty(extensionName, propertyName)

        then:
        environmentName == expectedEnvironmentName

        where:
        extensionName | propertyName | expectedEnvironmentName
        "foobar"      | "kat.zen"    | "FOOBAR_KAT_ZEN"
    }

    def "environment name is converted from property name"() {
        when:
        def environmentName = PropertyUtils.envNameFromProperty(propertyName)

        then:
        environmentName == expectedEnvironmentName

        where:
        propertyName | expectedEnvironmentName
        "kat.zen"    | "KAT_ZEN"
    }

    @Unroll
    def "toProviderSet returns a provider set method invocation #expectedResult from property #property"() {
        expect:
        PropertyUtils.toProviderSet(property) == expectedResult

        where:
        property      | expectedResult
        "foo"         | "foo.set"
        "foo.bar"     | "foo.bar.set"
        "foo.bar.baz" | "foo.bar.baz.set"
    }

    @Unroll
    def "toSetter returns a setter method invocation #expectedResult from property #property"() {
        expect:
        PropertyUtils.toSetter(property) == expectedResult

        where:
        property      | expectedResult
        "foo"         | "setFoo"
        "foo.bar"     | "foo.setBar"
        "foo.bar.baz" | "foo.bar.setBaz"

    }


}
