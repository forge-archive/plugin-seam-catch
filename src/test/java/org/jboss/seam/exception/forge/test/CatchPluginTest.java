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
package org.jboss.seam.exception.forge.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.project.Project;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.test.SingletonAbstractShellTest;
import org.jboss.seam.exception.control.HandlesExceptions;
import org.jboss.seam.exception.forge.CatchFacet;
import org.jboss.seam.exception.forge.CatchPlugin;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@RunWith(Arquillian.class)
public class CatchPluginTest extends SingletonAbstractShellTest {
    @Before
    @Override
    public void beforeTest() throws IOException {
        super.beforeTest();
        initializeJavaProject();
    }

    @Deployment
    public static JavaArchive getDeployment() {
        return SingletonAbstractShellTest.getDeployment().addPackages(true, CatchPlugin.class.getPackage());
    }

    @Test
    public void assertSetupAppliesCorrectly() {
        final Project project = this.getProject();

        this.queueInputLines(""); // Not sure why we do this...
        this.getShell().execute("seam-catch setup");

        assertThat(project.getFacet(CatchFacet.class).isInstalled(), is(true));
    }

    @Test
    public void assertHandlerContainerCreatesSuccessfully() throws FileNotFoundException {
        // Boiler plate to setup the project
        final Project project = this.getProject();

        this.queueInputLines(""); // Not sure why we do this...
        this.getShell().execute("seam-catch setup");

        this.getShell().execute("seam-catch create-handler-container --named TestContainer --package com.example.exceptionHandler");

        assertThat(this.getShell().getCurrentResource().exists(), is(true));
        assertThat(this.getShell().getCurrentResource().getName(), is("TestContainer.java"));
        assertThat(((JavaResource)this.getShell().getCurrentResource()).getJavaSource().getImport(HandlesExceptions.class), notNullValue());

        // Test package structure is what we sent it
        assertThat(this.getShell().getCurrentResource().getParent().getName(), is("exceptionHandler"));
        assertThat(this.getShell().getCurrentResource().getParent().getParent().getName(), is("example"));
        assertThat(this.getShell().getCurrentResource().getParent().getParent().getParent().getName(), is("com"));
    }

    // TODO: Figure out how to get this to work
    /*@Test
    public void assertHandlerContainerCreatesSuccessfullyWithoutPackageParameter() throws FileNotFoundException {
        // Boiler plate to setup the project
        final Project project = this.getProject();

        this.queueInputLines(""); // Not sure why we do this...
        this.getShell().execute("seam-catch setup");

        this.getShell().execute("seam-catch create-handler-container --named TestContainer");
        this.getShell().println("com.example.exceptionHandler");

        assertThat(this.getShell().getCurrentResource().exists(), is(true));
        assertThat(this.getShell().getCurrentResource().getName(), is("TestContainer.java"));
        assertThat(((JavaResource)this.getShell().getCurrentResource()).getJavaSource().getImport(HandlesExceptions.class), notNullValue());

        // Test package structure is what we sent it
        assertThat(this.getShell().getCurrentResource().getParent().getName(), is("exceptionHandler"));
        assertThat(this.getShell().getCurrentResource().getParent().getParent().getName(), is("example"));
        assertThat(this.getShell().getCurrentResource().getParent().getParent().getParent().getName(), is("com"));
    }*/
}
