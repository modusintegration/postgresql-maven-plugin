package com.github.adrianboimvaser.postgresql.plugin;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;

@Mojo(name = "initdb")
public class InitdbMojo extends PgsqlMojo {

    @Parameter(required = true)
    protected String dataDir;

    @Parameter
    protected String username;

    @Parameter
    protected String passwordFile;

    @Parameter
    protected String encoding;

    @Parameter
    protected String locale;

    /** The {@code --data-checksums} option was added in PostgreSQL 9.3. Will be used if present and not {@code false}. */
    @Parameter(alias = "data-checksums", property = "data-checksums", defaultValue = "false")
    protected boolean dataChecksums;

    @Parameter(defaultValue = "true")
    protected boolean overWrite;

    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        if (skip) {
            getLog().debug("Skipped.");
            return;
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList("dependency:unpack", "antrun:run"));

        try {
            final File pom = File.createTempFile("pom", ".xml");
            pom.deleteOnExit();
            InputStream pomInputStream = this.getClass().getResourceAsStream("/pom.xml");

            try (FileOutputStream out = new FileOutputStream(pom)) {
                IOUtils.copy(pomInputStream, out);
            }

            Properties properties = new Properties();
            properties.setProperty("outputDirectory", new File(pgsqlHomeVersion).getParent());
            properties.setProperty("postgresql.version", version);
            properties.setProperty("overWrite", String.valueOf(overWrite));
            properties.setProperty("pgsqlHomeVersion", pgsqlHomeVersion);

            request.setProperties(properties);
            request.setPomFile(pom);
            request.setInteractive(false);

            Invoker invoker = new DefaultInvoker();
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException(result.getExecutionException().getMessage(), result.getExecutionException().getCause());
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }

        final List<String> cmd = createCommand();
        if (getLog().isDebugEnabled()) {
            getLog().debug(cmd.toString());
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        String message = "";
        Exception cause = null;
        int returnValue = Integer.MIN_VALUE;
        try {
            Process process = processBuilder.start();
            logOutput(process);
            returnValue = process.waitFor();
            message = "initdb returned " + returnValue;
            getLog().debug(message);
        } catch (IOException|InterruptedException e) {
            cause = e;
            message = e.getLocalizedMessage();
            getLog().error(e);
        }
        if (returnValue != 0 && failOnError) {
            throw new MojoExecutionException(message, cause);
        }
    }

    private List<String> createCommand() throws MojoExecutionException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(getCommandPath("initdb"));

        cmd.add("-D");
        cmd.add(dataDir);

        cmd.add("--nosync");

        if (username != null) {
            cmd.add("-U");
            cmd.add(username);
        }

        if (passwordFile != null) {
            cmd.add("--pwfile");
            cmd.add(passwordFile);
        }

        if (encoding != null) {
            cmd.add("--encoding");
            cmd.add(encoding);
        }

        if (locale != null) {
            cmd.add("--locale");
            cmd.add(locale);
        }

        if (dataChecksums) {
            cmd.add("--no-password");
        }

        return cmd;
    }
}
