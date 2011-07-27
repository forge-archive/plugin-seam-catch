/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.exception.forge;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.project.Facet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.CDIFacet;

/**
 * Seam Catch Facet, used to list dependencies and allow for easier installation.
 *
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@Alias("org.jboss.seam.exception")
@RequiresFacet(CDIFacet.class)
public class CatchFacet extends BaseFacet {
    private static final Dependency SEAM_CATCH_DEPENDENCY = DependencyBuilder.create("org.jboss.seam.catch:seam-catch");

    @Inject
    private ShellPrompt prompt;

    @Inject
    private ShellPrintWriter writer;

    @Inject
    private DependencyResolver resolver;

    @Inject
    Event<InstallFacets> installFacetsEvent;

    @Override
    public boolean install() {
        final DependencyFacet deps = this.getProject().getFacet(DependencyFacet.class);

        writer.println();

        if (!deps.hasRepository(DependencyFacet.KnownRepository.JBOSS_NEXUS)) {
            deps.addRepository(DependencyFacet.KnownRepository.JBOSS_NEXUS);
        }

        final List<Dependency> versions = resolver.resolveVersions(SEAM_CATCH_DEPENDENCY);

        final Dependency dependency = prompt.promptChoiceTyped("Install Seam Catch version", versions,
                versions.isEmpty() ? null : versions.get(versions.size() - 1));

        final Dependency existingDependency = deps.getDependency(dependency);

        if ((existingDependency != null)
                && prompt.promptBoolean("Existing Seam Catch dependency found. Replace [" + existingDependency + "] with [" + dependency + "]?"))
            deps.removeDependency(existingDependency);

        deps.addDependency(dependency);

        this.getProject().registerFacet(this);

        return true;
    }

    @Override
    public boolean isInstalled() {
        final DependencyFacet allDependencies = this.getProject().getFacet(DependencyFacet.class);

        return this.getProject().hasAllFacets(Arrays.<Class<? extends Facet>>asList(CDIFacet.class, CatchFacet.class))
                && allDependencies.hasDependency(SEAM_CATCH_DEPENDENCY);

    }
}
