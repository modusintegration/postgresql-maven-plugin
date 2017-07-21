package com.github.adrianboimvaser.postgresql.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PgsqlMojoTest {
    private PgsqlMojo mojo = new PgsqlMojo(".", "9.5") {
        public void execute() {
            throw new UnsupportedOperationException("Test only");
        }
    };

    @Test(expected = MojoExecutionException.class)
    public void testGetCommandPathFail() throws MojoExecutionException {
        assertFalse(new PgsqlMojo("\\\\\\\\\\\\\\\\\\\\\\", "9.5") {
            public void execute() {
            }
        }.getCommandPath("pg_ctl").endsWith("/bin/pg_ctl"));
    }
}
