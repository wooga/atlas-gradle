package com.wooga.gradle.io

import com.wooga.gradle.ArgumentsSpec
import com.wooga.gradle.MockTask
import com.wooga.gradle.MockTaskIntegrationSpec
import com.wooga.gradle.PlatformUtils
import com.wooga.gradle.test.BatchmodeWrapper
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import spock.lang.Unroll

import static com.wooga.gradle.test.queries.TestValue.projectFile

class ExecSpecImplTask extends MockTask implements ExecSpec, ArgumentsSpec, ProcessOutputSpec {

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

    static Boolean compareCaseInsensitive(String value, String expectedValue) {
        if ((value == null && expectedValue != null) || (value != null && expectedValue == null)) {
            return false
        }
        value.toLowerCase() == expectedValue.toLowerCase()
    }

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
        def filePathGet = new PropertyGetterTaskWriter("${path}.executable")
        def nameGet = new PropertyGetterTaskWriter("${path}.executableName")
        def directoryGet = new PropertyGetterTaskWriter("${path}.executableDirectory")

        filePathGet.write(buildFile)
        nameGet.write(buildFile)
        directoryGet.write(buildFile)

        def result = runTasksSuccessfully(filePathGet.taskName, nameGet.taskName, directoryGet.taskName)

        then:
        def filePathQuery = filePathGet.generateQuery(this, result)
        if (PlatformUtils.windows) {
            compareCaseInsensitive(filePathQuery.getValue(), expectedPath)
        } else {
            filePathQuery.matches(expectedPath)
        }
        nameGet.generateQuery(this, result).matches(expectedName)

        def directoryQuery = directoryGet.generateQuery(this, result)
        if (PlatformUtils.windows) {
            compareCaseInsensitive(directoryQuery.getValue(), expectedDirectory as String)
        } else {
            directoryQuery.matches(expectedDirectory)
        }

        where:
        method                   | type                    | rawValue              | expectedPath          | expectedName | expectedDirectory
        "setExecutable"          | "String"                | "foobar"              | "foobar"              | "foobar"     | null
        "setExecutable"          | "String"                | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "Provider<String>"      | "foobar"              | "foobar"              | "foobar"     | null
        "setExecutable"          | "Provider<String>"      | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "File"                  | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "Provider<File>"        | "pancakes"            | "pancakes"            | "pancakes"   | null
        "setExecutable"          | "RegularFile"           | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "Provider<RegularFile>" | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "Closure<File>"         | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutable"          | "Closure<String>"       | "foobar"              | "foobar"              | "foobar"     | null
        "setExecutable"          | "Closure<String>"       | osPath("/bin/foobar") | osPath("/bin/foobar") | "foobar"     | osPath("/bin")
        "setExecutableDirectory" | "Directory"             | "foo/bar"             | null                  | null         | projectFile("foo/bar")
        "setExecutableName"      | "String"                | "wooby"               | "wooby"               | "wooby"      | null

        value = wrapValueBasedOnType(rawValue, type)
        path = "${subjectUnderTestName}"
    }

    def "set executable is properly executed by task"() {
        given: "a mock executable"
        def file = generateBatchWrapper("superkatzen", false)

        and: "it being set onto the task"
        appendToSubjectTask("setExecutable(${wrapValueBasedOnType(file.path, File)})")

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
        setExecutable(${wrapValueBasedOnType(file.path, File)})
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

