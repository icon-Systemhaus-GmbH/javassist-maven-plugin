package com.github.drochetti.javassist.maven;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;


public class JavassistMojoTest {

    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() throws Throwable 
        {
        }

        @Override
        protected void after()
        {
        }
    };

    @Test
    public void test() throws Exception {
        File pom = new File( "src/test/resources/project1/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        JavassistMojo mojo = (JavassistMojo) rule.lookupMojo( "javassist", pom );
        assertNotNull( mojo );
        mojo.execute();
    }



}
