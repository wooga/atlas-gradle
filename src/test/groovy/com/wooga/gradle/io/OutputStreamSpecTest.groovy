package com.wooga.gradle.io

import com.wooga.gradle.MockIntegrationSpec
import com.wooga.gradle.MockTask
import com.wooga.gradle.MockTaskIntegrationSpec
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import spock.lang.Unroll

class OutputStreamTask extends MockTask implements LogFileSpec, OutputStreamSpec {

    @TaskAction
    void run() {
        def _executable = MockIntegrationSpec.generateBatchWrapper("superhund")
        def _logFile = logFile.present ? logFile.get().asFile : null

        ExecResult execResult = project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec exec) {
                exec.with {
                    executable _executable
                    args = ["foo", "bar"]
                    ignoreExitValue = true
                    standardOutput = getOutputStream(_logFile)
                }
            }
        })
    }
}

class OutputStreamSpecTest extends MockTaskIntegrationSpec<OutputStreamTask> {

    @Unroll
    def "runs task with logToStdout = #logToStdout, setLogFile = #setLogFile"() {

        given: "a task that will use an output stream"
        appendToSubjectTask("logToStdout=${logToStdout}")

        File mockFile = null
        if (setLogFile) {
            appendToSubjectTask("logFile=file(\"${mockFileName}\")")
            mockFile = file(mockFileName)
        }

        when:
        def result = runSubjectTaskSuccessfully()

        then:
        result.success
        logToStdout == result.standardOutput.contains(expected)
        logFileIsWritten == (mockFile != null && mockFile.exists() && mockFile.text.contains(expected))

        where:
        logToStdout | setLogFile | logFileIsWritten
        true        | true       | true
        true        | false      | false
        false       | true       | false
        false       | false      | false

        mockFileName = "foobar24"
        expected = "[ARGUMENTS]:"
    }
}
