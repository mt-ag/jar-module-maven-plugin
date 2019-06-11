package com.mt_ag.jar.module;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the class UpdateModule.
 */
@DisplayName("Clear Jar Mojo Test")
public class ClearJarMojoTest {

  /**
   * Tests the method exec.
   */
  @Test
  void exec() throws IOException, MojoExecutionException {
    Path tempDir = Paths.get("temp");
    Files.createDirectory(tempDir);

    Path testPath = Paths.get("test-dir");
    Path orgPath = testPath.resolve("install-1.0.org.jar");
    Path copyPath = tempDir.resolve("install-1.0.jar");
    Files.copy(orgPath, copyPath);

    Log mockLog = Mockito.mock(Log.class);

    CleanJarMojo cleanJarMojo = new CleanJarMojo(mockLog, false);

    Path cleanPath = cleanJarMojo.exec(copyPath);

    assertTrue(Files.exists(copyPath), "Copy path dose not exist!");
    assertTrue(Files.exists(cleanPath), "Clean path dose not exist!");

    Mockito.verify(mockLog).info("MainClass: com.mtag.tools.config.gui.LinksDesktop");
    Mockito.verify(mockLog).info("command: jar -u -f install-1.0.temp.jar -e com.mtag.tools.config.gui.LinksDesktop");
    Mockito.verify(mockLog).info("out:");
    Mockito.verify(mockLog).info("exitVal: 0");

    Files.delete(copyPath);
    Files.delete(cleanPath);
    Files.delete(tempDir);
  }

  /**
   * Tests the method exec.
   */
  @Test
  void execRepack() throws IOException, MojoExecutionException {
    Path tempDir = Paths.get("temp");
    Files.createDirectory(tempDir);

    Path testPath = Paths.get("test-dir");
    Path orgPath = testPath.resolve("install-1.0.org.jar");
    Path copyPath = tempDir.resolve("install-1.0.jar");
    Files.copy(orgPath, copyPath);

    Log mockLog = Mockito.mock(Log.class);

    CleanJarMojo cleanJarMojo = new CleanJarMojo(mockLog, true);

    Path cleanPath = cleanJarMojo.exec(copyPath);

    assertTrue(Files.exists(copyPath), "Copy path dose not exist!");
    assertTrue(Files.exists(cleanPath), "Clean path dose not exist!");

    Mockito.verify(mockLog).info("MainClass: com.mtag.tools.config.gui.LinksDesktop");
    Mockito.verify(mockLog).info("command: jar -u -f install-1.0.temp.jar -e com.mtag.tools.config.gui.LinksDesktop");
    Mockito.verify(mockLog).info("out:");
    Mockito.verify(mockLog).info("exitVal: 0");

    Files.delete(copyPath);
    Files.delete(cleanPath);
    Files.delete(tempDir);
  }
}