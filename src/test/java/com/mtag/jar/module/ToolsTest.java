package com.mtag.jar.module;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test of the Tools")
class ToolsTest {

  @Test
  void callInDir() {
  }

  @Test
  void setModuleMain() throws MojoExecutionException, IOException {
    Path testPath = Paths.get("test-dir");
    Path orgPath = testPath.resolve("install-1.0.org.jar");
    Path copyPath = testPath.resolve("install-1.0.jar");
    Files.deleteIfExists(copyPath);
    Files.copy(orgPath, copyPath);
    Tools.setModuleMain(new MockLogger(), copyPath);

    ModuleReference mr = ModuleFinder.of(copyPath).findAll().stream().findFirst().get();
    assertEquals("com.mtag.tools.config.gui.LinksDesktop", mr.descriptor().mainClass().
        orElse(null), "main class is not as expected!");
  }
}