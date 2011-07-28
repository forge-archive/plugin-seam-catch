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
import java.util.List;

import javax.enterprise.inject.CreationException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.parser.java.Parameter;
import org.jboss.forge.resources.java.JavaMethodResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.test.SingletonAbstractShellTest;
import org.jboss.seam.exception.control.Handles;
import org.jboss.seam.exception.control.Precedence;
import org.jboss.seam.exception.control.TraversalMode;
import org.jboss.seam.exception.forge.CatchPlugin;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@RunWith(Arquillian.class)
public class HandlerPluginTest extends SingletonAbstractShellTest {
    @Before
    @Override
    public void beforeTest() throws IOException {
        super.beforeTest();
        initializeJavaProject();
        this.queueInputLines(""); // Not sure why we do this...
        this.getShell().execute("seam-catch setup");

        this.getShell().execute("seam-catch create-handler-container --named TestContainer --package com.example.exceptionHandler");
    }

    @Deployment
    public static JavaArchive getDeployment() {
        return SingletonAbstractShellTest.getDeployment().addPackages(true, CatchPlugin.class.getPackage());
    }

    @Test
    public void assertCreatingAMinimalOptionHandlerWorksCorrectly() {
        this.getShell().execute("handler create --method-name throwableHandler --exception-type java.lang.Throwable");

        assertThat(this.getShell().getCurrentResource().getChild("throwableHandler").exists(), is(true));
    }

    @Test
    public void assertCreatingABreadthFirstHandlerWorksCorrectly() throws FileNotFoundException {
        this.getShell().execute("handler create --method-name throwableHandler --exception-type Throwable --breadthFirst true");

        List<Parameter> params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("throwableHandler")).getUnderlyingResourceObject().getParameters();

        assertThat(params.get(0).toString(), containsString("@Handles(during=TraversalMode.BREADTH_FIRST)"));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(TraversalMode.class), is(true));
    }

    @Test
    public void assertCreatingAHandlerWithPrecedenceWorksCorrectly() throws FileNotFoundException {
        this.getShell().execute("handler create --method-name throwableHandlerLow --exception-type Throwable --precedence 50");

        List<Parameter> params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("throwableHandlerLow")).getUnderlyingResourceObject().getParameters();

        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(Precedence.class), is(true));
        assertThat(params.get(0).toString(), containsString("@Handles(precedence=Precedence.LOW)"));

        this.getShell().execute("handler create --method-name throwableHandlerHigh --exception-type Throwable --precedence 100");
        params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("throwableHandlerHigh")).getUnderlyingResourceObject().getParameters();
        assertThat(params.get(0).toString(), containsString("@Handles(precedence=Precedence.HIGH)"));

        this.getShell().execute("handler create --method-name throwableHandlerFramework --exception-type Throwable --precedence -50");
        params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("throwableHandlerFramework")).getUnderlyingResourceObject().getParameters();
        assertThat(params.get(0).toString(), containsString("@Handles(precedence=Precedence.FRAMEWORK)"));

        this.getShell().execute("handler create --method-name throwableHandlerBuiltIn --exception-type Throwable --precedence -100");
        params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("throwableHandlerBuiltIn")).getUnderlyingResourceObject().getParameters();
        assertThat(params.get(0).toString(), containsString("@Handles(precedence=Precedence.BUILT_IN)"));

        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasSyntaxErrors(), is(false));
    }

    @Test
    public void assertUsingAllOptionsWorksCorrectly() throws FileNotFoundException {
        this.getShell().execute("handler create --method-name creationExceptionHandlerLowBreadthFirst --exception-type javax.enterprise.inject.CreationException --precedence 50 --breadthFirst true");

        List<Parameter> params = ((JavaMethodResource) this.getShell().getCurrentResource().getChild("creationExceptionHandlerLowBreadthFirst")).getUnderlyingResourceObject().getParameters();

        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(Precedence.class), is(true));
        assertThat(params.get(0).toString(), containsString("@Handles(during=TraversalMode.BREADTH_FIRST,precedence=Precedence.LOW)"));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(TraversalMode.class), is(true));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(Handles.class), is(true));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(CreationException.class), is(true));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasSyntaxErrors(), is(false));
    }

    @Test
    public void assertCreatingAMinimalOptionHandlerContainsCorrectImports() throws FileNotFoundException {
        this.getShell().execute("handler create --method-name creationExceptionHandler --exception-type javax.enterprise.inject.CreationException");

        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(Handles.class), is(true));
        assertThat(((JavaResource) this.getShell().getCurrentResource()).getJavaSource().hasImport(CreationException.class), is(true));
    }
}
