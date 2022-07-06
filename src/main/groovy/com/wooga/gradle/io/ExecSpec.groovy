package com.wooga.gradle.io


import com.wooga.gradle.BaseSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.ProcessOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

import javax.inject.Inject
/**
 * Provides a task with an executable property to be used during task execution
 */
trait ExecSpec implements BaseSpec {

    @Inject
    ProcessOperations getProcessOperations() {
        throw new Exception("ProcessOperations is supposed to be injected here by gradle")
    }

    /**
     * The path to the executable
     */
    @Input
    Provider<String> getExecutable() {
        if (executableDirectory.present) {
            return executableDirectory.file(executableName).map {
                it.asFile.absolutePath
            }
        }
        return executableName
    }

    /**
     * @return The directory where the executable is located
     */
    @Internal
    DirectoryProperty getExecutableDirectory() {
        executableDirectory
    }
    private final DirectoryProperty executableDirectory = objects.directoryProperty()

    void setExecutableDirectory(Provider<Directory> value) {
        executableDirectory.set(value)
    }

    void setExecutableDirectory(File value) {
        executableDirectory.set(value)
    }

    /**
     * The name of the executable
     */
    @Internal
    Property<String> getExecutableName() {
        executableName
    }
    private final Property<String> executableName = objects.property(String)

    void setExecutableName(Provider<String> value) {
        executableName.set(value)
    }

    void setExecutableName(String value) {
        executableName.set(value)
    }

    /**
     * Sets the executable with the given file path.
     * If it's a path, it will resolve the directory property, otherwise it will leave it empty
     */
    void setExecutableByPath(String path) {
        def file = new File(path)
        setExecutableByFile(file)
    }

    /**
     * Sets the executable with the given file path.
     * If it's a path, it will resolve the directory property, otherwise it will leave it empty
     */
    void setExecutableByPath(Provider<String> provider) {
        setExecutableByFile(provider.map({ new File(it) }))
    }

    /**
     * Sets the executable by the provider
     */
    void setExecutableByFile(Provider<File> provider) {
        executableName.set(provider.map({ it.name }))
        executableDirectory.set(layout.dir(provider.map { it.parentFile }))
    }

    /**
     * Sets the executable by the provider
     */
    void setExecutableByFile(File file) {
        executableName.set(file.name)
        executableDirectory.set(file.parentFile)
    }

    /**
     * Sets the executable by the provider
     */
    void setExecutableByRegularFile(Provider<RegularFile> provider) {
        setExecutableByPath(provider.map({ it.asFile.path }))
    }

    /**
     * Sets the executable by the provider
     */
    void setExecutableByRegularFile(RegularFile file) {
        setExecutableByFile(file.asFile)
    }
    /**
     * To override the working directory when invoking the executable
     * @return
     */
    @Input
    @Optional
    Property<String> getWorkingDirectory() {
        workingDirectory
    }

    private final Property<String> workingDirectory = objects.property(String)

    void setWorkingDirectory(Provider<String> value) {
        workingDirectory.set(value)
    }

    /**
     * @return True if the executable has been set
     */
    @Internal
    Boolean getHasExecutable() {
        executable.get()
    }
}
