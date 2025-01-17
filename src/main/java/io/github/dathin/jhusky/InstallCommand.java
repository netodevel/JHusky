package io.github.dathin.jhusky;

import io.github.dathin.jhusky.components.GitValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "install")
public class InstallCommand extends AbstractMojo {

    @Parameter(property = "directory", defaultValue = ".husky")
    private String directory;

    private GitValidator gitValidator;


    @Override
    public void execute() throws MojoExecutionException {
        gitValidator = new GitValidator(getLog());
        try {
            // Check if it is a git repo
            gitValidator.isGitRepository(System.getProperty("user.dir"));

            String customDirHelp = "https://git.io/Jc3F9";
            if(directory.contains("..")) {
                throw new MojoExecutionException(".. not allowed (see" + customDirHelp + ")");
            }
            if (!Files.exists(Paths.get(".git"))){
                throw new MojoExecutionException(".git can't be found (see" + customDirHelp + ")");
            }

            Files.createDirectories(Paths.get(directory));
            Files.deleteIfExists(Paths.get(directory, "_", ".gitignore"));
            Files.deleteIfExists(Paths.get(directory, "_", "husky.sh"));
            Files.createDirectories(Paths.get(directory, "_"));
            Path createdFile = Files.createFile(Paths.get(directory, "_", "husky.sh"));
            createdFile.toFile().setExecutable(true);
            Files.write(createdFile, ("#!/bin/sh\n" +
                    "if [ -z \"$husky_skip_init\" ]; then\n" +
                    "  debug () {\n" +
                    "    if [ \"$HUSKY_DEBUG\" = \"1\" ]; then\n" +
                    "      echo \"husky (debug) - $1\"\n" +
                    "    fi\n" +
                    "  }\n" +
                    "\n" +
                    "  readonly hook_name=\"$(basename \"$0\")\"\n" +
                    "  debug \"starting $hook_name...\"\n" +
                    "\n" +
                    "  if [ \"$HUSKY\" = \"0\" ]; then\n" +
                    "    debug \"HUSKY env variable is set to 0, skipping hook\"\n" +
                    "    exit 0\n" +
                    "  fi\n" +
                    "\n" +
                    "  if [ -f ~/.huskyrc ]; then\n" +
                    "    debug \"sourcing ~/.huskyrc\"\n" +
                    "    . ~/.huskyrc\n" +
                    "  fi\n" +
                    "\n" +
                    "  export readonly husky_skip_init=1\n" +
                    "  sh -e \"$0\" \"$@\"\n" +
                    "  exitCode=\"$?\"\n" +
                    "\n" +
                    "  if [ $exitCode != 0 ]; then\n" +
                    "    echo \"husky - $hook_name hook exited with code $exitCode (error)\"\n" +
                    "  fi\n" +
                    "\n" +
                    "  exit $exitCode\n" +
                    "fi\n").getBytes());
            Path createdFile2 = Files.createFile(Paths.get(directory, "_", ".gitignore"));
            createdFile2.toFile().setExecutable(true);
            Files.write(createdFile2, "*".getBytes());


            // Change dir of hooks
            ProcessBuilder builder2 = new ProcessBuilder("git", "config", "core.hooksPath", directory);
            Process process2 = builder2.start();
            process2.waitFor();

            if (process2.exitValue() != 0) {
                throw new MojoExecutionException("Git hooks failed to install");
            }

            getLog().info("Git hooks installed");

        } catch (InterruptedException | IOException ex){
            ex.printStackTrace();
            throw new MojoExecutionException("Unable to uninstall: " + ex.getMessage());
        }
    }
}
