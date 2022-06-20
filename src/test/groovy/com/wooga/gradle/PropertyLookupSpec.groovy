package com.wooga.gradle


import spock.lang.Specification
import spock.lang.Unroll

class PropertyLookupSpec extends Specification {

    @Unroll
    def "returns value #expected"() {

        given:
        if (props == _) {
            props = [:]
        }
        if (env == _) {
            env = [:]
        }

        when:
        def lookup = new PropertyLookup(envKeys, propKeys, defaultValue)


        then:
        def actual = lookup.getValue(props, env)
        actual == expected

        where:
        expected | props                    | env             | propKeys | envKeys | defaultValue
        "FOO"    | _                        | ["B": "FOO"]    | "A"      | "B"     | "FOOBAR"
        "FOOBAR" | _                        | _               | "A"      | "B"     | { "FOOBAR" }
        "FU"     | _                        | ["A": { "FU" }] | "B"      | "A"     | "FUBAR"
        "FU"     | ["D": { "FU" }]          | _               | "D"      | "D"     | "FUBAR"
        "FOO"    | ["A": "FOO"]             | ["A": "BAR"]    | "A"      | _       | "FOOBAR"
        "FOOBAR" | ["C": "FOO"]             | ["D": "BAR"]    | "A"      | _       | "FOOBAR"
        "FOOBAR" | ["C": "FOO"]             | ["D": "BAR"]    | "A"      | _       | "FOOBAR"
        "WAH"    | ["A": "FOO", "C": "WAH"] | ["A": "BAR"]    | "C"      | _       | "FOOBAR"
        7        | _                        | _               | "C"      | "B"     | 7
    }

    def "returns default value when lookups fail"() {

        given:
        def props = [:]
        def env = [:]
        def propsKey = "A"
        def envKey = "B"

        when:
        def lookup = new PropertyLookup(envKey, propsKey, defaultValue)

        then:
        def actual = lookup.getValue(props, env)
        actual == expected

        where:
        defaultValue = "FOO"
        expected = defaultValue
    }

    def "returns properties before environment value"() {

        when:
        def lookup = new PropertyLookup(envKey, propsKey, defaultValue)

        then:
        def actual = lookup.getValue(props, env)
        actual == expected

        where:
        defaultValue = "FOO"
        propsKey = "A"
        envKey = "B"
        props = ["A": "7"]
        env = ["B": "3"]

        expected = "7"
    }

    def "creates lookup with generated environment key"() {
        when:
        def lookup = PropertyLookup.WithEnvironmentKeyFromProperty(propertyKey, defaultValue)

        then:
        lookup.environmentKeys.size() == 1
        lookup.environmentKeys[0] == expectedEnvironmentKey
        def actual = lookup.getValue(props, env)
        actual == expected

        where:
        propertyKey | expectedEnvironmentKey
        "bread.ham" | "BREAD_HAM"
        defaultValue = "FOO"
        expected = 7
        props = ["bread.ham": 7]
        env = ["BREAD_HAM": 7]
    }

    def "gets default value"() {
        given: "a property lookup"
        def lookup = new PropertyLookup(propertyValue)

        when:
        def actual = lookup.getValue()

        then:
        actual == propertyValue

        where:
        propertyValue = "bar"
    }

    def "gets all property lookups from mock conventions class"() {
        when:
        def lookups = PropertyLookup.getAll(MockConventions)

        then:
        lookups.size() == 3
        lookups.containsAll(MockConventions.name, MockConventions.version, MockConventions.directory)
    }

    def "gets all environment variables from mock conventions class"() {
        when:
        def envVars = PropertyLookup.getEnvironmentVariables(MockConventions)

        then:
        envVars.size() == 3
        envVars.containsAll("MOCK_NAME", "MOCK_VERSION", "MOCK_DIR")
    }

    def "gets all properties from mock conventions class"() {
        when:
        def properties = PropertyLookup.getGradleProperties(MockConventions)

        then:
        properties.size() == 3
        properties.containsAll("mock.name", "mock.version", "mock.dir")
    }
}

