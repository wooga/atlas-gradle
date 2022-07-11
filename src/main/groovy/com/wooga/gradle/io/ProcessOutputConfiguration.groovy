package com.wooga.gradle.io

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.gradle.internal.SystemProperties

import java.util.function.Function

/**
 * Provides configuration during the generation of an {@link OutputStream} by the {@link ProcessOutputSpec}
 */
class ProcessOutputConfiguration {

    Function<TextStream, OutputStream> outputStreamFunction = { TextStream s -> new LineBufferingOutputStream(s, SystemProperties.instance.lineSeparator)}
    Function<Writer, Writer> transformStreamWriter = { it }
    Function<Writer, Writer> transformLogWriter = { it }

    /**
     * Sets a function to generate the output stream from the given handler
     */
    ProcessOutputConfiguration withOutput(@ClosureParams(value = FromString.class, options = "com.wooga.gradle.io.TextStream") Closure<OutputStream> func) {
        outputStreamFunction = func
        this
    }

    /**
     * Sets the writer used for standard out/err
     */
    ProcessOutputConfiguration withStreamWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformStreamWriter = func
        this
    }

    /**
     * Sets the writer used for log files
     */
    ProcessOutputConfiguration withLogWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformLogWriter = func
        this
    }

    /**
     * Sets all writers
     */
    ProcessOutputConfiguration withWriter(@ClosureParams(value = FromString.class, options = "java.io.Writer") Closure<Writer> func) {
        transformStreamWriter = func
        transformLogWriter = func
        this
    }
}
