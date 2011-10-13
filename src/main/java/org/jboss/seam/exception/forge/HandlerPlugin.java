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

import java.io.FileNotFoundException;

import javax.inject.Inject;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresResource;
import org.jboss.solder.exception.control.Handles;
import org.jboss.solder.exception.control.HandlesExceptions;
import org.jboss.solder.exception.control.Precedence;
import org.jboss.solder.exception.control.TraversalMode;

/**
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@RequiresResource(JavaResource.class)
@RequiresFacet(CatchFacet.class)
@Alias("handler")
@Help("A plugin to setup and manage Seam Catch Exception Handlers.")
public class HandlerPlugin implements Plugin
{

   private final Project project;
   private final Shell shell;
   private final ShellPrintWriter writer;
   private final ShellPrompt prompt;

   /**
    * Injection constructor
    * 
    * @param project current project
    * @param shell current shell
    * @param writer current shell writer
    * @param prompt current shell prompt
    */
   @Inject
   public HandlerPlugin(final Project project, final Shell shell, final ShellPrintWriter writer,
            final ShellPrompt prompt)
   {
      this.project = project;
      this.shell = shell;
      this.writer = writer;
      this.prompt = prompt;
   }

   /**
    * Command to create a new handler.
    * 
    * @param methodName
    * @param exceptionType
    */
   @Command(value = "create", help = "Create a Seam Catch Exception Handler method.")
   public void handlerCommand(
            @Option(name = "method-name", required = true, type = PromptType.JAVA_VARIABLE_NAME,
                     help = "Name of the handler method to create") final String methodName,
            @Option(name = "exception-type", required = true, type = PromptType.JAVA_CLASS,
                     help = "Type of the exception the handler will handle") final String exceptionType,
            @Option(name = "breadthFirst", required = false, defaultValue = "false",
                     help = "Should the handler be a BREADTH_FIRST handler") final boolean breadthFirst,
            @Option(name = "precedence", required = false, defaultValue = "0",
                     help = "Precedence level relative to other handlers for the same exception type") final int precedence)
   {
      try {
         final JavaClass handlerContainerClass = this.getJavaClass();
         final JavaSourceFacet javaSourceFacet = this.project.getFacet(JavaSourceFacet.class);

         if (!handlerContainerClass.hasAnnotation(HandlesExceptions.class)) {
            writer.renderColor(ShellColor.RED,
                     "This class is not an Exception Handler Container (it must be annotated with @HandlesExceptions)");
            return;
         }

         String cleanedExceptionType = exceptionType;

         if (exceptionType.startsWith("java.lang"))
            cleanedExceptionType = exceptionType.replace("java.lang", "");

         Method<JavaClass> handlerMethod = handlerContainerClass.addMethod();

         StringBuilder parameterBuilder = new StringBuilder("@Handles");

         if (breadthFirst) {
            parameterBuilder.append("(during = TraversalMode.BREADTH_FIRST");

            if (!handlerContainerClass.hasImport(TraversalMode.class))
               handlerContainerClass.addImport(TraversalMode.class);

            if (precedence == Precedence.DEFAULT)
               parameterBuilder.append(")");
         }

         if (precedence != Precedence.DEFAULT) {
            // Import for Precedence
            if (!handlerContainerClass.hasImport(Precedence.class))
               handlerContainerClass.addImport(Precedence.class);

            // setup the parameter string correctly if this is the first param to the annotation
            if (!breadthFirst)
               parameterBuilder.append("(");
            else
               parameterBuilder.append(", ");

            parameterBuilder.append("precedence = ");

            switch (precedence)
            {
            case Precedence.BUILT_IN:
               parameterBuilder.append("Precedence.BUILT_IN");
               break;
            case Precedence.FRAMEWORK:
               parameterBuilder.append("Precedence.FRAMEWORK");
               break;
            case Precedence.HIGH:
               parameterBuilder.append("Precedence.HIGH");
               break;
            case Precedence.LOW:
               parameterBuilder.append("Precedence.LOW");
               break;
            }

            parameterBuilder.append(")");
         }

         parameterBuilder.append(" final CaughtException<")
                  .append(cleanedExceptionType).append("> caughtException");

         handlerMethod.setPublic().setReturnTypeVoid().setName(methodName).setParameters(parameterBuilder.toString());

         // Setup the imports for annotations and exception type
         if (!handlerContainerClass.hasImport(Handles.class))
            handlerContainerClass.addImport(Handles.class);

         if (!handlerContainerClass.hasImport(exceptionType) && !exceptionType.startsWith("java.lang"))
            handlerContainerClass.addImport(exceptionType);

         javaSourceFacet.saveJavaSource(handlerContainerClass);

         this.writer.println("Added Handler [" + methodName + "] to container [" + handlerContainerClass + "]");
      }
      catch (FileNotFoundException e) {
         this.writer.println("Error finding the class source file");
      }
   }

   private JavaClass getJavaClass() throws FileNotFoundException
   {
      Resource<?> resource = shell.getCurrentResource();
      if (resource instanceof JavaResource) {
         return getJavaClassFrom(resource);
      }
      else {
         throw new RuntimeException("Current resource is not a JavaResource!");
      }
   }

   private JavaClass getJavaClassFrom(final Resource<?> resource) throws FileNotFoundException
   {
      JavaSource<?> source = ((JavaResource) resource).getJavaSource();
      if (!source.isClass()) {
         throw new IllegalStateException("Current resource is not a JavaClass!");
      }
      return (JavaClass) source;
   }
}
