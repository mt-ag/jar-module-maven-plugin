package com.mtag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;

/**
 * Implements the goal jlink. Jlink calls the jlink command. Before it is done, it copies all needed jar to the module
 * dir. Than it tests if the jars are automatic modules, if so it generates the module-info and adds it to the jar.
 */
@Mojo(name = "jlink", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.RUNTIME,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JlinkMojo extends UpdateModules {

  /**
   * The maven project. Used for dependencies and the own artifact.
   */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * Maven ProjectHelper.
   */
  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The implementation of the Mojo.
   *
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {

    Path dir = project.getBasedir().toPath().resolve("target");
    Log log = getLog();

    log.info("Project:" + project.getId());
    log.info("own artifact:" + project.getArtifact().getClass().getCanonicalName()
        + project.getArtifact().toString() + "=" + project.getArtifact().getFile().getAbsolutePath());

    for (Artifact a : project.getArtifacts()) {
      log.info("Artifact:" + a.getClass().getCanonicalName() + a.getGroupId() + ":" + a.getArtifactId()
          + ":" + a.getVersion() + ":" + a.getFile().getAbsolutePath());
    }
    log.info("Work dir: " + dir.toAbsolutePath().toString());
    Path modulesPath = createModules(project);

    Path targetJar = project.getArtifact().getFile().toPath();
    String moduleName = ModuleFinder.of(targetJar).findAll().stream().findFirst().get().descriptor().name();
    log.info("Found module:" + moduleName);
    callInDir(modulesPath, "jlink", "--compress=2", "--module-path", ".", "--add-modules", moduleName,
        "--output", "run");
    Path runZipPath = zipDir(modulesPath.resolve("run"), targetJar, "run");
    projectHelper.attachArtifact(project, "zip", "run", runZipPath.toFile());
  }
}
