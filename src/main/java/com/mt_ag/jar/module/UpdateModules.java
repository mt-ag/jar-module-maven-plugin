package com.mt_ag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Interface to update the jar as modules. Generates the module-info.java for the jar.
 */
public abstract class UpdateModules extends AbstractMojo {

  /**
   * The const --module-path.
   */
  private static final String MODULE_PATH = "--module-path";

  /**
   * The const jdeps.
   */
  private static final String JDEPS = "jdeps";

  /**
   * The const jdeps.
   */
  private static final String LOCAL_DIR = ".";

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
   * Maven parameter to set if the unresolved classes should be removed.
   */
  @Parameter
  private boolean removeUnresolvedClasses;

  /**
   * The standard constructor.
   */
  public UpdateModules() {

  }

  /**
   * The constructor for tests.
   * @param pOpenmodule use as openmodule.
   */
  protected UpdateModules(boolean pOpenmodule) {
    openmodule = pOpenmodule;
  }

  /**
   * Calls a command in the dir.
   *
   * @param dir   the working dir of the call.
   * @param param the list of parameters.
   * @return lines returned.
   * @throws MojoExecutionException thrown, when an error occurs.
   */
  protected CallResult callInDir(Path dir, String... param) throws MojoExecutionException {
    return Tools.callInDir(getLog(), dir, param);
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
  private static void zipDir(String parent, ZipOutputStream zipTmpOut, ByteArrayOutputStream tmpOut,
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
   * Reads the missing classes from the lines of jedps answer.
   *
   * @param lines the lines.
   * @return the not found classes.
   */
  private static Set<String> readMissingClassesFromLines(List<String> lines) {
    Set<String> classes = new TreeSet<>();
    for (String line : lines) {
      if (line.endsWith("not found")) {
        classes.add(line.trim().replaceAll("\\s+", " ").split("\\s")[2]);
      }
    }
    return classes;
  }

  /**
   * Calls the commands to generate the module-info.java.
   *
   * @param workDir the working directory (target/modules).
   * @param jarPath the path to the jar.
   * @throws MojoExecutionException thrown, when an error occurs.
   */
  private void addModuleInfo(Path workDir, Path jarPath) throws MojoExecutionException {
    String jarName = jarPath.getFileName().toString();
    String subDir = jarName.substring(0, jarName.length() - EXTENSION_LENGTH);
    String moduleType = (openmodule) ? "--generate-open-module" : "--generate-module-info";
    String moduleName = ModuleFinder.of(jarPath).findAll().stream().findFirst().get().descriptor().name();
    Path modulePath = workDir.resolve(subDir + '/' + moduleName);
    List<String> lines = callInDir(workDir, JDEPS, MODULE_PATH, LOCAL_DIR, moduleType, subDir, jarName).getOutLines();
    Set<String> classes = readMissingClassesFromLines(lines);

    if (!classes.isEmpty()) {
      addClassesToMissingModule(jarPath, classes);
      callInDir(workDir, JDEPS, MODULE_PATH, LOCAL_DIR, moduleType, subDir, jarName);
      Path moduleInfoPath = modulePath.resolve("module-info.java");
      try {
        List<String> infoLines = Files.readAllLines(moduleInfoPath, StandardCharsets.UTF_8);
        try (BufferedWriter wb = Files.newBufferedWriter(moduleInfoPath, StandardOpenOption.TRUNCATE_EXISTING)) {
          for (String line : infoLines) {
            if (line.endsWith("missing;")) {
              wb.write("  requires static missing;");
            } else {
              wb.write(line);
            }
            wb.newLine();
          }
        }
      } catch (IOException e) {
        throw new MojoExecutionException("unable to find module dir for jar: " + jarName, e);
      }
    }

    Path subDirPath = workDir.resolve(subDir);
    try {
      try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
        Path rootDir = fs.getPath(".");
        copyDir(rootDir, modulePath);
      }
      callInDir(subDirPath, "javac", "--module-source-path", LOCAL_DIR, "-d", LOCAL_DIR, MODULE_PATH, "..",
          "-m", moduleName);
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
  private static void copyDir(Path srcDir, Path targetDir) throws IOException {
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
   * @param jarPath the jar path.
   * @throws MojoExecutionException is thrown if an IOException is thrown.
   */
  private static void makeSingleVersionJar(Path jarPath) throws MojoExecutionException {
    String jarName = jarPath.getFileName().toString();
    Path tempJar = jarPath.resolveSibling(jarName + ".temp");

    try (JarFile jarFile = new JarFile(jarPath.toFile(), false, ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
      try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(tempJar, StandardOpenOption.CREATE))) {
        List<JarEntry> entryList = jarFile.versionedStream().collect(Collectors.toList());
        for (JarEntry jarEntry : entryList) {
          if (!jarEntry.isDirectory()) {
            byte[] data = jarFile.getInputStream(jarEntry).readAllBytes();
            if (jarEntry.getName().equals("META-INF/MANIFEST.MF")) {
              String manifestText = new String(data, StandardCharsets.UTF_8);
              data = manifestText.replace("Multi-Release: true\r\n", "").getBytes(StandardCharsets.UTF_8);
            }
            JarEntry newJarEntry = new JarEntry(jarEntry.getName());
            jarOut.putNextEntry(newJarEntry);
            jarOut.write(data);
            jarOut.closeEntry();
          }
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("unable read jar file: " + jarName, e);
    }
    try {
      Files.delete(jarPath);
      Files.move(tempJar, jarPath);
    } catch (IOException e) {
      throw new MojoExecutionException("unable delete or move jar file: " + jarName, e);
    }
  }

  /**
   * Adds the missing classes to the missing module.
   *
   * @param jarPath the path of the jar, that misses the classes.
   * @param classes the list of missing classes.
   * @throws MojoExecutionException is throw when an io error occurs.
   */
  private void addClassesToMissingModule(Path jarPath, Set<String> classes) throws MojoExecutionException {
    Path missingPath = jarPath.resolveSibling("missing");
    if (Files.notExists(missingPath)) {
      try {
        Files.createDirectory(missingPath);
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to create missing dir!", e);
      }
    }
    List<String> classRev = new ArrayList<>(classes);
    Collections.reverse(classRev);
    StringBuilder innerClasses = new StringBuilder();
    for (String clazz : classRev) {
      int posInner = clazz.indexOf('$');
      if (posInner > 0) {
        getLog().info("innerclass " + clazz);
        innerClasses.append("\npublic class ").append(clazz.substring(posInner + 1)).append(" {}");
      } else {
        writeAndCompileClass(missingPath, innerClasses, clazz);
        innerClasses.setLength(0);
      }
    }
    try {
      Files.deleteIfExists(jarPath.resolveSibling("missing.jar"));
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to delete existing missing.jar", e);
    }
    callInDir(missingPath, "jar", "--create", "--file=../missing.jar", "*");
  }

  /**
   * Method to write and compile a class.
   *
   * @param missingPath  the missing class.
   * @param innerClasses the inner classes.
   * @param clazz        the class.
   * @throws MojoExecutionException thrown if an IOException is thrown.
   */
  private void writeAndCompileClass(Path missingPath, StringBuilder innerClasses, String clazz)
      throws MojoExecutionException {
    String filename = clazz.replace('.', '/') + ".java";
    int pos = clazz.lastIndexOf('.');
    String packagge = clazz.substring(0, pos);
    String className = clazz.substring(pos + 1);
    Path clazzPath = missingPath.resolve(filename);
    Path parentPath = clazzPath.getParent();
    try {
      Files.createDirectories(parentPath);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to create package dirs for: " + clazz, e);
    }
    try (BufferedWriter out = Files.newBufferedWriter(clazzPath, StandardOpenOption.CREATE)) {
      out.append("package ").append(packagge).append(";");
      out.newLine();
      out.append("public class ").append(className).append("{ ").append(innerClasses).append("}");
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to write class: " + clazz, e);
    }
    callInDir(missingPath, "javac",
        packagge.replace('.', '/') + '/' + className + ".java");
    try {
      Files.delete(clazzPath);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to delete source of: " + clazz, e);
    }
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

    for (Artifact artifact : project.getArtifacts()) {
      File sourceJar = artifact.getFile();
      try {
        Files.copy(sourceJar.toPath(), modulesPath.resolve(sourceJar.getName()));
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to copy jar!", e);
      }
    }

    List<Path> multiJars;
    try (Stream<Path> pathStream = Files.list(modulesPath)) {
      multiJars = pathStream.filter(path -> {
            try (JarFile jarFile = new JarFile(path.toFile())) {
              return jarFile.isMultiRelease();
            } catch (IOException e) {
              return false;
            }
          }
      ).collect(Collectors.toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Error getting files", e);
    }

    for (Path path : multiJars) {
      makeSingleVersionJar(path);
    }

    List<Path> autoJars;
    try (Stream<Path> pathStream = Files.list(modulesPath)) {
      autoJars = pathStream.filter(path ->
          ModuleFinder.of(path).findAll().stream().findFirst().get().descriptor().isAutomatic()
      ).collect(Collectors.toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Error getting files", e);
    }

    for (Path jarPath : autoJars) {
      addModuleInfo(modulesPath, jarPath);
    }

    try {
      Files.deleteIfExists(modulesPath.resolve("missing.jar"));
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to delete missing.jar!", e);
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
