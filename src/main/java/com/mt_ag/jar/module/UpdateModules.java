package com.mtag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Interface to update the jar as modules. Generates the module-info.java for the jar.
 */
public abstract class UpdateModules extends AbstractMojo {

  /**
   * The length of an extension.
   */
  public static final int EXTENSION_LENGTH = 4;

  /**
   * Maven parameter to set if it generates an open module or a standard module.
   */
  @Parameter
  private boolean openmodule;

  /**
   * Calls a command in the dir.
   *
   * @param dir   the working dir of the call.
   * @param param the list of parameters.
   * @throws MojoExecutionException thrown, when an error occurs.
   */
  protected void callInDir(Path dir, String... param) throws MojoExecutionException {
    Tools.callInDir(getLog(), dir, param);
  }

  /**
   * Creates a zip of the dir.
   *
   * @param dir        the dir to zip.
   * @param targetPath the targetPath.
   * @param subName    the sub name.
   * @return the used zipPath.
   * @throws MojoExecutionException thrown if an error occurs.
   */
  protected Path zipDir(Path dir, Path targetPath, String subName) throws MojoExecutionException {
    String targetFileName = targetPath.getFileName().toString();
    String runZipName = targetFileName.substring(0, targetFileName.length() - UpdateModules.EXTENSION_LENGTH)
        + "." + subName + ".zip";
    Path zipFile = targetPath.resolveSibling(runZipName);

    try (ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
         ZipOutputStream zipTmpOut = new ZipOutputStream(tmpOut);
         ZipOutputStream zipFileOut = new ZipOutputStream(Files.newOutputStream(zipFile,
             StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
      zipDir("", zipTmpOut, tmpOut, zipFileOut, dir);
      return zipFile;
    } catch (IOException e) {
      throw new MojoExecutionException("Error writing zip!", e);
    }
  }

  /**
   * Copies the entries of the dir to the streams. Uses it self to copy sub dirs.
   *
   * @param parent     the name of the parent.
   * @param zipTmpOut  the tmp zipOutput.
   * @param tmpOut     the tmp out for resetting the buffer.
   * @param zipFileOut the file output.
   * @param dir        the dir
   * @throws IOException thrown if an io error occurs.
   */
  private void zipDir(String parent, ZipOutputStream zipTmpOut, ByteArrayOutputStream tmpOut,
                      ZipOutputStream zipFileOut, Path dir) throws IOException {
    try (Stream<Path> pathStream = Files.list(dir)) {
      for (Path file : pathStream.collect(Collectors.toList())) {
        if (Files.isDirectory(file)) {
          zipDir(parent + file.getFileName().toString() + "/", zipTmpOut, tmpOut, zipFileOut, file);
        } else {
          ZipEntry entry = new ZipEntry(parent + file.getFileName().toString());
          entry.setMethod(ZipEntry.DEFLATED);
          zipTmpOut.putNextEntry(entry);
          Files.copy(file, zipTmpOut);
          zipTmpOut.closeEntry();
          tmpOut.reset();
          ZipEntry sizedEntry = new ZipEntry(entry);
          if (sizedEntry.getCompressedSize() >= sizedEntry.getSize()) {
            sizedEntry.setMethod(ZipEntry.STORED);
            sizedEntry.setCompressedSize(sizedEntry.getSize());
          }
          zipFileOut.putNextEntry(sizedEntry);
          Files.copy(file, zipFileOut);
          zipFileOut.closeEntry();
        }
      }
    }
  }

  /**
   * Calls the commands to generate the module-info.java.
   *
   * @param workDir the working directory (target/modules).
   * @param jarPath the path to the jar.
   * @throws MojoExecutionException thrown, when an error occurs.
   */
  private void callCommands(Path workDir, Path jarPath) throws MojoExecutionException {
    String jarName = jarPath.getFileName().toString();
    String subDir = jarName.substring(0, jarName.length() - EXTENSION_LENGTH);
    String moduleType = (openmodule) ? "--generate-open-module" : "--generate-module-info";
    callInDir(workDir, "jdeps", "-q", "--module-path", ".", moduleType, subDir, jarName);

    Path subDirPath = workDir.resolve(subDir);
    try (Stream<Path> pathStream = Files.list(subDirPath)) {
      Path modulePath = pathStream.findFirst().get();
      String moduleName = modulePath.getFileName().toString();
      try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
        Path rootDir = fs.getPath(".");
        copyDir(rootDir, modulePath);
      }
      callInDir(subDirPath, "javac", "--module-source-path", ".", "-d", ".", "--module-path", "..", "-m", moduleName);
      callInDir(workDir, "jar", "--update", "--file", jarName, "--module-version", "1.0",
          "-C", subDir + "/" + moduleName, "module-info.class");
    } catch (IOException e) {
      throw new MojoExecutionException("unable to find module dir for jar: " + jarName, e);
    }
  }

  /**
   * Copies an dir and all its sub dirs.
   *
   * @param srcDir    the source dir.
   * @param targetDir the target dir.
   * @throws IOException is thrown if an IOException is thrown.
   */
  private void copyDir(Path srcDir, Path targetDir) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(srcDir)) {
      for (Path path : dirStream) {
        if (Files.isDirectory(path)) {
          Path newTargetDir = targetDir.resolve(path.getFileName().toString());
          Files.createDirectory(newTargetDir);
          copyDir(path, newTargetDir);
        } else {
          Files.copy(path, targetDir.resolve(path.getFileName().toString()));
        }
      }
    }
  }

  /**
   * Sets the module-info in the jar.
   *
   * @param workDir the work dir.
   * @param jarPath the jar path.
   * @throws MojoExecutionException is thrown if an IOException is thrown.
   */
  private void setModuleInfo(Path workDir, Path jarPath) throws MojoExecutionException {
    String jarName = jarPath.getFileName().toString();
    String subDir = jarName.substring(0, jarName.length() - EXTENSION_LENGTH);
    Path subDirPath = workDir.resolve(subDir);

    try (Stream<Path> pathSteam = Files.list(subDirPath)) {
      Path moduleSubPath = pathSteam.findFirst().get();
      String jarModuleName = moduleSubPath.getFileName().toString();
      callInDir(workDir, "jar", "--update", "--file", jarName, "--module-version", "1.0",
          "-C", subDir + "/" + jarModuleName, "module-info.class");
    } catch (IOException e) {
      throw new MojoExecutionException("unable to find module dir for update in jar: " + jarName, e);
    }
    Tools.setModuleMain(getLog(), workDir.resolve(jarName));
  }

  /**
   * Creates a copy of the jar of the dependencies to test and update the modules.
   *
   * @param project the maven project.
   * @return return the path of the modules dir.
   * @throws MojoExecutionException is thrown if an IOException is thrown.
   */
  protected Path createModules(MavenProject project) throws MojoExecutionException {
    Path targetJar = project.getArtifact().getFile().toPath();

    Path modulesPath = project.getBasedir().toPath().resolve("target").resolve("modules");
    if (!Files.exists(modulesPath)) {
      try {
        Files.createDirectory(modulesPath);
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to create dir: ", e);
      }
    } else {
      return modulesPath;
    }

    project.getArtifacts().forEach(new Consumer<Artifact>() {
      @Override
      public void accept(Artifact artifact) {
        File sourceJar = artifact.getFile();
        try {
          Files.copy(sourceJar.toPath(), modulesPath.resolve(sourceJar.getName()));
        } catch (IOException e) {
          getLog().error("Unable to copy jar!", e);
        }
      }
    });

    List<Path> autoJars;
    try (Stream<Path> pathStream = Files.list(modulesPath)) {
      autoJars = pathStream.filter(new Predicate<Path>() {
        @Override
        public boolean test(Path path) {
          ModuleReference mr = ModuleFinder.of(path).findAll().stream().findFirst().get();
          return mr.descriptor().isAutomatic();
        }
      }).collect(Collectors.toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Error getting files", e);
    }

    for (Path jarPath : autoJars) {
      callCommands(modulesPath, jarPath);
    }

    for (Path jarPath : autoJars) {
      setModuleInfo(modulesPath, jarPath);
    }

    Path newTargetJar = modulesPath.resolve(targetJar.getFileName().toString());
    try {
      Files.copy(targetJar, newTargetJar);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to created jar!", e);
    }

    Tools.setModuleMain(getLog(), newTargetJar);
    return modulesPath;
  }
}
