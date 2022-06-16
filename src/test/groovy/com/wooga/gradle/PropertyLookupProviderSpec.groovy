package com.wooga.gradle

import com.wooga.gradle.test.IntegrationSpec
import nebula.test.ProjectSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import spock.lang.Unroll

class PropertyLookupProviderSpec extends ProjectSpec {

    @Unroll
    def "gets value `#expected` from object provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getObjectValueProvider(project, defaultValue)

        when:
        def actual = provider.get()

        then:
        expected == actual

        where:
        value    | defaultValue
        true     | null
        true     | false
        42       | null
        42       | 24
        "foobar" | null
        "foobar" | "barfoo"
        expected = (defaultValue != null) ? defaultValue : value
    }

    @Unroll
    def "gets value `#expected` from string provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getStringValueProvider(project, defaultValue)

        when:
        def actual = provider.get()

        then:
        expected == actual

        where:
        value    | defaultValue
        "foobar" | null
        ""       | null
        "foobar" | "barfoo"
        null     | "barfoo"
        expected = defaultValue ?: value
    }

    @Unroll
    def "gets value `#expected` from boolean provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getBooleanValueProvider(project, defaultValue)

        when:
        def actual = provider.get()

        then:
        expected == actual

        where:
        value | defaultValue
        true  | null
        true  | false
        false | null
        false | true
        null  | true
        null  | false
        expected = (defaultValue != null) ? defaultValue : value
    }

    @Unroll
    def "gets value #expected from integer provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getIntegerValueProvider(project, defaultValue)

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        value | defaultValue | expected
        0     | null         | 0
        -42   | null         | -42
        42    | null         | 42
        "42"  | null         | 42
        0     | 7            | 7
        -42   | 42           | 42
        42    | -42          | -42
        "42"  | "54"         | 54
    }

    enum SuperCoolEnum {
        hot,
        cold
    }

    @Unroll
    def "gets enum value #expected from object provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getObjectValueProvider(project, defaultValue).map({
            SuperCoolEnum.valueOf(it.toString())
        })

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        value              | defaultValue      | expected
        "hot"              | null              | SuperCoolEnum.hot
        SuperCoolEnum.cold | null              | SuperCoolEnum.cold
        "hot"              | "cold"            | SuperCoolEnum.cold
        SuperCoolEnum.cold | SuperCoolEnum.hot | SuperCoolEnum.hot
    }

    @Unroll
    def "gets enum value #expected from generic provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getValueProvider(project, {
            SuperCoolEnum.valueOf(it.toLowerCase())
        }, defaultValue)

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        value              | defaultValue      | expected
        "hot"              | null              | SuperCoolEnum.hot
        "HOT"              | null              | SuperCoolEnum.hot
        SuperCoolEnum.cold | null              | SuperCoolEnum.cold
        "hot"              | "cold"            | SuperCoolEnum.cold
        "HOT"              | "COLD"            | SuperCoolEnum.cold
        SuperCoolEnum.cold | SuperCoolEnum.hot | SuperCoolEnum.hot
    }

    def "overrides default value set on property"() {

        given: "a constructed lookup"
        def lookup = new PropertyLookup(defaultValue)

        when: "a generated provider"
        def provider = lookup.getStringValueProvider(project)

        then: "return the initial default value"
        defaultValue == provider.get()

        when: "overriding the default value"
        lookup.defaultValue = newDefaultValue

        then: "return the new default value"
        newDefaultValue == provider.get()

        where:
        defaultValue = "FOO"
        newDefaultValue = 'BAR'
    }

    @Unroll
    def "gets enum value #expected from enum provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getEnumValueProvider(project, SuperCoolEnum.class, defaultValue)

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        value              | defaultValue      | expected
        "hot"              | null              | SuperCoolEnum.hot
        SuperCoolEnum.cold | null              | SuperCoolEnum.cold
        "hot"              | "cold"            | SuperCoolEnum.cold
        SuperCoolEnum.cold | SuperCoolEnum.hot | SuperCoolEnum.hot
    }


    def "gets property value from project's properties"() {
        given: "a property lookup"
        def lookup = new PropertyLookup(null, propertyKey, null)

        and: "a value in the project"
        project.extensions.add(propertyKey, propertyValue)

        when:
        def actual = lookup.getValue(project)

        then:
        actual == propertyValue

        where:
        propertyKey | propertyValue
        "foo"       | "bar"
    }

    @Unroll
    def "generates correct provider of type #type with value #value from lookup when set"() {
        given: "a property lookup"
        def lookup = new PropertyLookup(null, propertyKey, null)
            .of(type)

        and: "a value in the project"
        project.extensions.add(propertyKey, value)

        and: "a generated provider"
        def provider = lookup.getDefaultProvider(project)
        assert provider != null
        assert value == lookup.getValue(project)

        when:
        Object expected = value
        Object actual
        if (type == File || type == RegularFile) {
            actual = ((Provider<RegularFile>) provider).get().asFile.path
        } else if (type == Directory) {
            actual = ((Provider<Directory>) provider).get().asFile.path
        } else {
            actual = provider.get()
        }

        then:
        actual == expected

        where:
        type        | value
        String      | "foobar"
        Integer     | 24
        Boolean     | true
        Boolean     | false
        File        | IntegrationSpec.osPath("/foobar.txt")
        RegularFile | IntegrationSpec.osPath("/foobar.txt")
        Directory   | IntegrationSpec.osPath("/foo/bar")

        propertyKey = "pancakes"
    }
}
