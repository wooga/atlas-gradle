package com.wooga.gradle

import nebula.test.ProjectSpec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

class PropertyConventionSpec extends ProjectSpec {

    class MockPropertyConventionExtension implements BaseSpec {

        final Property<String> name = objects.property(String)

        @StringPropertyConvention(propertyKeys = ["foo", "bar"])
        Property<String> getName() {
            name
        }
    }

    class MockPropertyConventionPlugin implements Plugin<Project> {
        @Override
        void apply(Project project) {
            def extension = project.extensions.create(MockPropertyConventionExtension, "propertyConventions", MockPropertyConventionExtension)
        }    }

    def 'creates the plugin'() {
        when:
        def plugin = project.plugins.apply(MockBasePlugin)

        then:
        plugin != null
    }
}
