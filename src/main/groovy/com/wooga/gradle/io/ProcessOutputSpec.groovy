package com.wooga.gradle.io

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

trait ProcessOutputSpec extends OutputStreamSpec {
    private final Property<Boolean> logToStderr = objects.property(Boolean)

    /**
     * @return Whether the process should also log to stderr
     */
    @Internal
    Property<Boolean> getLogToStderr() {
        logToStderr
    }

    /**
     * Generates an stdout stream. If {@code logToStdout} is true, it will fork the output to it
     * @param logFile A file to fork the output to
     */
    OutputStream getStandardOutputStream(File logFile, Action<ProcessOutputConfiguration> configure = null) {
        Boolean log = this.logToStdout.present && this.logToStdout.get()
        getStream(logFile, log ? System.out : null, configure)
    }

    /**
     * Generates an stderr stream. If {@code logToStderr} is true, it will fork the output to it
     * @param logFile A file to fork the output to
     */
    OutputStream getStandardErrorStream(File logFile, Action<ProcessOutputConfiguration> configure = null) {
        Boolean logToStderr = this.logToStderr.present && this.logToStderr.get()
        getStream(logFile, logToStderr ? System.err : null, configure)
    }

    /**
     * Generates an output stream that will fork output to the given file and other stream
     * @param logFile A file to fork the output to
     * @param stream The stream to fork the output to
     */
    OutputStream getStream(File logFile, OutputStream stream, Action<ProcessOutputConfiguration> configure = null) {
        OutputStream result

        // If there is a valid stream to be created
        if (logFile || stream) {

            ProcessOutputConfiguration configuration = new ProcessOutputConfiguration()
            if (configure != null) {
                configure.execute(configuration)
            }

            // Construct the handler
            TextStream handler = new ForkTextStream()
            result = configuration.outputStreamFunction.apply(handler)

            // Optionally, write to log file
            if (logFile) {
                handler.addWriter(configuration.transformLogWriter.apply(logFile.newPrintWriter()))
            }

            if (stream) {
                handler.addWriter(configuration.transformStreamWriter.apply(stream.newPrintWriter()))
            }
        }
        // Else if we are discarding the output
        else {
            result = new ByteArrayOutputStream()
        }

        result
    }

    OutputStream getOutputStream(File logFile) {
        getStandardOutputStream(logFile)
    }
}
