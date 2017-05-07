
package de.icongmbh.oss.maven.plugin.javassist;

import static de.icongmbh.oss.maven.plugin.javassist.JavassistTransformerExecutor.STAMP_FIELD_NAME;

import javassist.build.IClassTransformer;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Iterator;
import javassist.ClassPool;
import javassist.CtClass;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.startsWith;

import java.io.IOException;
import javassist.CannotCompileException;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.build.JavassistBuildException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests
 * {@link JavassistTransformerExecutor#transform(IClassTransformer, String, String, Iterator) } with
 * nested classes.
 *
 * <p>
 * Github Issue:
 * <a href="https://github.com/icon-Systemhaus-GmbH/javassist-maven-plugin/issues/48">#48</a>
 * <p>
 *
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 *
 * @throws Exception
 */
public class TestJavassistTransformerExecutor_transform_with_nested_classes
        extends JavassistTransformerExecutorTestBase {

  private ClassPool classPool;

  private IClassTransformer classTransformer;

  private JavassistTransformerExecutor sut;

  @Before
  public void setUp_ClassPool() throws NotFoundException {
    classPool = configureClassPool(mock("classPool", ClassPool.class));
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
  public void throws_NullPointerException_if_nestedClasses_returns_null() throws Exception {
    // given
    expectedExceptionRule.expect(NullPointerException.class);

    final String className = oneTestClass();

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = configureCandidateClassFoTransformation(className,
                                                                           stampedClassWithNestedClasses(className,
                                                                                                         (CtClass[])null));

    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    executeTransform(classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void throws_NullPointerException_if_nestedClasses_returns_array_with_null() throws Exception {
    // given
    expectedExceptionRule.expect(NullPointerException.class);

    final String className = oneTestClass();

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = configureCandidateClassFoTransformation(className,
                                                                           stampedClassWithNestedClasses(className,
                                                                                                         (CtClass)null));

    replay(candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    executeTransform(classNames);

    // then
    verify(candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void do_nothing_if_nestedClass_is_not_modified() throws Exception {
    // given
    final String className = oneTestClass();
    final CtClass nestedClass = mock("nestedClass", CtClass.class);
    expect(nestedClass.isModified()).andReturn(false);

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = configureCandidateClassFoTransformation(className,
                                                                           stampedClassWithNestedClasses(className,
                                                                                                         nestedClass));

    replay(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    executeTransform(classNames);

    // then
    verify(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void do_nothing_if_nestedClass_is_stamped() throws Exception {
    // given
    final String className = oneTestClass();

    final CtClass nestedClass = mock("nestedClass", CtClass.class);
    expect(nestedClass.isModified()).andReturn(true);
    expect(nestedClass.getDeclaredField(startsWith(STAMP_FIELD_NAME)))
      .andReturn(mock("nestedClass_stampField", CtField.class));

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = configureCandidateClassFoTransformation(className,
                                                                           stampedClassWithNestedClasses(className,
                                                                                                         nestedClass));

    replay(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    executeTransform(classNames);

    // then
    verify(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);
  }

  @Test
  public void transform_and_stamp_nested_class() throws Exception {
    // given
    final String className = oneTestClass();
    final String nestedClassClassName = className + "$NestedClass";

    final CtClass nestedClass = stampedClass(nestedClassClassName,
                                             initializeClass(mock("nestedClass", CtClass.class)));
    expect(this.classPool.get(nestedClassClassName)).andReturn(nestedClass);
    expect(nestedClass.isModified()).andReturn(true);
    nestedClass.writeFile(eq(transformedClassDirectory().getAbsolutePath()));

    final Iterator<String> classNames = classNames(className);

    final CtClass candidateClass = configureCandidateClassFoTransformation(className,
                                                                           stampedClassWithNestedClasses(className,
                                                                                                         nestedClass));

    replay(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);

    // when
    executeTransform(classNames);

    // then
    verify(nestedClass, candidateClass, classNames, this.classPool, this.classTransformer);
  }

  private CtClass configureCandidateClassFoTransformation(final String className,
                                                          final CtClass candidateClass) throws JavassistBuildException,
                                                                                        NotFoundException {
    this.classPool.importPackage(className);
    expectLastCall();
    expect(this.classPool.get(className)).andReturn(candidateClass);

    expect(this.classTransformer.shouldTransform(same(candidateClass))).andReturn(true);
    this.classTransformer.applyTransformations(same(candidateClass));
    expectLastCall();
    return candidateClass;
  }

  private void executeTransform(final Iterator<String> classNames) {
    sut.transform(this.classTransformer,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);
  }

  private CtClass stampedClassWithNestedClasses(final String className,
                                                final CtClass ... nestedClasses) throws CannotCompileException,
                                                                                 NotFoundException,
                                                                                 IOException {
    final CtClass candidateClass = super.stampedClass(className,
                                                      initializeClass(mock("candidateClass",
                                                                           CtClass.class)));
    candidateClass.writeFile(eq(transformedClassDirectory().getAbsolutePath()));
    expect(candidateClass.getNestedClasses()).andReturn(nestedClasses);
    return candidateClass;
  }

}
