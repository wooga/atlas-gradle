package com.wooga.gradle

import spock.lang.Specification
import spock.lang.Unroll

class PropertyUtilsSpec extends Specification {

    @Unroll
    def "environment name is converted from extension and property name"() {
        expect:
        PropertyUtils.envNameFromProperty(extensionName, propertyName) == expectedEnvironmentName

        where:
        extensionName | propertyName          || expectedEnvironmentName
        "foobar"      | "kat.zen"             || "FOOBAR_KAT_ZEN"
        "foobar"      | "fooBar"              || "FOOBAR_FOO_BAR"
        "foobar"      | "foo.bar"             || "FOOBAR_FOO_BAR"
        "foobar"      | "fooBar.barFoo"       || "FOOBAR_FOO_BAR_BAR_FOO"
        "foobar"      | "foo2Bar.bar2Foo"     || "FOOBAR_FOO_2_BAR_BAR_2_FOO"
        "foobar"      | "foo2bar.bar2foo"     || "FOOBAR_FOO_2BAR_BAR_2FOO"
        "foobar"      | "foo44Bar.bar5555Foo" || "FOOBAR_FOO_44_BAR_BAR_5555_FOO"
        "foobar"      | "foo22bar.bar2222foo" || "FOOBAR_FOO_22BAR_BAR_2222FOO"
    }

    @Unroll
    def "environment name is converted from property name"() {
        expect:
        PropertyUtils.envNameFromProperty(propertyName) == expectedEnvironmentName

        where:
        propertyName          || expectedEnvironmentName
        "fooBar"              || "FOO_BAR"
        "foo.bar"             || "FOO_BAR"
        "fooBar.barFoo"       || "FOO_BAR_BAR_FOO"
        "foo2Bar.bar2Foo"     || "FOO_2_BAR_BAR_2_FOO"
        "foo2bar.bar2foo"     || "FOO_2BAR_BAR_2FOO"
        "foo44Bar.bar5555Foo" || "FOO_44_BAR_BAR_5555_FOO"
        "foo22bar.bar2222foo" || "FOO_22BAR_BAR_2222FOO"
    }

    @Unroll
    def "toCamelCase converts '#input' to camel case '#expectedValue"() {
        expect:
        PropertyUtils.toCamelCase(input) == expectedValue

        where:
        input                     || expectedValue
        "FOO_BAR"                 || "fooBar"
        "FOO_BAR_BAR_FOO"         || "fooBarBarFoo"
        "FOO_2_BAR_BAR_2_FOO"     || "foo2BarBar2Foo"
        "FOO_2BAR_BAR_2FOO"       || "foo2barBar2foo"
        "FOO_44_BAR_BAR_5555_FOO" || "foo44BarBar5555Foo"
        "FOO_22BAR_BAR_2222FOO"   || "foo22barBar2222foo"
        "foo_bar"                 || "fooBar"
        "foo_bar_bar_foo"         || "fooBarBarFoo"
        "foo_2_bar_bar_2_foo"     || "foo2BarBar2Foo"
        "foo_2bar_bar_2foo"       || "foo2barBar2foo"
        "foo_44_bar_bar_5555_foo" || "foo44BarBar5555Foo"
        "foo_22bar_bar_2222foo"   || "foo22barBar2222foo"
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

