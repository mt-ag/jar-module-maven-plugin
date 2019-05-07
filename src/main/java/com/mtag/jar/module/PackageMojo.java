package com.mtag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;

/**
 * Implements the goal javapackager. Javapackager calls the javapackager command. Before it is done, it copies all
 * needed jar to the module dir. Than it tests if the jars are automatic modules, if so it generates the module-info
 * and adds it to the jar.
 */
@Mojo(name = "javapackager", defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyCollection = ResolutionScope.RUNTIME, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageMojo extends UpdateModules {

  /**
   * The installer dir name.
   */
  private static final String INSTALLER_DIR_NAME = "installer";

  /**
   * The part name and the classifier name.
   */
  private static final String INSTALL_PART_NAME = "install";

  /**
   * The enum of the output type.
   */
  public enum NativeType {
    /**
     * installer all for one platform.
     */
    installer,
    /**
     * image the image that is used for the installers.
     */
    image,
    /**
     * exe the windows exe.
     */
    exe,
    /**
     * msi the windows msi file.
     */
    msi,
    /**
     * dmg the mac dmg.
     */
    dmg,
    /**
     * pkg the mac pkg.
     */
    pkg,
    /**
     * rpm the linux rpm.
     */
    rpm,
    /**
     * deb the linux deb.
     */
    deb
  }

  /**
   * The generated installer type.
   */
  @Parameter(property = "nativeType", defaultValue = "image")
  private NativeType nativeType;

  /**
   * The title of the app.
   */
  @Parameter(property = "appTitle", defaultValue = "")
  private String title;

  /**
   * The name of the app.
   */
  @Parameter(property = "appName", defaultValue = "")
  private String appName;

  /**
   * The name of the vendor.
   */
  @Parameter(property = "appVendor", defaultValue = "my vendor")
  private String appVendor;

  /**
   * The name of the menu group.
   */
  @Parameter(property = "appMenuGroup", defaultValue = "my vendor")
  private String appMenuGroup;

  /**
   * The maven project.
   */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * Maven ProjectHelper.
   */
  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The implementation of the mojo.
   *
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {
    Path modulesPath = createModules(project);

    Path targetJar = project.getArtifact().getFile().toPath();
    for (Artifact art : project.getAttachedArtifacts()) {
      getLog().info("attached artifact: " + art.getFile().getName());
    }
    ModuleDescriptor md = ModuleFinder.of(targetJar).findAll().stream().findFirst().get().descriptor();
    String moduleName = md.name();
    Optional<String> main = md.mainClass();
    if (!main.isPresent()) {
      try (JarFile jf = new JarFile(targetJar.toFile())) {
        main = Optional.ofNullable(jf.getManifest().getMainAttributes().getValue("Main-Class"));
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to read target jar", e);
      }
    }
    if (!main.isPresent()) {
      throw new MojoExecutionException("No main is set in jar");
    }

    getLog().info("Found module:" + moduleName);
    try {
      Files.createDirectory(modulesPath.resolve("inst"));
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to create dir inst!");
    }

    String appTitle = title != null ? title : moduleName;
    String name = appName != null ? appName : moduleName;
    callInDir(modulesPath, "javapackager", "-deploy", "-native", nativeType.name(), "-p", ".", "-srcdir",
        "inst", "-m", moduleName, "-name", name, "-appclass", main.get(), "-outdir", INSTALLER_DIR_NAME, "-title",
        appTitle, "-Bvendor=" + appVendor, "-Bwin.menuGroup=" + appMenuGroup);

    Path instZip = zipDir(modulesPath.resolve(INSTALLER_DIR_NAME), targetJar, INSTALL_PART_NAME);
    projectHelper.attachArtifact(project, "zip", INSTALL_PART_NAME, instZip.toFile());
  }
}
