package com.wooga.gradle.io

import com.wooga.gradle.ArgumentsSpec
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.ProcessOperations
import org.gradle.process.ExecResult

import java.util.function.Function

/**
 * Executes a process after having configured it through exposed methods
 */
class ProcessExecutor {

    //------------------------------------------------------------------------/
    // Fields
    //------------------------------------------------------------------------/
    // Required
    Function<Action<? super org.gradle.process.ExecSpec>, ExecResult> function
    String _executable
    // Optional
    List<String> arguments
    Map<String, ?> environment
    Boolean appendToEnvironment
    Closure closure
    Boolean _ignoreExitValue = false
    String workingDirectory
    OutputStream _standardOutput, _standardError

    //------------------------------------------------------------------------/
    // Constructors
    //------------------------------------------------------------------------/
    private ProcessExecutor(ProcessOperations processOperations) {
        function = { a -> processOperations.exec(a as Action<? super org.gradle.process.ExecSpec>) }
    }

    private ProcessExecutor(Project project) {
        function = { a -> project.exec(a as Action<? super org.gradle.process.ExecSpec>) }
    }

    static ProcessExecutor from(ProcessOperations processOperations) {
        new ProcessExecutor(processOperations)
    }

    static ProcessExecutor from(Project project) {
        new ProcessExecutor(project)
    }

    static ProcessExecutor from(ExecSpec spec) {
        new ProcessExecutor(spec.processOperations)
            .withExecutable(spec.executable.getOrNull())
            .withWorkingDirectory(spec.workingDirectory.getOrNull())
    }

    static ProcessExecutor fromExecutable(Executable executable) {
        from(executable).withArguments(executable, true)
    }

    //------------------------------------------------------------------------/
    // Configuration
    //------------------------------------------------------------------------/
    ProcessExecutor withExecutable(String filePath) {
        _executable = filePath
        this
    }

    ProcessExecutor withExecutable(File file) {
        withExecutable(file.path)
    }

    ProcessExecutor withArguments(ArgumentsSpec spec, Boolean includeEnvironment = true) {
        withArguments(spec.arguments.getOrElse([]))
        if (includeEnvironment) {
            withEnvironment(spec.environment.getOrElse([:]))
        }
        this
    }

    ProcessExecutor withArguments(Iterable<String> args) {
        this.arguments = args.toList()
        this
    }

    ProcessExecutor withArguments(String... args) {
        this.arguments = args.toList()
        this
    }

    /**
     * Sets the working directory for the execution
     */
    ProcessExecutor withWorkingDirectory(String value) {
        this.workingDirectory = value
        this
    }

    ProcessExecutor withEnvironment(Map<String, ?> environment, Boolean append = true) {
        this.environment = environment
        this.appendToEnvironment = append
        this
    }

    /**
     * The exit value of the process will be ignored, otherwise an exception
     * would be thrown on an error.
     */
    ProcessExecutor ignoreExitValue() {
        _ignoreExitValue = true
        this
    }

    /**
     * Assigns a closure to be executed when the process is about to be executed
     * @param configure A closure that configures an ExecSpec
     */
    ProcessExecutor with(@ClosureParams(value = FromString.class, options = ["org.gradle.process.ExecSpec"]) Closure configure) {
        closure = configure
        this
    }

    /**
     * Sets the standard output and standard error streams
     * @param spec A trait for generating an output stream
     * @param standardOutput The file to write standard output to
     * @param standardError The file to write standard error output to
     */
    ProcessExecutor withOutput(ProcessOutputSpec spec, File standardOutput, File standardError, Action<ProcessOutputConfiguration> configure = null) {
        if (standardOutput != null) {
            FileUtils.ensureFile(standardOutput)
            withStandardOutput(spec.getStandardOutputStream(standardOutput, configure))
        }
        if (standardError != null) {
            FileUtils.ensureFile(standardError)
            withStandardError(spec.getStandardErrorStream(standardError, configure))
        }
        this
    }

    /**
     * Sets the standard output and standard error streams to the same tile
     * @param spec A trait for generating an output stream
     * @param file The file to write standard output and standard error to
     */
    ProcessExecutor withOutput(ProcessOutputSpec spec, File file, Action<ProcessOutputConfiguration> configure = null) {
        withOutput(spec, file, file, configure)
    }

    /**
     * Sets the standard output and standard error streams
     * @param outputStreamSpec A trait for generating an output stream
     * @param stdout A trait for setting a log file
     * @param standardError Whether to write standard error to the log file
     */
    ProcessExecutor withOutputLogFile(ProcessOutputSpec outputStreamSpec,
                                      LogFileSpec stdout,
                                      LogFileSpec stderr,
                                      Action<ProcessOutputConfiguration> configure = null) {
        withOutput(outputStreamSpec,
            stdout.logFile.asFile.getOrNull(),
            stderr.logFile.asFile.getOrNull(),
            configure)
    }

    /**
     * Writes the standard output and error to the same file
     * @param outputStreamSpec A trait for generating an output stream
     * @param logFile A trait containing a log file
     */
    ProcessExecutor withOutputLogFile(ProcessOutputSpec outputStreamSpec,
                                      LogFileSpec logFile,
                                      Action<ProcessOutputConfiguration> configure = null) {
        withOutputLogFile(outputStreamSpec,
            logFile,
            logFile,
            configure)
    }

    /**
     * Sets the standard output stream
     */
    ProcessExecutor withStandardOutput(OutputStream stream) {
        _standardOutput = stream
        this
    }

    /**
     * Sets the standard error stream
     */
    ProcessExecutor withStandardError(OutputStream stream) {
        _standardError = stream
        this
    }

    /**
     * Executes the specified executable according to the configuration
     * @return The result of the execution
     */
    ExecResult execute() {
        function.apply({ exec ->
            exec.with {
                executable _executable

                if (workingDirectory) {
                    workingDir(workingDirectory)
                }

                if (arguments != null) {
                    it.args = arguments
                }

                if (this.environment != null) {
                    if (appendToEnvironment) {
                        it.environment(this.environment)
                    } else {
                        it.environment = this.environment
                    }
                }

                if (_standardOutput != null) {
                    standardOutput = _standardOutput
                }

                if (_standardError != null) {
                    errorOutput = _standardError
                }

                ignoreExitValue = _ignoreExitValue

                if (closure != null) {
                    closure(it)
                }
            }
        })
    }
}
