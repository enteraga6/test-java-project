package io.github.adamkrocz;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleScriptException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.LinkedList;

public class JarfileHashTask extends DefaultTask {
    private final String jsonBase = "{\"version\": 1, \"attestations\":[%ATTESTATIONS%]}";
    private final String attestationTemplate = "{\"name\": \"%NAME%.intoto\",\"subjects\":[{\"name\": \"%NAME%\",\"digest\":{\"sha256\":\"%HASH%\"}}]}";
    private String outputJsonPath;

    @TaskAction
    public void jarfileHash() {
        try {
            this.outputJsonPath = System.getenv("JAFILE_HASH_OUTPUT_JSON_PATH");

            StringBuilder attestations = new StringBuilder();
            File buildDir = new File(this.getProject().getBuildDir(), "libs");
            File outputJson = this.getOutputJsonFile(buildDir.getAbsolutePath());

            for (File file : buildDir.listFiles()) {
                String filePath = file.getAbsolutePath();
                if (!filePath.endsWith("original") && (filePath.endsWith(".pom") || filePath.endsWith(".jar"))) {
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
            Files.write(outputJson.toPath(), json.getBytes());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new GradleScriptException("Fail to generate hash for the jar files", e);
        }
    }

    private File getOutputJsonFile(String targetDir) {
        try {
            if (this.outputJsonPath != null && this.outputJsonPath.length() > 0) {
                File outputJson = new File(outputJsonPath);
                if (!outputJson.exists() || !outputJson.isFile()) {
                    outputJson.getParentFile().mkdirs();
                    Files.createFile(outputJson.toPath());
                }

                if (Files.isWritable(outputJson.toPath())) {
                    return outputJson;
                }
            }
            return new File(targetDir, "hash.json");
        } catch (IOException e) {
            return new File(targetDir, "hash.json");
        }
    }
}
