package com.github.drochetti.javassist.maven.it.project1;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import com.github.drochetti.javassist.maven.example.transformer.MethodCallClassTransformer;

public class Integration1Test {
    
    @org.junit.Test
    public void testClassesHaveBeenTransformed() throws NoSuchFieldException, SecurityException {
        Class<?> c = com.github.drochetti.javassist.maven.it.project1.Test.class;
        assertNotNull(c);
        
        Field f = c.getDeclaredField(MethodCallClassTransformer.ALREADY_INTROSPECTED_FIELD_NAME);
        assertNotNull(f);

    }

}
