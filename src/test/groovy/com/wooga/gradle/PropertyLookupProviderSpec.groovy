package com.wooga.gradle

import nebula.test.ProjectSpec
import org.gradle.api.Project
import org.gradle.api.file.Directory
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
    def "sets `#value`, gets value `#expected` List<String> provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getListStringValueProvider(project, defaultValue)

        when:
        def actual = provider.getOrNull()

        then:
        expected == actual

        where:
        value              | expected                 | defaultValue
        "foobar"           | ["foobar"]               | null
        "foobar"           | ["barfoo"]               | "barfoo"
        "[foobar]"         | ["foobar"]               | null
        "foo,bar"          | ["foo", "bar"]           | null
        "[foo,bar]"        | ["foo", "bar"]           | null
        "foo;bar;foobar"   | ["foo", "bar", "foobar"] | null
        "[foo;bar;foobar]" | ["foo", "bar", "foobar"] | null
        ""                 | null                     | null
        null               | null                     | null
        null               | ["barfoo"]               | "barfoo"
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
        def actual = provider.getOrNull()

        then:
        actual == expected

        where:
        value              | defaultValue      | expected
        "hot"              | null              | SuperCoolEnum.hot
        SuperCoolEnum.cold | null              | SuperCoolEnum.cold
        "hot"              | "cold"            | SuperCoolEnum.cold
        SuperCoolEnum.cold | SuperCoolEnum.hot | SuperCoolEnum.hot
        ""                 | null              | null
        ""                 | "cold"            | SuperCoolEnum.cold
        null               | null              | null
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
    def "gets file property value relative to base directory"() {
        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a file provider for it"
        def baseDir = project.provider { baseDirFactory(project) as Directory }
        def provider = lookup.getFileValueProvider(project, null, baseDir)

        when:
        def actual = provider.orNull

        then:
        def expected = value ? baseDir.get().file(value) : null
        expected == actual

        where:
        value       | baseDirFactory
        null        | { Project p -> p.layout.projectDirectory }
        "file"      | { Project p -> p.layout.projectDirectory }
        "otherFile" | { Project p -> p.layout.projectDirectory.dir("dir") }
    }

    @Unroll
    def "gets directory property value relative to base directory"() {
        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a file provider for it"
        def baseDir = project.provider { baseDirFactory(project) as Directory }
        def provider = lookup.getDirectoryValueProvider(project, null, baseDir)

        when:
        def actual = provider.orNull

        then:
        def expected = value ? baseDir.get().dir(value) : null
        expected == actual

        where:
        value    | baseDirFactory
        null     | { Project p -> p.layout.projectDirectory }
        "dir"    | { Project p -> p.layout.projectDirectory }
        "subdir" | { Project p -> p.layout.projectDirectory.dir("dir") }
    }

}
