package com.wooga.gradle.io

import com.wooga.gradle.BaseSpec
import com.wooga.gradle.MockTask
import com.wooga.gradle.MockTaskIntegrationSpec
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

class ExecutableUsingTask extends MockTask implements BaseSpec, LogFileSpec, ProcessOutputSpec {

    @Nested
    public final Executable foo = objects.newInstance(Executable)

    Executable getFoo() {
        foo
    }

    @Nested
    public final Executable bar = objects.newInstance(Executable)

    Executable getBar() {
        bar
    }

    ExecutableUsingTask() {
    }

    @TaskAction
    void run() {
        if (foo.hasExecutable) {
            ExecResult fooResult = ProcessExecutor.fromExecutable(foo).execute()
            fooResult.assertNormalExitValue()
        }
        if (bar.hasExecutable) {
            ExecResult barResult = ProcessExecutor.fromExecutable(bar).execute()
            barResult.assertNormalExitValue()
        }
    }
}

class ExecutableUsingTaskSpec extends MockTaskIntegrationSpec<ExecutableUsingTask> {

    def "sets multiple executables which are executed by task"() {

        given: "two mock executables"
        def foo = generateBatchWrapper("foo_exec", false)
        def bar = generateBatchWrapper("bar_exec", false)

        and: "them being set onto the task"
        appendToSubjectTask("foo.setExecutable(${wrapValueBasedOnType(foo.path, File)})")
        appendToSubjectTask("bar.setExecutable(${wrapValueBasedOnType(bar.path, File)})")

        and: "additional arguments being set for each"
        appendToSubjectTask("foo.setAdditionalArguments(${wrapValueBasedOnType(fooArgs, String)})")
        appendToSubjectTask("bar.setAdditionalArguments(${wrapValueBasedOnType(barArgs, String)})")

        when:
        fooGetter.write(this)
        barGetter.write(this)
        def result = runTasksSuccessfully(subjectUnderTestName, fooGetter.taskName, barGetter.taskName)
        def fooQuery = fooGetter.generateQuery(this, result)
        def barQuery = barGetter.generateQuery(this, result)

        then:
        fooQuery.contains(fooArgs)
        barQuery.contains(barArgs)

        where:
        fooArgs    | barArgs
        "pancakes" | "waffles"

        fooGetter = new PropertyGetterTaskWriter(subjectUnderTestName + ".foo.arguments")
        barGetter = new PropertyGetterTaskWriter(subjectUnderTestName + ".bar.arguments")
    }
}
