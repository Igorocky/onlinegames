package org.igor.onlinegames.tools;

import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Release {
    public static void main(String[] args) throws IOException, InterruptedException {
        checkWorkingDirectory();
        String releaseVersion = setReleaseProjectVersion();
        buildProject();
        commit("Release " + releaseVersion);
        tag("release-" + releaseVersion);
        String newVersion = incProjectVersion();
        commit("Increase project version " + newVersion);
    }

    private static String setReleaseProjectVersion() throws IOException {
        log("setReleaseProjectVersion");
        final String[] version = {null};
        final File pomFile = new File("./pom.xml");
        RegexReplace.replace(
                pomFile,
                Pattern.compile("<version>(\\d+\\.\\d+\\.\\d+)-SNAPSHOT</version>"),
                matcher -> {
                    if (version[0] == null) {
                        version[0] = matcher.group(1);
                        return "<version>" + matcher.group(1) + "</version>";
                    } else {
                        return null;
                    }
                },
                pomFile
        );
        if (version[0] == null) {
            throw new RuntimeException("Failed to change project version from SNAPSHOT to RELEASE.");
        }
        return version[0];
    }

    private static void checkWorkingDirectory() throws IOException {
        log("checkFiles");
        Pair<String, Matcher> result = runCommand(
                "git status",
                Pattern.compile("nothing to commit, working tree clean")
        );
        if (result == null) {
            throw new RuntimeException("Working directory is not clean.");
        }
    }

    private static void buildProject() throws IOException {
        log("buildProject");
        Pair<String, Matcher> result = runCommand(
                "mvn clean install",
                Pattern.compile("(.*\\[INFO\\] BUILD SUCCESS.*)|(.*\\[INFO\\] BUILD FAILURE.*)")
        );
        if (result == null || result.getLeft().contains("BUILD FAILURE")) {
            throw new RuntimeException("Project build failed.");
        }
    }

    private static void commit(String commitMessage) throws IOException, InterruptedException {
        int exitCode = runCommandForExitValue("git commit -a -m \"" + commitMessage + "\"");
        if (0 != exitCode) {
            throw new RuntimeException("exitCode = " + exitCode);
        }
    }

    private static void tag(String tagName) throws IOException, InterruptedException {
        int exitCode = runCommandForExitValue("git tag " + tagName);
        if (0 != exitCode) {
            throw new RuntimeException("exitCode = " + exitCode);
        }
    }

    private static String incProjectVersion() throws IOException {
        log("incProjectVersion");
        final String[] newVersion = {null};
        final File pomFile = new File("./pom.xml");
        RegexReplace.replace(
                pomFile,
                Pattern.compile("<version>(\\d+)\\.(\\d+)\\.(\\d+)</version>"),
                matcher -> {
                    if (newVersion[0] == null) {
                        int v1 = Integer.parseInt(matcher.group(1));
                        int v2 = Integer.parseInt(matcher.group(2));
                        int v3 = Integer.parseInt(matcher.group(3));
                        newVersion[0] = v1 + "." + v2 + "." + (v3+1) + "-SNAPSHOT";
                        return "<version>" + newVersion[0] + "</version>";
                    } else {
                        return null;
                    }
                },
                pomFile
        );
        if (newVersion[0] == null) {
            throw new RuntimeException("Failed to change project version from RELEASE to SNAPSHOT.");
        }
        return newVersion[0];
    }

    private static Pair<String, Matcher> runCommand(String command, Pattern pattern) throws IOException {
        log("Command: " + command);
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process proc = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        Pair<String, Matcher> result = readTill(reader, pattern);
        proc.destroy();
        return result;
    }

    private static int runCommandForExitValue(String command) throws IOException, InterruptedException {
        log("Command: " + command);
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process proc = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        readTill(reader, null);
        return proc.waitFor();
    }

    private static Pair<String, Matcher> readTill(BufferedReader reader, Pattern pattern) throws IOException {
        List<String> lines = new ArrayList<>();
        Matcher matcher = null;
        String line;
        do {
            line = reader.readLine();
            log(line);
            lines.add(line);
            if (line == null) {
                return null;
            }
            if (pattern != null) {
                matcher = pattern.matcher(line);
            }
        } while (line != null && (matcher == null || !matcher.matches()));
        return Pair.of(line, matcher);
    }

    private static void log(String msg) {
        System.out.println("release>>> " + msg);
    }
}
