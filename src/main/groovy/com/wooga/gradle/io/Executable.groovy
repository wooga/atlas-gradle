package com.wooga.gradle.io

import com.wooga.gradle.ArgumentsSpec
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult

/**
 * In order to properly instantiate this, do so through an Object's factory instantiate method 'objects.newInstance(Executable)'
 */
abstract class Executable implements ExecSpec, ArgumentsSpec {
}

