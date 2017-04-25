package de.icongmbh.oss.maven.plugin.javassist.it.project1;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import de.icongmbh.oss.maven.plugin.javassist.example.transformer.MethodCallClassTransformer;

public class Integration1Test
{

  @org.junit.Test
  public void testClassesHaveBeenTransformed() throws NoSuchFieldException, SecurityException
  {
    Class<?> c = de.icongmbh.oss.maven.plugin.javassist.it.project1.Test.class;
    assertNotNull( c );

    Field f = c.getDeclaredField( MethodCallClassTransformer.ALREADY_INTROSPECTED_FIELD_NAME );
    assertNotNull( f );

  }

}
