package com.wooga.gradle.io

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.ProcessOperations
import org.gradle.api.internal.file.FileOperations
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
    FileOperations getFileOperations() {
        throw new UnsupportedOperationException();
    }

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
     * Resolves a relative file path. This method converts the supplied path contained in the provider based on its type:
     * <ul>
     * <li>A CharSequence, including String or GString. Interpreted relative to the project directory. A string that starts with file: is treated as a file URL.</li>
     * <li>A File. If the file is an absolute file, it is returned as is. Otherwise, the file's path is interpreted relative to the project directory.</li>
     * <li>A Path. The path must be associated with the default provider and is treated the same way as an instance of File.</li>
     * <li>A URI or URL. The URL's path is interpreted as the file path. Only file: URLs are supported.</li>
     * <li>A Directory or RegularFile.</li>
     * <li>A Provider of any supported type. The provider's value is resolved recursively.</li>
     * <li>A TextResource.</li>
     * <li>A Groovy Closure or Kotlin function that returns any supported type. The closure's return value is resolved recursively.</li>
     * <li>A Callable that returns any supported type. The callable's return value is resolved recursively.</li>
     * </ul>
     */
    void setExecutable(Provider<Object> provider) {
        executableName.set(provider.map({
            def file = new File(fileOperations.relativePath(it))
            file.name
        }))

        executableDirectory.set(layout.dir(provider.map({
            def file = new File(fileOperations.relativePath(it))
            file.parentFile
        })))
    }

    /**
     * Sets the executable with the given file path.
     * If it's a path, it will resolve the directory property, otherwise it will leave it empty
     * Resolves a relative file path. This method converts the supplied path based on its type:
     * <ul>
     * <li>A CharSequence, including String or GString. Interpreted relative to the project directory. A string that starts with file: is treated as a file URL.</li>
     * <li>A File. If the file is an absolute file, it is returned as is. Otherwise, the file's path is interpreted relative to the project directory.</li>
     * <li>A Path. The path must be associated with the default provider and is treated the same way as an instance of File.</li>
     * <li>A URI or URL. The URL's path is interpreted as the file path. Only file: URLs are supported.</li>
     * <li>A Directory or RegularFile.</li>
     * <li>A Provider of any supported type. The provider's value is resolved recursively.</li>
     * <li>A TextResource.</li>
     * <li>A Groovy Closure or Kotlin function that returns any supported type. The closure's return value is resolved recursively.</li>
     * <li>A Callable that returns any supported type. The callable's return value is resolved recursively.</li>
     * </ul>
     */
    void setExecutable(Object file) {
        setExecutable(providers.provider({file}))
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
