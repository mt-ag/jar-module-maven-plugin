package com.mt_ag.jar.module;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Tools class.
 */
@DisplayName("Test of the Tools")
public class ToolsTest {

  /**
   * Tests the method callInDir.
   *
   * @throws MojoExecutionException not expected.
   */
  @Test
  void callInDir() throws MojoExecutionException {
    Log mockLogger = Mockito.mock(Log.class);
    Tools.callInDir(mockLogger, Paths.get("."), "jlink", "--version");
    assertTrue(true);
    Mockito.verify(mockLogger).info("command: jlink --version");
    Mockito.verify(mockLogger).info("exitVal: 0");
    Mockito.verify(mockLogger).info("out:");
  }

  /**
   * Tests the method setModuleMain, with module jar, where main class is set in the manifest but not
   * in the module.
   *
   * @throws MojoExecutionException not expected.
   * @throws IOException            not expected.
   */
  @Test
  void setModuleMainOk() throws MojoExecutionException, IOException {
    Log mockLogger = Mockito.mock(Log.class);

    Path testPath = Paths.get("test-dir");
    Path orgPath = testPath.resolve("install-1.0.org.jar");
    Path copyPath = testPath.resolve("install-1.0.jar");
    Files.deleteIfExists(copyPath);
    Files.copy(orgPath, copyPath);
    Tools.setModuleMain(mockLogger, copyPath);

    ModuleReference mr = ModuleFinder.of(copyPath).findAll().stream().findFirst().get();
    assertEquals("com.mtag.tools.config.gui.LinksDesktop", mr.descriptor().mainClass().
        orElse(null), "main class is not as expected!");
    Mockito.verify(mockLogger).info("MainClass: com.mtag.tools.config.gui.LinksDesktop");
  }

  /**
   * Tests the method setModuleMain, with wrong filename.
   */
  @Test
  void setModuleMainWrongFilename() {
    Log mockLogger = Mockito.mock(Log.class);

    Path testPath = Paths.get("test-dir");
    Path copyPath = testPath.resolve("install-1.0.err.jar");
    assertThrows(RuntimeException.class, () ->
        Tools.setModuleMain(mockLogger, copyPath));
  }

  /**
   * Tests the method setModuleMain, with no main set.
   *
   * @throws MojoExecutionException not expected.
   */
  @Test
  void setModuleMainNoMain() throws MojoExecutionException {
    Log mockLogger = Mockito.mock(Log.class);

    Path testPath = Paths.get("test-dir");
    Path copyPath = testPath.resolve("install-noMain.jar");
    Tools.setModuleMain(mockLogger, copyPath);

    ModuleReference mr = ModuleFinder.of(copyPath).findAll().stream().findFirst().get();
    assertNull(mr.descriptor().mainClass().
        orElse(null), "main class is set! Not as expected!");
  }

  /**
   * Tests the method setModuleMain, with no module jar.
   *
   * @throws MojoExecutionException not expected.
   */
  @Test
  void setModuleMainNoModule() throws MojoExecutionException {
    Log mockLogger = Mockito.mock(Log.class);

    Path testPath = Paths.get("test-dir");
    Path copyPath = testPath.resolve("install-noModule.jar");
    Tools.setModuleMain(mockLogger, copyPath);

    ModuleReference mr = ModuleFinder.of(copyPath).findAll().stream().findFirst().get();
    assertNull(mr.descriptor().mainClass().
        orElse(null), "main class is set! Not as expected!");
  }

  /**
   * Tests the method setModuleMain, with module jar and main is set.
   *
   * @throws MojoExecutionException not expected.
   */
  @Test
  void setModuleMainOK() throws MojoExecutionException {
    Log mockLogger = Mockito.mock(Log.class);

    Path testPath = Paths.get("test-dir");
    Path copyPath = testPath.resolve("install-OK.jar");
    Tools.setModuleMain(mockLogger, copyPath);

    ModuleReference mr = ModuleFinder.of(copyPath).findAll().stream().findFirst().get();
    assertEquals("com.mtag.tools.config.gui.LinksDesktop", mr.descriptor().mainClass().
        orElse(null), "main class is set! Not as expected!");
  }
}