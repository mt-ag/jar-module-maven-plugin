package com.mt_ag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class to test the goal jlink.
 */
@DisplayName("Jlink Mojo Test")
public class JlinkMojoTest {

  /**
   * Test of exec.
   *
   * @throws MojoExecutionException not expected to be thrown.
   * @throws IOException            not expected to be thrown.
   */
  @Test
  public void execOK() throws MojoExecutionException, IOException {
    Path tempDir = Paths.get("temp");
    if (Files.exists(tempDir)) {
      TestTools.deleteDir(tempDir);
    }
    Files.createDirectory(tempDir);
    Path tempTargetDir = tempDir.resolve("target");
    Files.createDirectory(tempTargetDir);

    Path testDir = Paths.get("test-dir");
    Path srcTargetJar = testDir.resolve("asm-test-1.0.jar");
    Path targetJar = tempTargetDir.resolve("asm-test-1.0.jar");
    Files.copy(srcTargetJar, targetJar);

    MavenProject mavenProject = Mockito.mock(MavenProject.class);
    Artifact artifactMain = Mockito.mock(Artifact.class);
    Mockito.when(artifactMain.getFile()).thenReturn(targetJar.toFile());
    Mockito.when(mavenProject.getArtifact()).thenReturn(artifactMain);

    Mockito.when(mavenProject.getBasedir()).thenReturn(tempDir.toFile());
    Mockito.when(mavenProject.getId()).thenReturn("com.mt-ag.tools:asm-test:1.0");

    Log mockLog = Mockito.mock(Log.class);


    Artifact artifactAsm = Mockito.mock(Artifact.class);
    Mockito.when(artifactAsm.getFile()).thenReturn(testDir.resolve("asm-7.1.jar").toFile());
    Artifact artifactLog4jApi = Mockito.mock(Artifact.class);
    Mockito.when(artifactLog4jApi.getFile()).thenReturn(testDir.resolve("log4j-api-2.11.2.jar").toFile());
    Artifact artifactLog4jCore = Mockito.mock(Artifact.class);
    Mockito.when(artifactLog4jCore.getFile()).thenReturn(testDir.resolve("log4j-core-2.11.2.jar").toFile());
    Set<Artifact> artifactSet = Set.of(artifactAsm, artifactLog4jApi, artifactLog4jCore);
    Mockito.when(mavenProject.getArtifacts()).thenReturn(artifactSet);

    MavenProjectHelper helper = Mockito.mock(MavenProjectHelper.class);

    JlinkMojo jlinkMojo = new JlinkMojo(mockLog, mavenProject, helper, CompressEnum.ZIP, true, false, true);
    jlinkMojo.execute();

    assertTrue(Files.exists(tempTargetDir.resolve("asm-test-1.0.run.zip")), "Missing result of jlink!");
    TestTools.deleteDir(tempDir);
  }

  /**
   * Test of exec with all parameters set.
   *
   * @throws IOException            not expected to be thrown.
   */
  @Test
  public void execStripAllOK() throws IOException {
    Path tempDir = Paths.get("temp");
    if (Files.exists(tempDir)) {
      TestTools.deleteDir(tempDir);
    }
    Files.createDirectory(tempDir);
    Path tempTargetDir = tempDir.resolve("target");
    Files.createDirectory(tempTargetDir);

    Path testDir = Paths.get("test-dir");
    Path srcTargetJar = testDir.resolve("asm-test-1.0.jar");
    Path targetJar = tempTargetDir.resolve("asm-test-1.0.jar");
    Files.copy(srcTargetJar, targetJar);

    MavenProject mavenProject = Mockito.mock(MavenProject.class);
    Artifact artifactMain = Mockito.mock(Artifact.class);
    Mockito.when(artifactMain.getFile()).thenReturn(targetJar.toFile());
    Mockito.when(mavenProject.getArtifact()).thenReturn(artifactMain);

    Mockito.when(mavenProject.getBasedir()).thenReturn(tempDir.toFile());
    Mockito.when(mavenProject.getId()).thenReturn("com.mt-ag.tools:asm-test:1.0");

    Log mockLog = Mockito.mock(Log.class);


    Artifact artifactAsm = Mockito.mock(Artifact.class);
    Mockito.when(artifactAsm.getFile()).thenReturn(testDir.resolve("asm-7.1.jar").toFile());
    Artifact artifactLog4jApi = Mockito.mock(Artifact.class);
    Mockito.when(artifactLog4jApi.getFile()).thenReturn(testDir.resolve("log4j-api-2.11.2.jar").toFile());
    Artifact artifactLog4jCore = Mockito.mock(Artifact.class);
    Mockito.when(artifactLog4jCore.getFile()).thenReturn(testDir.resolve("log4j-core-2.11.2.jar").toFile());
    Set<Artifact> artifactSet = Set.of(artifactAsm, artifactLog4jApi, artifactLog4jCore);
    Mockito.when(mavenProject.getArtifacts()).thenReturn(artifactSet);

    MavenProjectHelper helper = Mockito.mock(MavenProjectHelper.class);

    JlinkMojo jlinkMojo = new JlinkMojo(mockLog, mavenProject, helper, CompressEnum.NoCompress, false, false, false,
        "test=asm.test/com.mt_ag.asm.Asm");
    assertThrows(MojoExecutionException.class, jlinkMojo::execute, "Expected Exception is not thrown!");

    assertFalse(Files.exists(tempTargetDir.resolve("asm-test-1.0.run.zip")), "Found result of jlink!");
    TestTools.deleteDir(tempDir);
  }

  /**
   * Test of exec with all parameters set.
   *
   * @throws MojoExecutionException not expected to be thrown.
   * @throws IOException            not expected to be thrown.
   */
  @Test
  public void execYamlStripAllOK() throws MojoExecutionException, IOException {
    Path tempDir = Paths.get("temp");
    if (Files.exists(tempDir)) {
      TestTools.deleteDir(tempDir);
    }
    Files.createDirectory(tempDir);
    Path tempTargetDir = tempDir.resolve("target");
    Files.createDirectory(tempTargetDir);

    Path testDir = Paths.get("test-dir", "jackson-yaml");
    Path srcTargetJar = testDir.resolve("yaml-example-1.0-SNAPSHOT.jar");
    Path targetJar = tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.jar");
    Files.copy(srcTargetJar, targetJar);

    MavenProject mavenProject = Mockito.mock(MavenProject.class);
    Artifact artifactMain = Mockito.mock(Artifact.class);
    Mockito.when(artifactMain.getFile()).thenReturn(targetJar.toFile());
    Mockito.when(mavenProject.getArtifact()).thenReturn(artifactMain);

    Mockito.when(mavenProject.getBasedir()).thenReturn(tempDir.toFile());
    Mockito.when(mavenProject.getId()).thenReturn("com.mt-ag.tools:yaml-example:1.0-SNAPSHOT");

    Log mockLog = Mockito.mock(Log.class);


    Artifact artifactSnakeyaml = Mockito.mock(Artifact.class);
    Mockito.when(artifactSnakeyaml.getFile()).thenReturn(testDir.resolve("snakeyaml-1.18.jar").toFile());
    Artifact artifactJackson1 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson1.getFile()).thenReturn(testDir.resolve("jackson-annotations-2.9.0.jar").toFile());
    Artifact artifactJackson2 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson2.getFile()).thenReturn(testDir.resolve("jackson-core-2.9.3.jar").toFile());
    Artifact artifactJackson3 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson3.getFile()).thenReturn(testDir.resolve("jackson-databind-2.9.3.jar").toFile());
    Artifact artifactJackson4 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson4.getFile()).thenReturn(testDir.resolve("jackson-dataformat-yaml-2.9.3.jar").toFile());
    Set<Artifact> artifactSet = Set.of(artifactSnakeyaml, artifactJackson1, artifactJackson2, artifactJackson3,
        artifactJackson4);
    Mockito.when(mavenProject.getArtifacts()).thenReturn(artifactSet);

    MavenProjectHelper helper = Mockito.mock(MavenProjectHelper.class);

    JlinkMojo jlinkMojo = new JlinkMojo(mockLog, mavenProject, helper, CompressEnum.NoCompress, false, false, true,
        "test=yaml.example/com.mt_ag.tools.config.gui.LinksDesktop");
    jlinkMojo.execute();

    assertTrue(Files.exists(tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.run.zip")), "Missing result of jlink!");
    TestTools.deleteDir(tempDir);
  }

  /**
   * Test of exec with all parameters set and openmodule=true results in an error.
   *
   * @throws IOException            not expected to be thrown.
   */
  @Test
  public void execYamlOpenModuleError() throws IOException {
    Path tempDir = Paths.get("temp");
    if (Files.exists(tempDir)) {
      TestTools.deleteDir(tempDir);
    }
    Files.createDirectory(tempDir);
    Path tempTargetDir = tempDir.resolve("target");
    Files.createDirectory(tempTargetDir);

    Path testDir = Paths.get("test-dir", "jackson-yaml");
    Path srcTargetJar = testDir.resolve("yaml-example-1.0-SNAPSHOT.jar");
    Path targetJar = tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.jar");
    Files.copy(srcTargetJar, targetJar);

    MavenProject mavenProject = Mockito.mock(MavenProject.class);
    Artifact artifactMain = Mockito.mock(Artifact.class);
    Mockito.when(artifactMain.getFile()).thenReturn(targetJar.toFile());
    Mockito.when(mavenProject.getArtifact()).thenReturn(artifactMain);

    Mockito.when(mavenProject.getBasedir()).thenReturn(tempDir.toFile());
    Mockito.when(mavenProject.getId()).thenReturn("com.mt-ag.tools:yaml-example:1.0-SNAPSHOT");

    Log mockLog = Mockito.mock(Log.class);


    Artifact artifactSnakeyaml = Mockito.mock(Artifact.class);
    Mockito.when(artifactSnakeyaml.getFile()).thenReturn(testDir.resolve("snakeyaml-1.18.jar").toFile());
    Artifact artifactJackson1 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson1.getFile()).thenReturn(testDir.resolve("jackson-annotations-2.9.0.jar").toFile());
    Artifact artifactJackson2 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson2.getFile()).thenReturn(testDir.resolve("jackson-core-2.9.3.jar").toFile());
    Artifact artifactJackson3 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson3.getFile()).thenReturn(testDir.resolve("jackson-databind-2.9.3.jar").toFile());
    Artifact artifactJackson4 = Mockito.mock(Artifact.class);
    Mockito.when(artifactJackson4.getFile()).thenReturn(testDir.resolve("jackson-dataformat-yaml-2.9.3.jar").toFile());
    Set<Artifact> artifactSet = Set.of(artifactSnakeyaml, artifactJackson1, artifactJackson2, artifactJackson3,
        artifactJackson4);
    Mockito.when(mavenProject.getArtifacts()).thenReturn(artifactSet);

    MavenProjectHelper helper = Mockito.mock(MavenProjectHelper.class);

    JlinkMojo jlinkMojo = new JlinkMojo(mockLog, mavenProject, helper, CompressEnum.NoCompress, true, true, true,
        "test=yaml.example/com.mt_ag.tools.config.gui.LinksDesktop");
    assertThrows(MojoExecutionException.class, jlinkMojo::execute, "Expected Exception is not thrown!");

    assertTrue(Files.exists(tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.run.zip")), "Missing result of jlink!");
    TestTools.deleteDir(tempDir);
  }
}