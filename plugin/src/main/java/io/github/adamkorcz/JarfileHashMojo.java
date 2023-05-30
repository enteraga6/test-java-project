package io.github.adamkorcz;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.LinkedList;

@Mojo(name = "hash-jarfile", defaultPhase = LifecyclePhase.PACKAGE)
public class JarfileHashMojo extends AbstractMojo {
    private final String jsonBase = "{\"version\": \"0.1.0\", \"attestations\":[%ATTESTATIONS%]}";
    private final String attestationTemplate = "{\"name\": \"%NAME%.intoto\",\"subjects\":[{\"name\": \"%NAME%\",\"digest\":{\"sha256\":\"%HASH%\"}}]}";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            StringBuilder attestations = new StringBuilder();

            File targetDir = new File(project.getBasedir(), "target");
            File outputJson = new File(targetDir, "hash.json");
            for (File file : targetDir.listFiles()) {
                String filePath = file.getAbsolutePath();
                if (filePath.endsWith(".pom") || filePath.endsWith(".jar")) {
                    byte[] data = Files.readAllBytes(file.toPath());
                    byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
                    String checksum = new BigInteger(1, hash).toString(16);

                    String attestation = attestationTemplate.replaceAll("%NAME%", file.getName());
                    attestation = attestation.replaceAll("%HASH%", checksum);
                    if (attestations.length() > 0) {
                        attestations.append(",");
                    }
                    attestations.append(attestation);
                }
            }
            String json = jsonBase.replaceAll("%ATTESTATIONS%", attestations.toString());

            Files.write(outputJson.toPath(), new JSONObject(json).toString(4).getBytes());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new MojoFailureException("Fail to generate hash for the jar files", e);
        }

    }
}
