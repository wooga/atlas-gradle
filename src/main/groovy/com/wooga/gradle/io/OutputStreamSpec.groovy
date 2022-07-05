package com.wooga.gradle.io

import com.wooga.gradle.BaseSpec
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.internal.SystemProperties

import java.util.function.Function

/**
 * Generates an output stream
 */
trait OutputStreamSpec extends BaseSpec {

    private final Property<Boolean> logToStdout = objects.property(Boolean)

    /**
     * @return Whether the process should also log to stdout
     */
    @Internal
    Property<Boolean> getLogToStdout() {
        logToStdout
    }

    private final Property<Boolean> logToStderr = objects.property(Boolean)

    /**
     * @return Whether the process should also log to stderr
     */
    @Internal
    Property<Boolean> getLogToStderr() {
        logToStderr
    }

    /**
     * @return An output stream that can fork to a given logFile, if it's been set
     */
    OutputStream getOutputStream(File logFile) {
        getStandardOutputStream(logFile)
    }

    /**
     * Generates an stdout stream. If {@code logToStdout} is true, it will fork the output to it
     * @param logFile A file to fork the output to
     */
    OutputStream getStandardOutputStream(File logFile, Action<OutputStreamConfiguration> configure = null) {
        Boolean log = this.logToStdout.present && this.logToStdout.get()
        getStream(logFile, log ? System.out : null, configure)
    }

    /**
     * Generates an stderr stream. If {@code logToStderr} is true, it will fork the output to it
     * @param logFile A file to fork the output to
     */
    OutputStream getStandardErrorStream(File logFile, Action<OutputStreamConfiguration> configure = null) {
        Boolean logToStderr = this.logToStderr.present && this.logToStderr.get()
        getStream(logFile, logToStderr ? System.err : null, configure)
    }

    /**
     * Generates an output stream that will fork output to the given file and other stream
     * @param logFile A file to fork the output to
     * @param stream The stream to fork the output to
     */
    OutputStream getStream(File logFile, OutputStream stream, Action<OutputStreamConfiguration> configure = null) {
        OutputStream result
        if (logFile || stream) {
            OutputStreamConfiguration configuration = new OutputStreamConfiguration()
            if (configure != null) {
                configure.execute(configuration)
            }
            // Construct the handler
            TextStream handler = new ForkTextStream()

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
        return result
    }
}

class OutputStreamConfiguration {
    Function<Writer, Writer> transformStreamWriter = { it }
    Function<Writer, Writer> transformLogWriter = { it }

    Function<TextStream, OutputStream> outputStreamFunction = { TextStream s -> new LineBufferingOutputStream(s, SystemProperties.instance.lineSeparator)}

    OutputStreamConfiguration withOutput(@ClosureParams(value = FromString.class, options = "com.wooga.gradle.io.TextStream") Closure<OutputStream> func) {
        outputStreamFunction = func
        this
    }

    OutputStreamConfiguration withStreamWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformStreamWriter = func
        this
    }

    OutputStreamConfiguration withLogWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformLogWriter = func
        this
    }

    OutputStreamConfiguration withWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformStreamWriter = func
        transformLogWriter = func
        this
    }
}
