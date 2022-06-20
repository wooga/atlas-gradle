package com.wooga.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.impldep.com.sun.xml.bind.v2.schemagen.xmlschema.Annotated

import java.lang.reflect.ParameterizedType

abstract class BasePlugin<TConvention> implements Plugin<Project> {

    Class<TConvention> getConventionClass() {
        if (!_conventionClass) {
            try {
                this._conventionClass = (Class<TConvention>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
            }
            catch (Exception e) {
            }
        }
        _conventionClass
    }
    private Class<TConvention> _conventionClass

    @Override
    void apply(Project project) {
        for (lookup in PropertyLookup.getAll(conventionClass)) {
            if (lookup.hasDefaultProvider) {

            }
        }
    }

    List<PropertyLookup> getPropertyLookups() {
        PropertyLookup.getAll(conventionClass)
    }

    List<String> getDeclaredEnvironmentVariables() {
        PropertyLookup.getEnvironmentVariables(conventionClass)
    }
}

