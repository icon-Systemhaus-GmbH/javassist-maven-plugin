/*
 * Copyright 2017 barthel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.icongmbh.oss.maven.plugin.javassist;

import static de.icongmbh.oss.maven.plugin.javassist.JavassistTransformerExecutor.STAMP_FIELD_NAME;
import static java.util.Arrays.asList;
import static javassist.CtClass.booleanType;
import static javassist.bytecode.AccessFlag.FINAL;
import static javassist.bytecode.AccessFlag.PRIVATE;
import static javassist.bytecode.AccessFlag.PUBLIC;
import static javassist.bytecode.AccessFlag.STATIC;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.easymock.Capture.newInstance;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.startsWith;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.build.IClassTransformer;
import javassist.bytecode.ClassFile;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests various methods with different focus on {@link JavassistTransformerExecutor}.
 */
public class TestJavassistTransformerExecuter extends JavassistTransformerExecutorTestBase {

  private ClassPool classPool;

  private JavassistTransformerExecutor sut;
  private final IClassTransformer transformer = createMock("mockTransformer",
                                                           IClassTransformer.class);

  @Before
  public void setUp_ClassPool() {
    classPool = mock("classPool", ClassPool.class);
  }

  @Before
  public void setUp_SubjectUnderTest() {
    sut = javassistTransformerExecutor(this.classPool);
  }

  @Test
  public void outputDirectory_null() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getOutputDirectory());
    sut.setOutputDirectory(null);
    assertNull(sut.getOutputDirectory());
  }

  @Test
  public void outputDirectory_empty() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getOutputDirectory());
    sut.setOutputDirectory("   ");
    assertEquals("   ", sut.getOutputDirectory());
  }

  @Test
  public void outputDirectory() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getOutputDirectory());
    sut.setOutputDirectory(transformedClassDirectory().getAbsolutePath());
    assertEquals(transformedClassDirectory().getAbsolutePath(), sut.getOutputDirectory());
  }

  @Test
  public void inputDirectory_null() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getInputDirectory());
    sut.setInputDirectory(null);
    assertNull(sut.getInputDirectory());
  }

  @Test
  public void inputDirectory_empty() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getInputDirectory());
    sut.setInputDirectory("   ");
    assertEquals("   ", sut.getInputDirectory());
  }

  @Test
  public void inputDirectory() {
    sut = new JavassistTransformerExecutor();
    assertNull(sut.getInputDirectory());
    sut.setInputDirectory(classDirectory().getAbsolutePath());
    assertEquals(classDirectory().getAbsolutePath(), sut.getInputDirectory());
  }

  @Test
  public void evaluateOutputDirectory_throws_NullPointer_if_both_directories_are_null() {
    assertThrows(NullPointerException.class, () -> sut.evaluateOutputDirectory(null, null));
  }

  @Test
  public void evaluateOutputDirectory_returns_inputDir_if_null() {
    assertEquals(classDirectory().getAbsolutePath(),
                 sut.evaluateOutputDirectory(null, classDirectory().getAbsolutePath()));
  }

  @Test
  public void evaluateOutputDirectory_returns_inputDir_if_empty() {
    assertEquals(classDirectory().getAbsolutePath(),
                 sut.evaluateOutputDirectory("   ", classDirectory().getAbsolutePath()));
  }

  @Test
  public void evaluateOutputDirectory() {
    assertEquals(transformedClassDirectory().getAbsolutePath(),
                 sut.evaluateOutputDirectory(transformedClassDirectory().getAbsolutePath(),
                                             classDirectory().getAbsolutePath()));
  }

  @Test
  public void buildClassPool() {
    assertNotNull(sut.buildClassPool());
  }

  @Test
  public void configureClassPool_throws_NullPointer_if_classPool_is_null() {
    assertThrows(NullPointerException.class, () -> sut.configureClassPool(null, classDirectory().getAbsolutePath()));
  }

  @Test
  public void configureClassPool() throws NotFoundException {
    // given
    final ClassPath classPath = mock("classPath", ClassPath.class);

    expect(classPool.appendClassPath(eq(classDirectory().getAbsolutePath()))).andReturn(classPath);
    expect(classPool.appendClassPath(anyObject(ClassPath.class))).andReturn(classPath);
    expect(classPool.appendSystemPath()).andReturn(classPath);

    replay(classPool);
    // when
    assertSame(classPool,
               sut.configureClassPool(classPool, classDirectory().getAbsolutePath()));

    // then
    verify(classPool);
  }

  @Test
  public void iterateClassnames_throws_NullPointerException_if_directory_is_null() {
    assertThrows(NullPointerException.class, () -> sut.iterateClassnames(null));
  }

  @Test
  public void iterateClassnames_should_return_two_of_three_files() throws IOException {
    // given
    final List<String> expectedClassNames = asList(withInnerClass()); // 2x *.class, 1x
    // *.java
    assertEquals("3 classes in classes directory",
                 3,
                 listFiles(classDirectory(), null, true).size());

    // when
    final Iterator<String> classnames = sut.iterateClassnames(classDirectory().getAbsolutePath());

    // then
    assertNotNull(classnames);
    while (classnames.hasNext()) {
      assertThat("iterated class name is expected", expectedClassNames, hasItem(classnames.next()));
    }
  }

  @Test
  public void hasStamp_returns_false_if_NotFoundException_is_thrown() throws NotFoundException {
    // given
    final CtClass ctClass = mock("ctClass", CtClass.class);
    final NotFoundException internalException = new NotFoundException("expected exception");

    expect(ctClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andThrow(internalException);
    replay(ctClass);
    // when
    assertFalse(sut.hasStamp(transformer, ctClass));

    // then
    verify(ctClass);
  }

  @Test
  public void hasStamp_returns_false_if_getDeclaredField_returns_null() throws NotFoundException {
    // given
    final CtClass ctClass = mock("ctClass", CtClass.class);

    expect(ctClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andReturn(null);
    replay(ctClass);
    // when
    assertFalse(sut.hasStamp(transformer, ctClass));

    // then
    verify(ctClass);
  }

  @Test
  public void hasStamp_returns_true_if_getDeclaredField_returns_not_null() throws NotFoundException {
    // given
    final CtClass ctClass = mock("ctClass", CtClass.class);

    expect(ctClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andReturn(mock(CtField.class));
    replay(ctClass);
    // when
    assertTrue(sut.hasStamp(transformer, ctClass));

    // then
    verify(ctClass);
  }

  @Test
  public void applyStamp_throws_NullPointer_if_candidateClass_is_null() {
    // given

    // when
    assertThrows(NullPointerException.class, () -> sut.applyStamp(transformer, null));

    // then
  }

  @Test
  public void applyStamp_on_Interface() throws CannotCompileException, NotFoundException {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = mock("candidateClass", CtClass.class);

    final Capture<CtField> fieldCaptures = newInstance();

    // createStampField
    expect(candidateClass.isInterface()).andReturn(true);
    expect(candidateClass.getClassPool()).andReturn(classPool).anyTimes();
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();

    candidateClass.addField(capture(fieldCaptures), anyObject(CtField.Initializer.class));

    replay(candidateClass, classPool);
    // when
    sut.applyStamp(transformer, candidateClass);

    // then
    verify(candidateClass, classPool);
    assertThat("generated stamp field starts with the constant prefix.",
               fieldCaptures.getValue().getName(),
               org.hamcrest.core.StringStartsWith
                 .startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME));
    assertEquals("generated stamp field must have the right modifiers.",
                 STATIC | FINAL | PUBLIC,
                 fieldCaptures.getValue().getModifiers());
    assertEquals("generated stamp field is a boolean.",
                 booleanType,
                 fieldCaptures.getValue().getType());
  }

  @Test
  public void applyStamp_on_Class() throws CannotCompileException, NotFoundException {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = mock("candidateClass", CtClass.class);

    final Capture<CtField> fieldCaptures = newInstance();

    // createStampField
    expect(candidateClass.isInterface()).andReturn(false);
    expect(candidateClass.getClassPool()).andReturn(classPool).anyTimes();
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();

    candidateClass.addField(capture(fieldCaptures), anyObject(CtField.Initializer.class));

    replay(candidateClass, classPool);
    // when
    sut.applyStamp(transformer, candidateClass);

    // then
    verify(candidateClass, classPool);
    assertThat("generated stamp field starts with the constant prefix.",
               fieldCaptures.getValue().getName(),
               org.hamcrest.core.StringStartsWith
                 .startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME));
    assertEquals("generated stamp field must have the right modifiers.",
                 STATIC | FINAL | PRIVATE,
                 fieldCaptures.getValue().getModifiers());
    assertEquals("generated stamp field is a boolean.",
                 booleanType,
                 fieldCaptures.getValue().getType());
  }

  @Test
  public void removeStamp_ignores_internal_NotFoundException() throws CannotCompileException,
                                                                      NotFoundException {
    // given
    final String className = "test.TestClass";
    final NotFoundException internalException = new NotFoundException("expected exception");
    final CtClass candidateClass = mock("candidateClass", CtClass.class);

    // createStampField
    expect(candidateClass.isInterface()).andReturn(false);
    expect(candidateClass.getClassPool()).andReturn(classPool).anyTimes();
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();

    final Capture<CtField> fieldCaptures = newInstance();

    candidateClass.removeField(capture(fieldCaptures));
    EasyMock.expectLastCall().andThrow(internalException);

    replay(candidateClass, classPool);
    // when
    sut.removeStamp(transformer, candidateClass);

    // then
    verify(candidateClass, classPool);
    assertThat("generated stamp field starts with the constant prefix.",
               fieldCaptures.getValue().getName(),
               org.hamcrest.core.StringStartsWith
                 .startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME));
    assertEquals("generated stamp field must have the right modifiers.",
                 STATIC | FINAL | PRIVATE,
                 fieldCaptures.getValue().getModifiers());
    assertEquals("generated stamp field is a boolean.",
                 booleanType,
                 fieldCaptures.getValue().getType());
  }

  @Test
  public void removeStamp_on_Class() throws CannotCompileException, NotFoundException {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = mock("candidateClass", CtClass.class);

    // createStampField
    expect(candidateClass.isInterface()).andReturn(false);
    expect(candidateClass.getClassPool()).andReturn(classPool).anyTimes();
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();

    final Capture<CtField> fieldCaptures = newInstance();

    candidateClass.removeField(capture(fieldCaptures));

    replay(candidateClass, classPool);
    // when
    sut.removeStamp(transformer, candidateClass);

    // then
    verify(candidateClass, classPool);
    assertThat("generated stamp field starts with the constant prefix.",
               fieldCaptures.getValue().getName(),
               org.hamcrest.core.StringStartsWith
                 .startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME));
    assertEquals("generated stamp field must have the right modifiers.",
                 STATIC | FINAL | PRIVATE,
                 fieldCaptures.getValue().getModifiers());
    assertEquals("generated stamp field is a boolean.",
                 booleanType,
                 fieldCaptures.getValue().getType());
  }

  @Test
  public void removeStamp_on_Interface() throws CannotCompileException, NotFoundException {
    // given
    final String className = "test.TestClass";
    final CtClass candidateClass = mock("candidateClass", CtClass.class);

    // createStampField
    expect(candidateClass.isInterface()).andReturn(true);
    expect(candidateClass.getClassPool()).andReturn(classPool).anyTimes();
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();

    final Capture<CtField> fieldCaptures = newInstance();

    candidateClass.removeField(capture(fieldCaptures));

    replay(candidateClass, classPool);
    // when
    sut.removeStamp(transformer, candidateClass);

    // then
    verify(candidateClass, classPool);
    assertThat("generated stamp field starts with the constant prefix.",
               fieldCaptures.getValue().getName(),
               org.hamcrest.core.StringStartsWith
                 .startsWith(JavassistTransformerExecutor.STAMP_FIELD_NAME));
    assertEquals("generated stamp field must have the right modifiers.",
                 STATIC | FINAL | PUBLIC,
                 fieldCaptures.getValue().getModifiers());
    assertEquals("generated stamp field is a boolean.",
                 booleanType,
                 fieldCaptures.getValue().getType());
  }
}
