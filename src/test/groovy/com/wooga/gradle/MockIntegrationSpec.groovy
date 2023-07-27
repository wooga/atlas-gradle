package com.wooga.gradle

import org.gradle.api.DefaultTask

import java.lang.reflect.ParameterizedType
import java.nio.file.Files

abstract class MockIntegrationSpec extends com.wooga.gradle.test.IntegrationSpec {

    static File generateBatchWrapper(String fileName, Boolean printEnvironment = false) {
        File wrapper

        wrapper = Files.createTempFile(fileName, ".bat").toFile()
        wrapper.deleteOnExit()
        wrapper.executable = true
        if (PlatformUtils.windows) {
            wrapper << """
                    @echo off
                    echo [ARGUMENTS]:
                    echo %*
                """.stripIndent()

            if (printEnvironment) {
                wrapper << """
                    echo [ENVIRONMENT]:
                    set
                """.stripIndent()
            }

        } else {
            wrapper << """
                    #!/usr/bin/env bash
                    echo [ARGUMENTS]:
                    echo \$@
                """.stripIndent()

            if (printEnvironment) {
                wrapper << """
                    echo [ENVIRONMENT]:
                    env
                """.stripIndent()
            }
        }

        wrapper
    }
}

abstract class MockPluginIntegrationSpec<T extends MockPlugin> extends com.wooga.gradle.test.IntegrationSpec {

    Class<T> getPluginClass() {
        if (!_pluginClass) {
            try {
                this._pluginClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
            }
            catch (Exception e) {
                this._pluginClass = (Class<T>) MockPlugin
            }
        }
        _pluginClass
    }
    private Class<T> _pluginClass

    def setup() {
        buildFile << "${applyPlugin(pluginClass)}${System.lineSeparator()}"
    }

    void appendToExtension(String... lines) {
        buildFile << """ ${MockPlugin.extensionName} {
        ${lines.join('\n')}
        }
        """.stripIndent()
    }
}

class MockTask extends DefaultTask {
}

abstract class MockTaskIntegrationSpec<T extends MockTask> extends MockIntegrationSpec {

    Class<T> getSubjectUnderTestClass() {
        if (!_sutClass) {
            try {
                this._sutClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
            }
            catch (Exception e) {
                this._sutClass = (Class<T>) MockTask
            }
        }
        _sutClass
    }
    private Class<T> _sutClass

    String getSubjectUnderTestName() {
        "${subjectUnderTestClass.simpleName.uncapitalize()}"
    }

    String getSubjectUnderTestTypeName() {
        subjectUnderTestClass.getTypeName()
    }

    def setup() {
        addMockTask(false)
    }

    void addMockTask(Boolean force, String... lines) {
        addTask(subjectUnderTestName, subjectUnderTestTypeName, force, lines)
    }

    void appendToSubjectTask(String... lines) {
        buildFile << """
        $subjectUnderTestName {
            ${lines.join('\n')}
        }
        """.stripIndent()
    }

    def runSubjectTaskSuccessfully() {
        runTasksSuccessfully(subjectUnderTestName)
    }

    String addTask(String name, String typeName, Boolean force, String... lines) {
        lines = lines ?: []
        buildFile << """
        task (${name}, type: ${typeName}) {                       
            ${force ? "onlyIf = {true}\n" : ""}${lines.join('\n')}
        }
        """.stripIndent()
    }
}
