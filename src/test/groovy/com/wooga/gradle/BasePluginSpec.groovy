package com.wooga.gradle

import nebula.test.PluginProjectSpec
import nebula.test.ProjectSpec
import org.apache.tools.ant.Project

class MockBasePlugin extends BasePlugin<MockConventions> {

}

class BasePluginSpec extends ProjectSpec {

    String getPluginName() {
        "mock_plugin"
    }

    def 'creates the plugin'() {
        given:
        assert !project.plugins.hasPlugin(pluginName)
        assert !project.extensions.findByName(MockPlugin.extensionName)

        when:
        def plugin = project.plugins.apply(MockBasePlugin)

        then:
        plugin != null
    }

    def "plugins gets all property lookups from mock conventions class"() {
        when:
        def plugin = project.plugins.apply(MockBasePlugin)

        then:
        def lookups = plugin.propertyLookups
        lookups.size() == 3
        lookups.containsAll(MockConventions.name, MockConventions.version, MockConventions.directory)
    }

    def "gets all environment variables from mock conventions class"() {
        when:
        def plugin = project.plugins.apply(MockBasePlugin)

        then:
        def envVars = plugin.declaredEnvironmentVariables
        envVars.size() == 3
        envVars.containsAll("MOCK_NAME", "MOCK_VERSION", "MOCK_DIR")
    }
}
