package com.wooga.gradle.io


import com.wooga.gradle.MockTask
import com.wooga.gradle.MockTaskIntegrationSpec
import com.wooga.gradle.test.BatchmodeWrapper
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import spock.lang.Unroll

class ProcessOutputTask extends MockTask implements LogFileSpec, ProcessOutputSpec {

    static String text = "journey to hook mountain"

    private final Property<Boolean> modifyOutput = objects.property(Boolean)

    @Optional
    @Input
    Property<Boolean> getModifyOutput() {
        modifyOutput
    }

    void setModifyOutput(Provider<Boolean> value) {
        modifyOutput.set(value)
    }

    @TaskAction
    void run() {

        def _logFile = logFile.present ? logFile.get().asFile : null
        def wrapper = new BatchmodeWrapper("superhund")
        wrapper.withText(text)
        wrapper.withEnvironment(false)

        ExecResult execResult = project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec exec) {
                exec.with {
                    executable wrapper.toTempFile()
                    args = ["foo", "bar"]
                    ignoreExitValue = true
                    standardOutput = getStandardOutputStream(_logFile, { ProcessOutputConfiguration cfg ->
                        if (modifyOutput.present && modifyOutput.get()) {
                            cfg.withWriter({ wr ->
                                new MockWriter(wr).newPrintWriter()
                            })
                        }
                    })
                }
            }
        })
    }
}

class ProcessOutputSpecTest extends MockTaskIntegrationSpec<ProcessOutputTask> {

    @Unroll
    def "runs task with logToStdout(#logToStdout), setLogFile(#setLogFile), logFileIsWritten(#logFileIsWritten)"() {

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
        false       | true       | true
        false       | false      | false

        mockFileName = "foobar24"
        expected = "[ARGUMENTS]:"
    }

    def "can modify the output stream"() {
        given:
        File logFile = file("foobar")
        appendToSubjectTask("""
        modifyOutput.set(true)
        logFile=${wrapValueBasedOnType(logFile.path, File)}
        logToStdout=true
        """)

        when:
        def result = runSubjectTaskSuccessfully()

        then:
        result.success
        result.standardOutput.contains(ProcessOutputTask.text.toUpperCase())
        logFile.exists()
        logFile.text.contains(ProcessOutputTask.text.toUpperCase())

    }
}

// Uppercases all text
class MockWriter extends Writer {

    final Writer writer

    MockWriter(Writer writer) {
        super()
        this.writer = writer
    }

    @Override
    void write(char[] text, int offset, int length) throws IOException {
        new String(text, offset, length).eachLine {
            transformLine(it)
        }
    }

    @Override
    void flush() throws IOException {
        writer.flush()
    }

    @Override
    void close() throws IOException {
        writer.close()
    }

    def transformLine(String text) {
        String formattedText = text.toUpperCase()
        if (!formattedText.isEmpty()) {
            writer.print(formattedText + System.lineSeparator())
            writer.flush()
        }
    }
}
