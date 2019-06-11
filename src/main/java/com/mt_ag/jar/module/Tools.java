package com.mt_ag.jar.module;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;

/**
 * The Tools interface for static methods. Do not use as interface.
 */
public interface Tools {

  /**
   * Class run.
   */
  class Run {

    /**
     * Private constructor of static class.
     */
    private Run() {

    }

    /**
     * The used executor.
     */
    private static  Executor executor = Executors.newSingleThreadExecutor();
    /**
     * The run call.
     * @param runnable the method to exec.
     */
    public static void run(Runnable runnable) {
      executor.execute(runnable);
    }
  }

  /**
   * The run call.
   * @param runnable the method to exec.
   */
  static void run(Runnable runnable) {
    Run.run(runnable);
  }

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
      Process proc = new ProcessBuilder().directory(dir.toFile()).command(args).redirectErrorStream(true).start();
      AsyncStreamReader asRead = new AsyncStreamReader(proc.getInputStream());
      run(asRead);
      int exitVal = proc.waitFor();

      log.info("exitVal: " + exitVal);
      List<String> retVal = asRead.getOutput();
      log.info("out:");
      retVal.forEach(log::info);
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
      String mainClass;
      try (JarFile targetJarJar = new JarFile(targetJar.toFile())) {
        mainClass = targetJarJar.getManifest().getMainAttributes().getValue("Main-Class");
        log.info("MainClass: " + mainClass);
      } catch (IOException e) {
        throw new MojoExecutionException("Error in reading jar!" + targetJar, e);
      }

      if (mainClass != null) {
        callInDir(log, targetJar.getParent(), "jar", "-u", "-f", targetJar.getFileName().toString(), "-e", mainClass);
      }
    }
  }
}
