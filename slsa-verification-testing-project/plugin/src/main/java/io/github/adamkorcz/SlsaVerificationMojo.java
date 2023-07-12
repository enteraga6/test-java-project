package io.github.adamkorcz;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.util.Set;

/**
 * Resolve all dependencies defined in the pom.xml, from the default or specified remote repositories.
 * Each of the defined dependency artifacts that have slsa provenance provided is then verified by
 * slsa-verifier. All other dependency artifacts without slsa provenance file are skipped. Caveat:
 * will always check the central repository defined in the super pom first. You could use a mirror
 * entry in your <code>settings.xml</code>. This plugin execute in the VALIDATE phase of the build
 * and print the slsa verification result. It will never fail the build process. Remark: This plugin
 * requires golang installation for slsa verification process. Otherwise, the execution of this plugin
 * will be skipped.
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VALIDATE)
public class SlsaVerificationMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
      * Custom path of GOHOME, default value is $HOME/go
      */
    @Parameter(property = "slsa.gohome")
    private String gohome;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;


    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check Go Home directory
        if ((gohome == null) || gohome.equals("")) {
          gohome = System.getProperty("user.home") + "/go";
        }
        if (!(new File(gohome + "/bin")).exists()) {
            getLog().info("Skipping slsa verification: Golang not installed or GOHOME not set properly.");
            return;
        }

        // Install slsa verifier with go
        try {
            executeMojo(
                plugin(
                    groupId("org.codehaus.mojo"),
                    artifactId("exec-maven-plugin"),
                    version("3.1.0")
                ),
                goal("exec"),
                configuration(
                    element(name("executable"), "go"),
                    element(name("commandlineArgs"), "install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest")
                ),
                executionEnvironment(
                    project,
                    mavenSession,
                    pluginManager
                )
            );
        } catch(MojoExecutionException e) {
            getLog().info("Skipping slsa verification: Fail to retrieve slsa-verifier.");
            return;
        }

        // Verify the slsa of each dependency
        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
        for (Artifact artifact : dependencyArtifacts ) {
            // Retrieve the dependency jar and its slsa file
            String artifactStr = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
            try {
                // Retrieve the slsa file of the artifact
                executeMojo(
                    plugin(
                        groupId("com.googlecode.maven-download-plugin"),
                        artifactId("download-maven-plugin"),
                        version("1.7.0")
                    ),
                    goal("artifact"),
                    configuration(
                        element(name("outputDirectory"), "${project.build.directory}/slsa"),
                        element(name("groupId"), artifact.getGroupId()),
                        element(name("artifactId"), artifact.getArtifactId()),
                        element(name("version"), artifact.getVersion()),
                        element(name("type"), "intoto.build.slsa"),
                        element(name("classifier"), "jar")
                    ),
                    executionEnvironment(
                        project,
                        mavenSession,
                        pluginManager
                    )
                );

                // Retrieve the dependency jar if slsa file does exists for this artifact
                executeMojo(
                    plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("3.6.0")
                    ),
                    goal("copy"),
                    configuration(
                        element(name("outputDirectory"), "${project.build.directory}/slsa"),
                        element(name("artifact"), artifactStr)
                    ),
                    executionEnvironment(
                        project,
                        mavenSession,
                        pluginManager
                    )
                );
            } catch(MojoExecutionException e) {
                getLog().info("Skipping slsa verification for " + artifactStr + ": No slsa file found.");
                continue;
            }

            // Verify slsa file
            try {
                // Run slsa verification on the artifact and print the result
                // It will never fail the build process
                String arguments = "verify-artifact --provenance-path ";
                arguments += "${project.build.directory}/slsa/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "-jar.intoto.build.slsa ";
                arguments += " --source-uri ./ ${project.build.directory}/slsa/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
                executeMojo(
                    plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("exec-maven-plugin"),
                        version("3.1.0")
                    ),
                    goal("exec"),
                    configuration(
                        element(name("executable"), gohome + "/bin/slsa-verifier"),
                        element(name("commandlineArgs"), arguments),
                        element(name("useMavenLogger"), "true")
                    ),
                    executionEnvironment(
                        project,
                        mavenSession,
                        pluginManager
                    )
                );
            } catch(MojoExecutionException e) {
                // TODO: modify the failing and exception handling once the slsa verifier is working properly.
                getLog().info("Skipping slsa verification: Fail to run slsa verifier.");
                return;
            }
        }
    }
}
