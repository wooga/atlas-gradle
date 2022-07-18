package com.wooga.gradle.io


import com.wooga.gradle.test.BatchmodeWrapper
import nebula.test.ProjectSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Unroll

class ProcessExecutorTaskSpec extends ProjectSpec {

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    def "can execute"() {
        given:
        def file = new BatchmodeWrapper("foobar").toTempFile()

        expect:
        ProcessExecutor.from(project)
            .withExecutable(file)
            .execute()
            .assertNormalExitValue()
    }

    @Unroll
    def "can execute with arguments #arguments"() {
        given:
        def file = new BatchmodeWrapper("foobar").withEnvironment(false).toTempFile()

        and: "stdout stream to verify arguments and environment passed"
        def output = new ByteArrayOutputStream()
        when:
        def result = ProcessExecutor.from(project)
                .withExecutable(file)
                .withArguments(arguments)
                .withStandardOutput(output)
                .execute()

        then:
        result.assertNormalExitValue()
        def o = output.toString()

        def argumentCheck = (arguments ? [arguments] : []).flatten().toList().join(" ")
        o.contains("[ARGUMENTS]:${System.getProperty("line.separator")}${argumentCheck}")

        where:
        arguments      | _
        "foobar"       | _
        ["foo", "bar"] | _
        ["foo", "bar"] | _
        []             | _
    }

    @Unroll
    def "can execute with environment #environment and append #appendEnvironment"() {
        given:
        def file = new BatchmodeWrapper("foobar").toTempFile()

        and: "stdout stream to verify arguments and environment passed"
        def output = new ByteArrayOutputStream()

        and: "a base environment"
        env.set("BaseValue", "BaseEntry")

        when:
        def result = ProcessExecutor.from(project)
                .withExecutable(file)
                .withEnvironment(environment, appendEnvironment)
                .withStandardOutput(output)
                .execute()

        then:
        result.assertNormalExitValue()
        def o = output.toString()

        def environmentsLines = o.split(/\[ENVIRONMENT\]\:${System.getProperty("line.separator")}/).last().readLines()
        def environmentCheck = (environment ?: []).collect { key, value -> "${key}=${value}".toString() }
        environmentsLines.containsAll(environmentCheck)
        environmentsLines.contains("BaseValue=BaseEntry") == appendEnvironment

        where:
        environment                  | appendEnvironment
        [:]                          | false
        [:]                          | true
        ["FOO": "BAR", "BAR": "BAZ"] | false
        ["FOO": "BAR"]               | true
    }

    static class MockOutput implements ProcessOutputSpec {
    }

    def "can write to stdout"() {
        given:
        def mock = project.objects.newInstance(MockOutput)
        mock.logToStdout.set(true)
        mock.logToStderr.set(true)

        def stdoutFile = File.createTempFile("stdout", ".log")
        def file = new BatchmodeWrapper("foobar").toTempFile()

        when:
        def result = ProcessExecutor.from(project)
            .withExecutable(file)
            .withOutput(mock, stdoutFile)
            .execute()

        then:
        result.assertNormalExitValue()
        stdoutFile.exists()
        stdoutFile.text.contains("[ARGUMENTS]")
    }

    def "can write to stderr"() {
        given:
        def file = new BatchmodeWrapper("foobar")
            .withText("1>&2 \"${text}\"")
            .withExitValue(exitValue)
            .toTempFile()

        def target = project.objects.newInstance(MockOutput)
        target.logToStdout.set(true)
        target.logToStderr.set(true)

        def stderrFile = File.createTempFile("stderr", ".log")

        when:
        def result = ProcessExecutor.from(project)
            .withExecutable(file)
            .withOutput(target, null, stderrFile)
            .ignoreExitValue()
            .execute()

        then:
        result.exitValue == exitValue
        stderrFile.exists()
        stderrFile.text.contains(text)

        where:
        exitValue | text
        42        | "oh no"
    }

    @Unroll
    def "can write output stream with stdout #stdout and stderr #stderr"() {
        given:
        def wrapper = new BatchmodeWrapper("foobar")
        wrapper.withText("""
        ${stdout != null ? "echo \"${stdout}\"" : ""}
        ${stderr != null ? "1>&2 echo \"${stderr}\"" : ""}
        """.stripIndent())

        def file = wrapper.toTempFile()
        def target = project.objects.newInstance(MockOutput)
        // TODO: How to test this here?
        target.logToStdout.set(logToStdout)
        target.logToStderr.set(logToStdErr)

        when:
        def result = ProcessExecutor.from(project)
            .withExecutable(file)
            .withOutput(target, stdoutFile, stdErrFile)
            .with({
                it.environment.clear()
            })
            .execute()

        then:
        result.exitValue == 0
        if (stdout != null) {
            if (stdoutFile != null) {
                assert stdoutFile.text.contains(stdout)
            }
        }
        if (stderr != null) {
            if (stdErrFile != null) {
                assert stdErrFile.text.contains(stderr)
            }
        }

        where:
        stdout   | stderr  | stdoutFile                            | stdErrFile                            | logToStdout | logToStdErr
        "hooray" | null    | File.createTempFile("stdout", ".log") | null                                  | true        | true
        "hooray" | null    | File.createTempFile("stdout", ".log") | null                                  | false       | true
        null     | "oh no" | null                                  | File.createTempFile("stderr", ".log") | true        | true
        null     | "oh no" | null                                  | File.createTempFile("stderr", ".log") | true        | false
        "oh yes" | "oh no" | File.createTempFile("stdout", ".log") | File.createTempFile("stderr", ".log") | true        | true
    }
}
