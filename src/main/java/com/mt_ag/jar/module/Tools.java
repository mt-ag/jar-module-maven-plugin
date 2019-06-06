package com.mt_ag.jar.module;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

/**
 * The Tools interface for static methods. Do not use as interface.
 */
public interface Tools {
  /**
   * Calls a command with parameters in the work dir.
   *
   * @param log   the logger.
   * @param dir   the work dir.
   * @param param the command with its parameters.
   * @return the call result.
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  static CallResult callInDir(Log log, Path dir, String... param) throws MojoExecutionException {
    try {
      List<String> args = new ArrayList<>(Arrays.asList(param));
      log.info("command: " + String.join(" ", args));
      Path tempFile = Files.createTempFile("Temp", "out.txt");
      Process proc = new ProcessBuilder().directory(dir.toFile()).command(args).redirectErrorStream(true)
          .redirectOutput(tempFile.toFile()).start();
      int exitVal = proc.waitFor();

      log.info("exitVal: " + exitVal);
      List<String> retVal = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
      Files.delete(tempFile);
      log.info("out:");
      for (String line : retVal) {
        log.info(line);
      }
      return new CallResult(exitVal, retVal);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Error in calling: " + param[0] + " interrupted", e);
    } catch (IOException e) {
      throw new MojoExecutionException("Error in calling: " + param[0], e);
    }
  }

  /**
   * Sets the module main class if it is not an automatic module and the main class is not set but the main class is
   * set in the manifest.
   *
   * @param log       the logger.
   * @param targetJar the module to test and update.
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  static void setModuleMain(Log log, Path targetJar) throws MojoExecutionException {
    ModuleDescriptor md = ModuleFinder.of(targetJar).findAll().stream().findFirst().get().descriptor();
    if (!md.mainClass().isPresent() && !md.isAutomatic()) {
      try (JarFile targetJarJar = new JarFile(targetJar.toFile())) {
        String mainClass = targetJarJar.getManifest().getMainAttributes().getValue("Main-Class");
        log.info("MainClass: " + mainClass);

        if (mainClass != null) {
          callInDir(log, targetJar.getParent(), "jar", "-u", "-f", targetJar.getFileName().toString(), "-e", mainClass);
        }
      } catch (IOException e) {
        throw new MojoExecutionException("Error in reading jar!" + targetJar, e);
      }
    }
  }
}
