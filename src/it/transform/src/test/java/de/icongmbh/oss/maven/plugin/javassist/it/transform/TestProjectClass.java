package de.icongmbh.oss.maven.plugin.javassist.it.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test class is for use in integration tests only.
 *
 * @since 1.2.0
 */
public class TestProjectClass {

  @org.junit.Test
  public void classes_have_been_transformed() throws NoSuchFieldException, SecurityException {

    assertNotNull(ProjectClass.class
      .getDeclaredField(de.icongmbh.oss.maven.plugin.javassist.example.transformer.MethodCallClassTransformer.ALREADY_INTROSPECTED_FIELD_NAME));
  }

  @org.junit.Test
  public void method_modified() throws NoSuchFieldException, SecurityException {

    assertEquals("Modified by Javassist.", new ProjectClass().getName());
  }

}
