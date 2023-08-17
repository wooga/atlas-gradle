package com.wooga.gradle

import com.wooga.gradle.io.ExecSpec
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.ParameterizedType

class MockPlugin<T extends MockExtension> implements Plugin<Project> {

    Class<T> getExtensionClass() {
        if (!_extensionClass) {
            try {
                _extensionClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
            }
            catch (Exception e) {
                _extensionClass = (Class<T>) MockExtension
            }
        }
        _extensionClass
    }
    private Class<T> _extensionClass

    static final String extensionName = "commons"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(extensionClass, extensionName, extensionClass)
        apply(project, extension)
        project.tasks.withType(MockTask).configureEach { t ->
        }
    }

    void apply(Project project, T extension) {
    }
}

class MockExtension implements ExecSpec {
}

class MockConventions {

    static final PropertyLookup name = new PropertyLookup(
        "MOCK_NAME",
        "mock.name",
        "foobar"
    )

    static final PropertyLookup version = new PropertyLookup(
        "MOCK_VERSION",
        "mock.version",
        "latest"
    )

    static final PropertyLookup directory = new PropertyLookup(
        "MOCK_DIR",
        "mock.dir",
        null
    )
}
