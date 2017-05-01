
package de.icongmbh.oss.maven.plugin.javassist;

import static org.junit.Assert.assertEquals;

import javassist.CtClass;
import javassist.build.IClassTransformer;

import org.easymock.Capture;

import static org.easymock.Capture.newInstance;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Tests the main steps in transformation work flow in
 * {@link JavassistTransformerExecutor#execute() } .
 *
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 *
 * @throws Exception
 */
public class TestJavassistTransformerExecutor_TransformerClasses_handling
        extends JavassistTransformerExecutorTestBase {

  @Test
  public void throw_NullPointerException_if_set_null_for_TransformerClasses_array() throws Exception {
    // given
    expectedExceptionRule.expect(NullPointerException.class);

    final JavassistTransformerExecutor sut = javassistTransformerExecutor();

    // when
    sut.setTransformerClasses((IClassTransformer[])null);

    // then

  }

  @Test
  public void not_throw_any_Exception_with_empty_TransformerClasses_array() throws Exception {
    // given
    withInnerClass();
    assertThat("transformed class directory is empty before transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));
    final JavassistTransformerExecutor sut = javassistTransformerExecutor();

    // when
    sut.setTransformerClasses(new IClassTransformer[0]);
    sut.execute();

    // then
    assertThat("transformed class directory is empty after transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));

  }

  @Test
  public void not_throw_any_Exception_with_null_element_in_TransformerClasses_array() throws Exception {
    // given
    withInnerClass();
    assertThat("transformed class directory is empty before transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));
    final JavassistTransformerExecutor sut = javassistTransformerExecutor();

    // when
    sut.setTransformerClasses(new IClassTransformer[] {null});
    sut.execute();

    // then
    assertThat("transformed class directory is empty after transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));

  }

  @Test
  public void not_throw_any_Exception_without_TransformerClasses() throws Exception {
    // given
    withInnerClass();
    assertThat("transformed class directory is empty before transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));
    final JavassistTransformerExecutor sut = javassistTransformerExecutor();

    // when
    sut.execute();

    // then
    assertThat("transformed class directory is empty after transformation",
               FileUtils.listFiles(transformedClassDirectory(), null, true).size(),
               is(0));

  }

  @Test
  public void applyTransformation_if_shouldTransform_returns_true_and_class_has_no_stamp() throws Exception {
    // given
    final String className = oneTestClass();
    final Capture<CtClass> classCapture_shouldTransform = newInstance();
    final Capture<CtClass> classCapture_applyTransformations = newInstance();

    final IClassTransformer mockTransformer = createMock("mockTransformer",
                                                         IClassTransformer.class);
    expect(mockTransformer.shouldTransform(capture(classCapture_shouldTransform))).andReturn(true);
    mockTransformer.applyTransformations(capture(classCapture_applyTransformations));
    expectLastCall().times(1);

    replay(mockTransformer);

    final JavassistTransformerExecutor sut = javassistTransformerExecutor();
    sut.setTransformerClasses(mockTransformer);

    // when
    sut.execute();

    // then
    verify(mockTransformer);
    assertEquals(className, classCapture_shouldTransform.getValue().getName());
    assertEquals(className, classCapture_applyTransformations.getValue().getName());
  }

  @Test
  public void not_applyTransformation_if_shouldTransform_returns_false() throws Exception {
    // given
    final String className = oneTestClass();
    final Capture<CtClass> classCapture_shouldTransform = newInstance();

    final IClassTransformer mockTransformer = createMock("mockTransformer",
                                                         IClassTransformer.class);
    expect(mockTransformer.shouldTransform(capture(classCapture_shouldTransform))).andReturn(false);
    replay(mockTransformer);

    final JavassistTransformerExecutor sut = javassistTransformerExecutor();
    sut.setTransformerClasses(mockTransformer);

    // when
    sut.execute();

    // then
    verify(mockTransformer);
    assertEquals(className, classCapture_shouldTransform.getValue().getName());
  }

}
