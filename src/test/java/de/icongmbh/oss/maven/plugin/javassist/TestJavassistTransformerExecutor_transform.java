
package de.icongmbh.oss.maven.plugin.javassist;

import static de.icongmbh.oss.maven.plugin.javassist.JavassistTransformerExecutor.STAMP_FIELD_NAME;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToNice;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.startsWith;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.Iterator;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the main steps in {@link JavassistTransformerExecutor#transform(IClassTransformer, String, String, Iterator)
 * }.
 * <p>
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 */
public class TestJavassistTransformerExecutor_transform
  extends JavassistTransformerExecutorTestBase {

  private ClassPool classPool;

  private IClassTransformer classTransformer;

  private JavassistTransformerExecutor sut;

  @Before
  public void setUp_ClassPool() {
    classPool = mock("classPool", ClassPool.class);
  }

  @Before
  public void setUp_ClassTransformer() {
    classTransformer = mock("classTransformer", IClassTransformer.class);
  }

  @Before
  public void setUp_SubjectUnderTest() {
    sut = javassistTransformerExecutor(this.classPool);
  }

  @Test
  public void do_nothing_if_transformer_is_null() {
    // given
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(null,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);

  }

  @Test
  public void do_nothing_if_inputDir_is_null() {
    // given
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  null,
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);

  }

  @Test
  public void do_nothing_if_inputDir_is_empty() {
    // given
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  "   ",
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);

  }

  @Test
  public void do_nothing_if_classNames_is_null() {
    // given
    replay(this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  null);

    // then
    verify(this.classPool, this.classTransformer);
  }

  @Test
  public void do_nothing_if_classNames_not_hasNext() {
    // given
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    expect(classNames.hasNext()).andReturn(false);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void do_nothing_if_classNames_hasNext_but_next_returns_null() {
    // given
    resetToNice(this.classPool);
    final Iterator<String> classNames = classNames(null);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void append_required_classPathes_on_classPool() throws Exception {
    // given
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    expect(classNames.hasNext()).andReturn(true);
    expect(classNames.hasNext()).andReturn(false); // bypass execution in while-loop
    configureClassPool(this.classPool);

    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void throw_RuntimeException_if_internal_NotFoundException_was_catched() throws Exception {
    // given
    final NotFoundException internalException = new NotFoundException("expected exception");

    expectedExceptionRule.expect(RuntimeException.class);
    expectedExceptionRule.expectMessage("expected exception");
    expectedExceptionRule.expectCause(is(internalException));
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    expect(classNames.hasNext()).andReturn(true);

    expect(this.classPool.appendClassPath(eq(classDirectory().getAbsolutePath())))
      .andThrow(internalException);

    replay(classNames, this.classPool, this.classTransformer);

    try {
      // when
      sut.transform(this.classTransformer,
                    classDirectory().getAbsolutePath(),
                    transformedClassDirectory().getAbsolutePath(),
                    classNames);
    } finally {
      // then
      verify(classNames, this.classPool, this.classTransformer);
    }
  }

  @Test
  public void continue_if_class_could_not_found_in_classPool_and_throws_NotFoundException() throws Exception {
    // given
    final String className = "test.TestClass";
    final NotFoundException internalException = new NotFoundException("expected exception");

    final Iterator<String> classNames = classNames(className);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andThrow(internalException);
    replay(classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void not_applyTransformation_if_hasStamp_returns_true() throws Exception {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = initializeClass();
    expect(candidateClass.getDeclaredField(startsWith(STAMP_FIELD_NAME)))
      .andReturn(mock("stampField", CtField.class));

    final Iterator<String> classNames = classNames(className);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);
    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void not_applyTransformation_if_shouldTransform_returns_false() throws Exception {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = initializeClass();
    expect(candidateClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andReturn(null);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(false);

    final Iterator<String> classNames = classNames(className);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);
    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void transform_stamp_and_write_class() throws Exception {
    // given
    final String className = oneTestClass();

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = stampedClass(className);
    candidateClass.writeFile(eq(transformedClassDirectory().getAbsolutePath()));

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void continue_transform_with_next_class_if_internal_JavassistBuildException_was_catched_on_applyTransformations() throws Exception {
    // given
    final String className = oneTestClass();
    final JavassistBuildException internalException = new JavassistBuildException("expected exception");

    final Iterator<String> classNames = classNames(className);

    CtClass candidateClass = initializeClass();
    expect(candidateClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andReturn(null);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall().andThrow(internalException);

    replay(classNames, candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);
    // then
    verify(classNames, candidateClass, this.classPool, this.classTransformer);
  }

  @Test
  public void continue_transform_with_next_class_if_internal_IOException_was_catched_on_writeFile() throws Exception {
    // given
    final String className = oneTestClass();
    final IOException internalException = new IOException("expected exception");

    final Iterator<String> classNames = classNames(className);

    CtClass candidateClass = stampedClass(className);

    candidateClass.writeFile(eq(transformedClassDirectory().getAbsolutePath()));
    expectLastCall().andThrow(internalException);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(classNames, candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);
    // then
    verify(classNames, candidateClass, this.classPool, this.classTransformer);
  }

  @Test
  public void continue_transform_with_next_class_if_internal_CannotCompileException_was_catched_on_writeFile() throws Exception {
    // given
    final String className = oneTestClass();
    final CannotCompileException internalException = new CannotCompileException("expected exception");

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = stampedClass(className);

    candidateClass.writeFile(eq(transformedClassDirectory().getAbsolutePath()));
    expectLastCall().andThrow(internalException);

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(classNames, candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);
    // then
    verify(classNames, candidateClass, this.classPool, this.classTransformer);
  }

  @Test
  public void use_only_input_directory() throws Exception {
    // given
    final String className = oneTestClass();

    final CtClass candidateClass = stampedClass(className);
    // real stamping
    // use input if output is not set
    candidateClass.writeFile(eq(classDirectory().getAbsolutePath()));

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer, classDirectory().getAbsolutePath());

    // then
    verify(candidateClass, this.classPool, this.classTransformer);
  }

  @Test
  public void use_input_directory_if_output_directory_is_null() throws Exception {
    // given
    final String className = oneTestClass();

    final CtClass candidateClass = stampedClass(className);
    // use input if output is not set
    candidateClass.writeFile(eq(classDirectory().getAbsolutePath()));

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer, classDirectory().getAbsolutePath(), null);

    // then
    verify(candidateClass, this.classPool, this.classTransformer);
  }

  @Test
  public void use_input_directory_if_output_directory_is_empty() throws Exception {
    // given
    final String className = oneTestClass();

    final CtClass candidateClass = stampedClass(className);
    // use input if output is not set
    candidateClass.writeFile(eq(classDirectory().getAbsolutePath()));

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();

    replay(candidateClass, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer, classDirectory().getAbsolutePath(), "   ");

    // then
    verify(candidateClass, this.classPool, this.classTransformer);
  }

  private CtClass stampedClass(final String className) throws CannotCompileException,
                                                              NotFoundException {
    final CtClass candidateClass = super.stampedClass(className,
                                                      initializeClass(mock("candidateClass",
                                                                           CtClass.class)));
    expect(candidateClass.getNestedClasses()).andReturn(new CtClass[0]);
    return candidateClass;
  }

}
