
package de.icongmbh.oss.maven.plugin.javassist;

import javassist.build.IClassTransformer;

import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Iterator;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.resetToNice;
import static org.hamcrest.core.Is.is;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the main steps in
 * {@link JavassistTransformerExecutor#transform(IClassTransformer, String, String, Iterator) }.
 *
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 *
 * @throws Exception
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
  @SuppressWarnings("unchecked")
  public void do_nothing_if_transformer_is_null() throws Exception {
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
  @SuppressWarnings("unchecked")
  public void do_nothing_if_inputDir_is_null() throws Exception {
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
  @SuppressWarnings("unchecked")
  public void do_nothing_if_inputDir_is_empty() throws Exception {
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
  public void do_nothing_if_classNames_is_null() throws Exception {
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
  @SuppressWarnings("unchecked")
  public void do_nothing_if_classNames_not_hasNext() throws Exception {
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
  public void do_nothing_if_classNames_hasNext_but_next_returns_null() throws Exception {
    // given
    resetToNice(this.classPool);
    final Iterator<String> classNames = createAndConfigureClassNamesIteratorMock(null);
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
  @SuppressWarnings("unchecked")
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
    expect(classNames.hasNext()).andReturn(false); // bypass execution in while-loop

    expect(this.classPool.appendClassPath(eq(classDirectory().getAbsolutePath())))
      .andThrow(internalException);

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
  public void do_not_break_if_class_could_not_found_in_classPool_and_throws_NotFoundException() throws Exception {
    // given
    final String className = "test.TestClass";
    final NotFoundException internalException = new NotFoundException("expected exception");

    final Iterator<String> classNames = createAndConfigureClassNamesIteratorMock(className);

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
    final CtClass candidateClass = initializeClass(mock("candidateClass", CtClass.class));
    expect(candidateClass
      .getDeclaredField(EasyMock.startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME)))
        .andReturn(mock("stampField", CtField.class));

    final Iterator<String> classNames = createAndConfigureClassNamesIteratorMock(className);

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
    final CtClass candidateClass = initializeClass(mock("candidateClass", CtClass.class));
    expect(candidateClass
      .getDeclaredField(EasyMock.startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME)))
        .andReturn(null);

    expect(this.classTransformer.shouldTransform(EasyMock.same(candidateClass))).andReturn(false);

    final Iterator<String> classNames = createAndConfigureClassNamesIteratorMock(className);

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

    final Iterator<String> classNames = createAndConfigureClassNamesIteratorMock(className);

    final CtClass candidateClass = initializeClass(mock("candidateClass", CtClass.class));
    expect(candidateClass
      .getDeclaredField(EasyMock.startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME)))
        .andReturn(null);
    // real stamping
    expect(candidateClass.isInterface()).andReturn(false);
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();
    candidateClass.addField(EasyMock.anyObject(CtField.class),
                            EasyMock.anyObject(CtField.Initializer.class));
    candidateClass.writeFile(EasyMock.eq(transformedClassDirectory().getAbsolutePath()));

    configureClassPool(this.classPool).importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(EasyMock.same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(EasyMock.same(candidateClass));
    EasyMock.expectLastCall();

    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @SuppressWarnings("unchecked")
  protected Iterator<String> createAndConfigureClassNamesIteratorMock(final String className) {
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    expect(classNames.hasNext()).andReturn(true).times(2);
    expect(classNames.hasNext()).andReturn(false);
    expect(classNames.next()).andReturn(className);
    return classNames;
  }

  private ClassPool configureClassPool(final ClassPool classPool) throws NotFoundException {
    // actual class directory
    expect(classPool.appendClassPath(eq(classDirectory().getAbsolutePath()))).andReturn(null);
    // actual classloader
    expect(classPool.appendClassPath(isA(LoaderClassPath.class))).andReturn(null);
    // actual system classpath
    expect(classPool.appendSystemPath()).andReturn(null);
    expect(classPool.get(Object.class.getName())).andReturn(mock("Object_CtClass", CtClass.class))
      .anyTimes();
    return classPool;
  }

  private CtClass initializeClass(final CtClass candidateClass) throws NotFoundException {
    expect(candidateClass.getClassFile2()).andReturn(null);
    expect(candidateClass.subtypeOf(EasyMock.anyObject(CtClass.class))).andReturn(true);
    return candidateClass;
  }
}
