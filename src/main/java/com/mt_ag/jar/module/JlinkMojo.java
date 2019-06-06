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

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the goal jlink. Jlink calls the jlink command. Before it is done, it copies all needed jar to the module
 * dir. Than it tests if the jars are automatic modules, if so it generates the module-info and adds it to the jar.
 */
@Mojo(name = "jlink", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.RUNTIME,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JlinkMojo extends UpdateModules {

  /**
   * The compress Enable compression of resources:
   * NoCompression = 0
   * ConstantStringSharing = 1
   * ZIP = 2.
   */
  @Parameter(property = "compress", defaultValue = "ZIP")
  private CompressEnum compress;

  /**
   * Ignore the signing information in the jars.
   */
  @Parameter(property = "ignoreSigning", defaultValue = "true")
  private boolean ignoreSigning;

  /**
   * Don't add header files.
   */
  @Parameter(property = "noHeaderFiles", defaultValue = "true")
  private boolean noHeaderFiles;

  /**
   * Don't add man pages.
   */
  @Parameter(property = "noManPages", defaultValue = "true")
  private boolean noManPages;

  /**
   * Strip debug. Remove debug infos.
   */
  @Parameter(property = "stripDebug")
  private boolean stripDebug;

  /**
   * The launchers for the image.
   * A launcher is the name=module(/main-class)
   */
  @Parameter(property = "launcher")
  private List<String> launcherList;

  /**
   * The maven project. Used for dependencies and the own artifact.
   */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * Maven ProjectHelper.
   */
  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The used logger, set by excecute.
   */
  private Log myLog;

  /**
   * The default constructor.
   */
  public JlinkMojo() {
    super();
  }

  /**
   * The constructor for testing.
   *
   * @param pLog          the MockLog.
   * @param pProject      the param project.
   * @param helper        the project helper.
   * @param pCompress     the compress enum.
   * @param minData       nin data.
   * @param openmodule    openmodule.
   * @param launcherArray the launchers as array.
   */
  protected JlinkMojo(Log pLog, MavenProject pProject, MavenProjectHelper helper, CompressEnum pCompress,
                      boolean minData, boolean openmodule, String... launcherArray) {
    super(openmodule);
    myLog = pLog;
    project = pProject;
    projectHelper = helper;
    compress = pCompress;
    ignoreSigning = minData;
    noManPages = minData;
    noHeaderFiles = minData;
    stripDebug = minData;
    launcherList = List.of(launcherArray);
  }

  /**
   * The implementation of the Mojo.
   *
   * @throws MojoExecutionException is thrown if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {
    myLog = (myLog != null) ? myLog : getLog();
    Path dir = project.getBasedir().toPath().resolve("target");
    myLog.info("Project:" + project.getId());
    myLog.info("own artifact:" + project.getArtifact().getFile().getAbsolutePath());

    for (Artifact a : project.getArtifacts()) {
      myLog.info("Artifact:" + a.getFile().getAbsolutePath());
    }
    myLog.info("Work dir: " + dir.toAbsolutePath().toString());
    Path modulesPath = createModules(project);

    Path targetJar = project.getArtifact().getFile().toPath();
    String moduleName = ModuleFinder.of(targetJar).findAll().stream().findFirst().get().descriptor().name();
    myLog.info("Found module:" + moduleName);
    List<String> params = new ArrayList<>();
    params.add("jlink");
    if (ignoreSigning) {
      params.add("--ignore-signing-information");
    }
    if (stripDebug) {
      params.add("--strip-debug");
    }
    if (noHeaderFiles) {
      params.add("--no-header-files");
    }
    if (noManPages) {
      params.add("--no-man-pages");
    }
    for (String launcher : launcherList) {
      params.add("--launcher");
      params.add(launcher);
    }
    Collections.addAll(params, "--compress=" + compress.getRate(), "--module-path", ".", "--add-modules",
        moduleName, "--output", "run");

    CallResult result = callInDir(modulesPath, params.toArray(new String[0]));
    if (result.getExitVal() < 0) {
      throw new MojoExecutionException("Error in calling jlink!");
    }
    Path zipPath = zipDir(modulesPath.resolve("run"), targetJar, "run");
    projectHelper.attachArtifact(project, "zip", "run", zipPath.toFile());
  }
}
