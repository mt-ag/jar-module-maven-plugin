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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the Package Mojo.
 */
@DisplayName("Package Mojo Test")
public class PackageMojoTest {
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

    PackageMojo packageMojo = new PackageMojo(mockLog, mavenProject, helper, PackageMojo.NativeType.exe, false);
    packageMojo.execute();

    assertTrue(Files.exists(tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.install.zip")), "Missing result of packager!");
    TestTools.deleteDir(tempDir);
  }

  /**
   * Test of exec with all parameters set. Use open modules and get an error.
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

    PackageMojo packageMojo = new PackageMojo(mockLog, mavenProject, helper, PackageMojo.NativeType.msi, true);

    assertThrows(MojoExecutionException.class, packageMojo::execute);

    assertFalse(Files.exists(tempTargetDir.resolve("yaml-example-1.0-SNAPSHOT.install.zip")),
        "Missing result of packager!");
    TestTools.deleteDir(tempDir);
  }
}
