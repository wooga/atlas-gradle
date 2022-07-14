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
 * Generates an {@link OutputStream}
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

    /**
     * @return An output stream that can fork to a given logFile, if it's been set
     */
    OutputStream getOutputStream(File logFile) {
        OutputStream outputStream
        // If logging to stdout is enabled
        if (logToStdout.isPresent() && logToStdout.get()) {
            TextStream handler = new ForkTextStream()
            String lineSeparator = SystemProperties.getInstance().getLineSeparator()
            outputStream = new LineBufferingOutputStream(handler, lineSeparator)

            if (logFile) {
                handler.addWriter(logFile.newPrintWriter())
            }
            handler.addWriter(System.out.newPrintWriter())
        }
        // Else if we are discarding the output
        else {
            outputStream = new ByteArrayOutputStream()
        }
        return outputStream
    }
}


