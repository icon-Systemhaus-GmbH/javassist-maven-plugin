package com.github.drochetti.javassist.maven;

import static org.junit.Assert.*;

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
    public void testSimpleConfig() throws Exception {
        File pom = new File( "src/test/resources/project1/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        JavassistMojo mojo = (JavassistMojo) rule.lookupMojo( "javassist", pom );
        assertNotNull( mojo );
        
        assertNotNull( mojo.isSkip() );
        assertFalse( mojo.isSkip() );

        assertNotNull( mojo.getIncludeTestClasses() );
        assertFalse( mojo.getIncludeTestClasses() );
        
        assertNotNull( mojo.getTransformerClasses() );
        assertEquals( 1, mojo.getTransformerClasses().length );
        assertNotNull( mojo.getTransformerClasses()[0] );
        assertEquals( SampleTransformer.class.getName(), mojo.getTransformerClasses()[0].getClassName() );
        mojo.execute();
    }
    
    @Test
    public void testOverrideBuildDirAndTestDir() throws Exception {
        File pom = new File( "src/test/resources/project2/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        JavassistMojo mojo = (JavassistMojo) rule.lookupMojo( "javassist", pom );
        assertNotNull( mojo );
        
        assertNotNull( mojo.isSkip() );
        assertFalse( mojo.isSkip() );

        assertNotNull( mojo.getIncludeTestClasses() );
        assertFalse( mojo.getIncludeTestClasses() );
        
        assertNotNull( mojo.getTransformerClasses() );
        assertEquals( 1, mojo.getTransformerClasses().length );
        assertNotNull( mojo.getTransformerClasses()[0] );
        assertEquals( SampleTransformer.class.getName(), mojo.getTransformerClasses()[0].getClassName() );
        
        assertNotNull( mojo.getBuildDir() );
        assertEquals( "bin/classes",mojo.getBuildDir() );
        
        assertNotNull( mojo.getTestBuildDir() );
        assertEquals( "bin/test-classes",mojo.getTestBuildDir() );
        
        mojo.execute();
    }

    @Test
    public void testSkip() throws Exception {
        File pom = new File( "src/test/resources/project3/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        JavassistMojo mojo = (JavassistMojo) rule.lookupMojo( "javassist", pom );
        assertNotNull( mojo );
        
        assertNotNull( mojo.isSkip() );
        assertTrue( mojo.isSkip() );
        mojo.execute();
    }
}
