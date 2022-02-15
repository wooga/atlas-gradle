package com.wooga.gradle.io

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal

/**
 * For use when the functionality of writing output to log files is needed
 */
trait LogFileSpec extends BaseSpec {

    private final RegularFileProperty logFile = objects.fileProperty()

    /**
     * @return The log file to write standard out/**
     */
    @Internal
    RegularFileProperty getLogFile() {
        logFile
    }

    void setLogFile(Provider<RegularFile> value) {
        logFile.set(value)
    }

    void setLogFile(File value) {
        logFile.set(value)
    }
}
