package com.mt_ag.jar.module;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
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
   * The const My Vendor.
   */
  private static final String MY_VENDOR = "My Vendor";

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
  @Parameter(defaultValue = "image")
  private NativeType nativeType;

  /**
   * The title of the app.
   */
  @Parameter
  private String appTitle;

  /**
   * The name of the app.
   */
  @Parameter
  private String appName;

  /**
   * The name of the vendor.
   */
  @Parameter(defaultValue = MY_VENDOR)
  private String appVendor;

  /**
   * The name of the menu group.
   */
  @Parameter(defaultValue = MY_VENDOR)
  private String appMenuGroup;

  /**
   * The description of the app.
   */
  @Parameter(defaultValue = "A short description of the app")
  private String appDescription;

  /**
   * The maven project.
   */
  @Parameter(required = true, readonly = true)
  private MavenProject project;

  /**
   * Maven ProjectHelper.
   */
  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The used logger, set by execute.
   */
  private Log myLog;

  /**
   * The standard constructor.
   */
  public PackageMojo() {
    super();
  }

  /**
   * The constructor for testing.
   *
   * @param pLog          the MockLog.
   * @param pProject      the param project.
   * @param pHelper       the project helper.
   * @param pNative       nativeType.
   * @param openmodule    openmodule.
   */
  protected PackageMojo(Log pLog, MavenProject pProject, MavenProjectHelper pHelper,
                        NativeType pNative, boolean openmodule) {
    super(openmodule);
    myLog = pLog;
    project = pProject;
    projectHelper = pHelper;
    nativeType = pNative;
    appMenuGroup = MY_VENDOR;
    appVendor = MY_VENDOR;
    appDescription = "A short description";
  }

  /**
   * The implementation of the mojo.
   *
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {
    myLog = (myLog == null) ? getLog() : myLog;
    Path modulesPath = createModules(project);

    Path targetJar = project.getArtifact().getFile().toPath();
    for (Artifact art : project.getAttachedArtifacts()) {
      myLog.info("attached artifact: " + art.getFile().getName());
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

    myLog.info("Found module:" + moduleName);
    try {
      Files.createDirectory(modulesPath.resolve("inst"));
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to create dir inst!");
    }

    String title = appTitle != null ? appTitle : moduleName;
    String name = appName != null ? appName : moduleName;
    CallResult result = callInDir(modulesPath, "javapackager", "-deploy", "-native", nativeType.name(), "-p",
        ".", "-srcdir", "inst", "-m", moduleName, "-name", name, "-appclass", main.get(), "-outdir", INSTALLER_DIR_NAME,
        "-title", title, "-description", appDescription, "-Bvendor=" + appVendor, "-Bwin.menuGroup=" + appMenuGroup);

    if (result.getExitVal() < 0) {
      throw new MojoExecutionException("Error in calling javapackager!");
    }

    Path instZip = zipDir(modulesPath.resolve(INSTALLER_DIR_NAME), targetJar, INSTALL_PART_NAME);
    projectHelper.attachArtifact(project, "zip", INSTALL_PART_NAME, instZip.toFile());
  }
}
