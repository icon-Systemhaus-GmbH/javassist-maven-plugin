package com.github.drochetti.javassist.maven;

import static org.junit.Assert.*;

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
    public void test() {
        fail("Not yet implemented");
    }



}
