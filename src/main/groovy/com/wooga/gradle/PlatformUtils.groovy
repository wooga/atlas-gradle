package com.wooga.gradle

import groovy.json.StringEscapeUtils
import org.gradle.internal.snapshot.PathUtil

import java.nio.file.Path

class PlatformUtils {

    private static final String _osName = System.getProperty("os.name").toLowerCase()
    private static final String _osArch = System.getProperty("os.arch").toLowerCase()

    /**
     * @return The name of the operating system, as fetched from the environment
     */
    static String osName() {
        _osName
    }

    /**
     * @return The operating system architecture (64/32 bit)
     */
    static String osArchitecture() {
        _osArch
    }

    /**
     * @return True if the current operating system architecture is 64 bit
     */
    static Boolean is64BitArchitecture() {
        _osArch.contains("64")
    }

    /**
     * @return True if the operating system is Windows
     */
    static boolean isWindows() {
        return (_osName.indexOf("win") >= 0)
    }

    /**
     * @return True if the operating system is OSX
     */
    static boolean isMac() {
        return (_osName.indexOf("mac") >= 0)
    }

    /**
     * @return True if the operating system is a Linux distribution
     */
    static boolean isLinux() {
        return (_osName.indexOf("linux") >= 0)
    }

    /**
     * @return True if the operating system is Unix (belonging to the family derived from the AT&T one)
     */
    static boolean isUnix() {
        return _osName.indexOf("unix") >= 0
    }

    /**
     * @param path A file path
     * @return A file path corrected for the current platform
     */
    static String escapedPath(String path) {
        if (isWindows()) {
            return StringEscapeUtils.escapeJava(path)
        }
        path
    }

    /**
     * Returns the pat to the usr directory in UNIX
     * @return
     */
    static String getUnixUserHomePath() {
        System.getProperty("user.home")
    }

    /**
     * Adjusts the given file path according to the file system being used.
     * This is to make sure paths are treated equally across different platforms
     * @param path A relative or absolute file path
     * @param rootDirectory Optionally, a root directory to start from if the path is found to be relative
     * @return An absolute file path
     */
    static String normalizePath(String path, String rootDirectory) {

        Boolean absolute = path.startsWith('/')
        if (absolute) {
            if (windows) {
                String volume = getDiskVolume()
                path = "${volume}${path[1..-1]}"
            }
        }
        else if (rootDirectory != null && rootDirectory != "") {
            path = "${rootDirectory}/${path}"
        }
        new File(path).path
    }

    static String normalizePath(String path) {
        normalizePath(path, null)
    }

    /**
     * @return Returns the name of the disk volume (such as 'C:/' for Windows)
     */
    static String getDiskVolume() {
        if (_diskVolume == null) {
            def current = new File(".").absoluteFile
            while (current.parentFile != null) {
                current = current.parentFile
            }
            _diskVolume = current.toString()
        }
        _diskVolume
    }
    private static String _diskVolume


}
