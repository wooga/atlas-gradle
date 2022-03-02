package com.wooga.gradle

import com.wooga.gradle.io.LogFileSpec
import com.wooga.gradle.io.OutputStreamSpec
import com.wooga.gradle.test.PropertyQueryTaskWriter
import nebula.test.ProjectSpec
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import spock.lang.Specification
import spock.lang.Unroll

class PropertyLookupTest extends Specification {

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
}

class PropertyLookupProviderSpec extends ProjectSpec {

    @Unroll
    def "gets value #value from object provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getObjectValueProvider(project)

        when:
        def actual = provider.get()

        then:
        actual == value

        where:
        value << [true, 42, "foobar"]
    }

    @Unroll
    def "gets value #value from string provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(value)

        and: "a provider for it"
        def provider = lookup.getStringValueProvider(project)

        when:
        def actual = provider.get()

        then:
        actual == value

        where:
        value << ["foobar", ""]
    }

    @Unroll
    def "gets value #expected from integer provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(input)

        and: "a provider for it"
        def provider = lookup.getIntegerValueProvider(project)

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        input | expected
        0     | 0
        -42   | -42
        42    | 42
        "42"  | 42
    }

    enum SuperCoolEnum {
        hot,
        cold
    }

    @Unroll
    def "gets enum value #expected from object provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(input)

        and: "a provider for it"
        def provider = lookup.getObjectValueProvider(project).map({
            SuperCoolEnum.valueOf(it.toString())
        }
        )

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        input              | expected
        "hot"              | SuperCoolEnum.hot
        SuperCoolEnum.cold | SuperCoolEnum.cold
    }

    @Unroll
    def "gets enum value #expected from generic provider"() {

        given: "a property lookup"
        def lookup = new PropertyLookup(input)

        and: "a provider for it"
        def provider = lookup.getValueProvider(project, { SuperCoolEnum.valueOf(it.toLowerCase()) })

        when:
        def actual = provider.get()

        then:
        actual == expected

        where:
        input              | expected
        "hot"              | SuperCoolEnum.hot
        "HOT"              | SuperCoolEnum.hot
        SuperCoolEnum.cold | SuperCoolEnum.cold
    }

}
