package com.wooga.gradle.io

import com.wooga.gradle.ArgumentsSpec
import com.wooga.gradle.MockTask
import com.wooga.gradle.MockTaskIntegrationSpec
import com.wooga.gradle.test.BatchmodeWrapper
import com.wooga.gradle.test.PropertyQueryTaskWriter
import com.wooga.gradle.test.queries.TestValue
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import spock.lang.Unroll

class ExecSpecImplTask extends MockTask implements ExecSpec, ArgumentsSpec, OutputStreamSpecExtended {

    @Nested
    LogFile stdoutFile = objects.newInstance(LogFile)
    @Nested
    LogFile stderrFile = objects.newInstance(LogFile)

    @TaskAction
    void run() {
        ExecResult execResult = ProcessExecutor.from(this)
            .withArguments(this)
            .withOutputLogFile(this, stdoutFile, stderrFile)
            .execute()
        execResult.assertNormalExitValue()
    }
}

class ExecSpecImplTaskIntegrationSpec extends MockTaskIntegrationSpec<ExecSpecImplTask> {

    @Unroll
    def "can set value #rawValue of type #type with method #method expecting path #expectedPath, name #expectedName and directory #expectedDirectory"() {
        given: "a custom task"
        buildFile << """
            ${subjectUnderTestName} {
            }
        """.stripIndent()

        and: "a set property"
        buildFile << """
            ${path}.${method}($value)
        """.stripIndent()

        when:
        def filePathQuery = new PropertyQueryTaskWriter("${path}.executable")
        filePathQuery.write(buildFile)
        def nameQuery = new PropertyQueryTaskWriter("${path}.executableName")
        nameQuery.write(buildFile)
        def directoryQueryWriter = new PropertyGetterTaskWriter("${path}.executableDirectory")
        directoryQueryWriter.write(buildFile)

        def result = runTasksSuccessfully(filePathQuery.taskName, nameQuery.taskName, directoryQueryWriter.taskName)

        then:
        filePathQuery.matches(result, expectedPath)
        nameQuery.matches(result, expectedName)
        def directoryQuery = directoryQueryWriter.generateQuery(this, result)
        directoryQuery.matches(expectedDirectory)

        where:
        method                       | type                    | rawValue              | expectedPath          | expectedName | expectedDirectory
        "setExecutableByPath"        | "String"                | "foobar"              | "foobar"              | "foobar"     | null
        "setExecutableByPath"        | "Provider<String>"      | "foobar"              | "foobar"              | "foobar"     | null
        "setExecutableByFile"        | "File"                  | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutableByFile"        | "Provider<File>"        | "pancakes"            | "pancakes"            | "pancakes"   | null
        "setExecutableByRegularFile" | "RegularFile"           | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutableByRegularFile" | "Provider<RegularFile>" | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutableDirectory"     | "Directory"             | "foo/bar"             | null                  | null         | TestValue.projectFile("foo/bar")
        "setExecutableName"          | "String"                | "wooby"               | "wooby"               | "wooby"      | null

        value = wrapValueBasedOnType(rawValue, type)
        path = "${subjectUnderTestName}"
    }

    def "set executable is properly executed by task"() {
        given: "a mock executable"
        def file = generateBatchWrapper("superkatzen", false)

        and: "it being set onto the task"
        appendToSubjectTask("setExecutableByFile(${wrapValueBasedOnType(file.path, File)})")

        and: "additional arguments being set"
        appendToSubjectTask("setAdditionalArguments(${wrapValueBasedOnType(arg, String)})")

        when:
        def result = runTasksSuccessfully(subjectUnderTestName)

        then:
        outputContains(result, arg)

        where:
        arg = "foobar24"
    }

    @Unroll
    def "can set exec spec property #property with value #value"() {

        expect:
        runPropertyQuery(getter, setter).matches(value)

        where:
        property           | value              | type
        "workingDirectory" | osPath("/foo/bar") | String

        setter = new PropertySetterWriter(subjectUnderTestName, property)
            .set(value, type)
            .toScript()

        getter = new PropertyGetterTaskWriter(setter)
    }

    @Unroll
    def "can write stdout #stdout and stderr #stderr when logToStdout #logToStdout and logToStderr #logToStderr"() {
        given:
        def wrapper = new BatchmodeWrapper("foobar")
        wrapper.withText("""
        ${stdout != null ? "echo \"${stdout}\"" : ""}
        ${stderr != null ? "1>&2 echo \"${stderr}\"" : ""}
        """.stripIndent())
        def file = wrapper.toTempFile()

        and: "a configured log file spec"
        appendToSubjectTask("""
        setExecutableByFile(${wrapValueBasedOnType(file.path, File)})
        stdoutFile.logFile = ${wrapValueBasedOnType(stdoutFile != null ? stdoutFile.path : null, File)}
        stderrFile.logFile = ${wrapValueBasedOnType(stderrFile != null ? stderrFile.path : null, File)}
        logToStdout = ${wrapValueBasedOnType(logToStdout, Boolean)}
        logToStderr = ${wrapValueBasedOnType(logToStderr, Boolean)}
        """.stripIndent())

        when:
        def result = runTasksSuccessfully(subjectUnderTestName)

        then:
        if (stdout != null) {
            if (stdoutFile != null) {
                assert stdoutFile.text.contains(stdout)
            }
            if (logToStdout) {
                result.standardOutput.contains(stdout)
            }
        }
        if (stderr != null) {
            if (stderrFile != null) {
                assert stderrFile.text.contains(stderr)
            }
            if (logToStderr) {
                result.standardError.contains(stderr)
            }
        }

        where:
        stdout   | stderr  | stdoutFile                            | stderrFile                            | logToStdout | logToStderr
        "hooray" | null    | File.createTempFile("stdout", ".log") | null                                  | true        | true
        "hooray" | null    | File.createTempFile("stdout", ".log") | null                                  | false       | true
        null     | "oh no" | null                                  | File.createTempFile("stderr", ".log") | true        | true
        null     | "oh no" | null                                  | File.createTempFile("stderr", ".log") | true        | false
        "oh yes" | "oh no" | File.createTempFile("stdout", ".log") | File.createTempFile("stderr", ".log") | true        | true
    }
}

