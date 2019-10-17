
package de.icongmbh.oss.maven.plugin.javassist;

import static org.easymock.Capture.newInstance;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javassist.CtClass;
import javassist.build.IClassTransformer;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the main steps in transformation work flow in {@link JavassistTransformerExecutor#execute()}.
 * <p>
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 */
public class TestJavassistTransformerExecutor_TransformerClasses_handling
  extends JavassistTransformerExecutorTestBase {

  private JavassistTransformerExecutor sut;

  @Before
  public void setUp_SubjectUnderTest() {
    sut = javassistTransformerExecutor();
  }

  @Test
  public void throw_NullPointerException_if_set_null_for_TransformerClasses_array() {
    // given

    // when
    assertThrows(NullPointerException.class, () -> sut.setTransformerClasses((IClassTransformer[]) null));

    // then

  }

  @Test
  public void not_throw_any_Exception_with_empty_TransformerClasses_array() throws Exception {
    // given
    withInnerClass();
    assertEquals("transformed class directory is empty before transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

    // when
    sut.setTransformerClasses();
    sut.execute();

    // then
    assertEquals("transformed class directory is empty after transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

  }

  @Test
  public void not_throw_any_Exception_with_null_element_in_TransformerClasses_array() throws Exception {
    // given
    withInnerClass();
    assertEquals("transformed class directory is empty before transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

    // when
    sut.setTransformerClasses(new IClassTransformer[] {null});
    sut.execute();

    // then
    assertEquals("transformed class directory is empty after transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

  }

  @Test
  public void not_throw_any_Exception_without_TransformerClasses() throws Exception {
    // given
    withInnerClass();
    assertEquals("transformed class directory is empty before transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

    // when
    sut.execute();

    // then
    assertEquals("transformed class directory is empty after transformation",
                 0,
                 FileUtils.listFiles(transformedClassDirectory(), null, true).size());

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

    sut.setTransformerClasses(mockTransformer);

    // when
    sut.execute();

    // then
    verify(mockTransformer);
    assertEquals(className, classCapture_shouldTransform.getValue().getName());
  }

}
