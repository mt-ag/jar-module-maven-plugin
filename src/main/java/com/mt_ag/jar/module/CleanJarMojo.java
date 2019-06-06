package com.mt_ag.jar.module;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Plugin class for the goal jar. In this goal the jar is cleaned form unused folder entries and the size is set
 * correct. Saves 4 bytes per entry.
 */
@Mojo(name = "jar", defaultPhase = LifecyclePhase.PACKAGE)
public class CleanJarMojo extends AbstractMojo {

  /**
   * Parameter repack. If true the jar is repacked.
   */
  @Parameter
  private boolean repack;

  /**
   * The ref to the maven project.
   */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * Maven ProjectHelper.
   */
  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The used logger, set by excecute.
   */
  private Log myLog;

  /**
   * The default constructor.
   */
  public CleanJarMojo() {
    super();
  }

  /**
   * The constructor for testing.
   *
   * @param pLog    the MockLog.
   * @param pRepack the param repack.
   */
  protected CleanJarMojo(Log pLog, boolean pRepack) {
    myLog = pLog;
    repack = pRepack;
  }

  /**
   * The implementation. Sets the main class if it is a module and the main class is not set but part of the manifest.
   *
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {
    myLog = getLog();
    Path jarPath = project.getArtifact().getFile().toPath();
    projectHelper.attachArtifact(project, "jar", "clean", exec(jarPath).toFile());
  }

  /**
   * The main execute method to update the jar and clean it.
   *
   * @param jarPath the path to the artifact(jar).
   * @return the new artifact.
   * @throws MojoExecutionException is thrown if an IOException is thrown.
   */
  protected Path exec(Path jarPath) throws MojoExecutionException {
    String name = jarPath.getFileName().toString();
    String tempName;
    if (name.endsWith(".jar")) {
      String baseName = name.substring(0, name.length() - UpdateModules.EXTENSION_LENGTH);
      tempName = baseName + ".temp.jar";
      name = baseName + ".clean.jar";
    } else {
      throw new MojoExecutionException("Unexpected artifact extension: " + name);
    }
    Path tempJar = jarPath.resolveSibling(tempName);
    Path cleanPath = jarPath.resolveSibling(name);
    try {
      Files.copy(jarPath, tempJar);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to copy target.jar", e);
    }
    Tools.setModuleMain(myLog, tempJar);
    if (repack) {
      rePack(tempJar, name);
      try {
        Files.delete(tempJar);
      } catch (IOException e) {
        throw new MojoExecutionException("unable to delete file!", e);
      }
    } else {
      try {
        Files.move(tempJar, cleanPath);
      } catch (IOException e) {
        throw new MojoExecutionException("unable to rename file!", e);
      }
    }
    return cleanPath;
  }

  /**
   * Repack cleans the jar from dir entries and set the size correct.
   *
   * @param orgPath the original file as path.
   * @param name    the new filename.
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  private void rePack(Path orgPath, String name) throws MojoExecutionException {
    Path cleanedPath = orgPath.resolveSibling(name);
    if (!Files.exists(cleanedPath)) {
      try {
        Files.createFile(cleanedPath);
      } catch (IOException e) {
        myLog.error("unable to create File: " + name);
      }
    }
    try (JarFile orgJar = new JarFile(orgPath.toFile());
         JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(cleanedPath,
             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
      for (JarEntry jar : Collections.list(orgJar.entries())) {
        myLog.info("Entry:" + jar.getName());
        if (!jar.isDirectory()) {
          copyCleanEntry(orgJar, jarOut, jar);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading artifact: " + orgPath.getFileName().toString(), e);
    }
  }

  /**
   * Copies an entry and cleans the entry.
   *
   * @param orgJar the org JarFile.
   * @param jarOut the jar output stream.
   * @param jar    the used entry.
   * @throws IOException thrown, when an error in io occurs.
   */
  private static void copyCleanEntry(JarFile orgJar, JarOutputStream jarOut, JarEntry jar) throws IOException {
    JarEntry newEntry = new JarEntry(jar);
    try (InputStream jarIn = orgJar.getInputStream(jar)) {
      byte[] data = jarIn.readAllBytes();
      jarOut.putNextEntry(newEntry);
      jarOut.write(data);
      jarOut.closeEntry();
    }
  }
}
