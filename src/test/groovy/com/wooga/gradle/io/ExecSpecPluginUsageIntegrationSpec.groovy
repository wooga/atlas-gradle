package com.wooga.gradle.io

import com.wooga.gradle.MockExtension
import com.wooga.gradle.MockIntegrationSpec
import com.wooga.gradle.MockPlugin
import com.wooga.gradle.MockPluginIntegrationSpec
import com.wooga.gradle.test.queries.TestValue
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import org.gradle.api.Project
import spock.lang.Unroll

class ExtensionWithExec extends MockExtension implements ExecSpec {
}

class MockPluginWithExtension extends MockPlugin<ExtensionWithExec> {
    @Override
    void apply(Project project, ExtensionWithExec extension) {
        project.tasks.withType(ExecSpecImplTask).configureEach { t ->
            t.setExecutable(extension.executable)
        }
    }
}

class ExecSpecPluginUsageIntegrationSpec extends MockPluginIntegrationSpec<MockPluginWithExtension> {

    private static String defaultExecutableName = "fork"

    @Unroll
    def "when #inputProperty is set to #inputValue, then #outputProperty is evaluated at the right time"() {

        given: "a configured extension with defaults"
        appendToExtension("""
        executableName = ${wrapValueBasedOnType(defaultExecutableName, String)}
        """.stripIndent())

        if (inputValue != null) {
            appendToExtension("${inputProperty} = ${wrapValueBasedOnType(inputValue, type)}".stripIndent())
        }

        and: "a task that should be mapped"
        addTask(taskName, ExecSpecImplTask, true)

        expect:
        if (outputValue == _) {
            outputValue = inputValue
        }
        runPropertyQuery(getter).matches(outputValue)

        where:
        inputProperty         | inputValue     | type   | outputProperty   | outputValue
        "executableDirectory" | null           | File   | "executable"     | defaultExecutableName
        "executableDirectory" | "usr/bin/asdf" | File   | "executable"     | TestValue.projectFile("usr/bin/asdf/fork")
        "executable"          | "foobar"       | String | "executable"     | _
        "executable"          | "foobar"       | String | "executableName" | _
        "executableName"      | null           | String | "executable"     | defaultExecutableName
        "executableName"      | "foobar"       | String | "executable"     | _
        "executableName"      | "foobar"       | String | "executableName" | _

        taskName = "pancakeLord"
        getter = new PropertyGetterTaskWriter("${taskName}.${outputProperty}")
    }
}
