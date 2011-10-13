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

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.util.ResourceUtil;
import org.jboss.solder.exception.control.HandlesExceptions;

public @Alias("seam-catch")
@RequiresFacet(CatchFacet.class)
@Help("A plugin to setup and manage Seam Catch Exception Handlers.")
class CatchPlugin implements org.jboss.forge.shell.plugins.Plugin
{

   private final Project project;
   private final Event<InstallFacets> installFacetsEvent;
   private final ShellPrintWriter writer;
   private final ShellPrompt prompt;
   private final Shell shell;

   /**
    * Injection Constructor
    * 
    * @param project
    * @param event
    * @param writer
    * @param prompt
    */
   @Inject
   public CatchPlugin(final Project project, final Event<InstallFacets> event, final ShellPrintWriter writer,
            final ShellPrompt prompt, final Shell shell)
   {
      this.project = project;
      this.installFacetsEvent = event;
      this.writer = writer;
      this.prompt = prompt;
      this.shell = shell;
   }

   /**
    * Setup Command.
    * 
    * @param out output pipe
    */
   @SetupCommand(help = "Install Seam Catch into the current project.")
   public void run(final PipeOut out)
   {
      if (!this.project.hasFacet(CatchFacet.class))
         installFacetsEvent.fire(new InstallFacets(CatchFacet.class));

      if (project.hasFacet(CatchFacet.class))
         ShellMessages.success(out, "Seam Catch Installed");
   }

   /**
    * Creates a class to holder Exception Handlers.
    * 
    * @param className Name of the class to create.
    * @param packageName Name of the package to create the class.
    * @throws Exception
    */
   @Command(value = "create-handler-container", help = "Create a Seam Catch Exception Handler container class.")
   public void newExceptionHandlerContainer(
            @Option(required = true, name = "named",
                     description = "The name of the containing class to create") final String className,
            @Option(required = false, name = "package", type = PromptType.JAVA_PACKAGE,
                     description = "Containing package name") final String packageName) throws Exception
   {
      final JavaSourceFacet javaSourceFacet = this.project.getFacet(JavaSourceFacet.class);

      String containerPackage;

      if ((packageName != null) && !"".equals(packageName))
         containerPackage = packageName;
      else if (this.getPackagePortionOfCurrentDirectory() != null)
         containerPackage = this.getPackagePortionOfCurrentDirectory();
      else
         containerPackage = prompt.promptCommon(
                  "In which package would you like to create this Exception Handler container:",
                  PromptType.JAVA_PACKAGE);

      final JavaClass handlerContainerClass = JavaParser.create(JavaClass.class)
               .setPackage(containerPackage)
               .setName(className)
               .setPublic()
               .addAnnotation(HandlesExceptions.class).getOrigin();

      final JavaResource handlerContainerFileLocation = javaSourceFacet.saveJavaSource(handlerContainerClass);

      writer.println("Created Exception Handler Container [" + handlerContainerClass.getQualifiedName() + "]");

      // pick up the generated class so they can then add handlers
      shell.execute("pick-up " + handlerContainerFileLocation.getFullyQualifiedName());
   }

   /**
    * Retrieves the package portion of the current directory if it is a package, null otherwise.
    * 
    * @return String representation of the current package, or null
    */
   private String getPackagePortionOfCurrentDirectory()
   {
      for (DirectoryResource r : this.project.getFacet(JavaSourceFacet.class).getSourceFolders()) {
         final DirectoryResource currentDirectory = shell.getCurrentDirectory();
         if (ResourceUtil.isChildOf(r, currentDirectory)) {
            // Have to remember to include the last slash so it's not part of the package
            return currentDirectory.getFullyQualifiedName().replace(r.getFullyQualifiedName() + "/", "")
                     .replaceAll("/", ".");
         }
      }
      return null;
   }
}
