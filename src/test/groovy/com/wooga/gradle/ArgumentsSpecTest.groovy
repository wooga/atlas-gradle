package com.wooga.gradle

import com.wooga.gradle.test.MapPropertyQueryTaskWriter
import com.wooga.gradle.test.PropertyQueryTaskWriter
import org.gradle.api.Action
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import spock.lang.Unroll

class ArgumentsUsingTask extends MockTask implements ArgumentsSpec {

    ArgumentsUsingTask() {
        internalArguments = project.provider({ composeCoolArguments() })
    }

    List<String> composeCoolArguments() {
        return ["foo", "bar"]
    }

    @TaskAction
    void run() {
        def _executable = MockIntegrationSpec.generateBatchWrapper("superkatzen", true)
        def _arguments = arguments.get()
        def _environment = environment.get()

        ExecResult execResult = project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec exec) {
                exec.with {
                    executable _executable
                    args = _arguments
                    environment = _environment
                }
            }
        })
    }
}

class ArgumentsSpecTest extends MockTaskIntegrationSpec<ArgumentsUsingTask> {

    def "runs task, generating composed arguments"() {
        when:
        def result = runTasksSuccessfully(subjectUnderTestName)
        then:
        result.success
    }

    @Unroll
    def "can configure arguments with #method #message"() {
        given: "a custom task"
        buildFile << """
            ${subjectUnderTestName} {
                arguments(["--test", "value"])
            }
        """.stripIndent()

        and: "a set property"
        buildFile << """
            ${subjectUnderTestName}.${method}($value)
        """.stripIndent()

        when:
        def query = new PropertyQueryTaskWriter("${subjectUnderTestName}.${property}")
        query.write(buildFile)
        def result = runTasksSuccessfully(query.taskName)

        then:
        query.matches(result, expectedValue)

        where:
        method                    | rawValue         | type                     | append | expectedValue
        "argument"                | "--foo"          | "String"                 | true   | ["--test", "value", "--foo"]
        "arguments"               | ["--foo", "bar"] | "List<String>"           | true   | ["--test", "value", "--foo", "bar"]
        "arguments"               | ["--foo", "bar"] | "String[]"               | true   | ["--test", "value", "--foo", "bar"]
        "setAdditionalArguments"  | ["--foo", "bar"] | "List<String>"           | false  | ["--foo", "bar"]
        "setAdditionalArguments"  | ["--foo", "bar"] | "Provider<List<String>>" | false  | ["--foo", "bar"]
        "additionalArguments.set" | ["--foo", "bar"] | "List<String>"           | false  | ["--foo", "bar"]
        "additionalArguments.set" | ["--foo", "bar"] | "Provider<List<String>>" | false  | ["--foo", "bar"]

        property = "additionalArguments"
        value = wrapValueBasedOnType(rawValue, type)
        message = (append) ? "which appends arguments" : "which replaces arguments"
    }

    @Unroll
    def "set environment variable #rawValue for task exec"() {
        given: "some clean environment variables"
        if (!PlatformUtils.windows) {
            def envNames = System.getenv().keySet().toArray()
            environmentVariables.clear(*envNames)
        }

        and: "a test value in system env"
        initialValue.each { key, v ->
            environmentVariables.set(key, v)
        }

        and: "an initial environment"
        appendToSubjectTask("setEnvironmentDefaults()")

        and: "an overridden environment"
        appendToSubjectTask("$method($value)")

        and: "some values in the user environment"
        environmentVariables.set("USER_A", "foo")

        when:
        def query = new MapPropertyQueryTaskWriter(propertyPath)
        query.write(buildFile)
        def result = runTasksSuccessfully(subjectUnderTestName, query.taskName)

        then:
        query.contains(result, rawValue)

        where:
        property      | useSetter | rawValue
        "environment" | true      | ["A": "foo"]
        "environment" | false     | ["A": "bar"]
        "environment" | true      | ["A": 7]
        "environment" | false     | ["A": 7]
        "environment" | true      | ["A": file("foo.bar")]
        "environment" | false     | ["A": file("foo.bar")]
        "environment" | true      | ["A": true]
        "environment" | false     | ["A": false]

        initialValue = ["B": "5", "C": "7"]
        method = (useSetter) ? "set${property.capitalize()}" : "${property}.set"
        value = wrapValueBasedOnType(rawValue, Map)
        propertyPath = "${subjectUnderTestName}.environment"
    }

    def "adds environment for task exec"() {
        given: "some clean environment variables"
        if (!PlatformUtils.windows) {
            def envNames = System.getenv().keySet().toArray()
            environmentVariables.clear(*envNames)
        }

        and: "an initial environment"
        appendToSubjectTask("setEnvironmentDefaults()")

        and: "a test value in system env"
        initialValue.each { key, v ->
            environmentVariables.set(key, v)
        }

        appendToSubjectTask("$method(${wrapValueBasedOnType(rawValue, Map)})")

        when:
        def query = new MapPropertyQueryTaskWriter(propertyPath)
        query.write(buildFile)
        def result = runTasksSuccessfully(subjectUnderTestName, query.taskName)

        then:
        query.contains(result, initialValue, rawValue)

        where:
        method               | rawValue   | initialValue
        "environment.putAll" | ["A": "7"] | ["B": "5"]

        value = wrapValueBasedOnType(rawValue, Map)
        propertyPath = "${subjectUnderTestName}.environment"
    }
}
