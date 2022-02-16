package com.wooga.gradle

import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.gradle.internal.impldep.org.apache.http.annotation.Obsolete

import javax.inject.Inject

/**
 * A trait with access to the factories provided by a {@link org.gradle.api.Project}
 */
trait BaseSpec {
    @Inject
    ProjectLayout getLayout() {
        throw new Exception("ProjectLayout is supposed to be injected here by gradle")
    }

    @Internal
    ProviderFactory getProviderFactory() {
        getProviders()
    }

    @Inject
    ProviderFactory getProviders() {
        throw new Exception("ProviderFactory is supposed to be injected here by gradle")
    }

    @Inject
    ObjectFactory getObjects() {
        throw new Exception("ObjectFactory is supposed to be injected here by gradle")
    }
}
