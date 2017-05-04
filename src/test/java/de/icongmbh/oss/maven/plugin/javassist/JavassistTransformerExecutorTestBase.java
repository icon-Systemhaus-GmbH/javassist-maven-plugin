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
import static java.lang.System.err;
import static java.lang.System.out;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.startsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javax.tools.JavaCompiler;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public abstract class JavassistTransformerExecutorTestBase {

  @Rule
  public final ExpectedException expectedExceptionRule = none();

  @Rule
  public final TemporaryFolder temporaryFolderRule = new TemporaryFolder();

  private JavassistTransformerExecutor javassistTransformerExecutor;

  private File classDirectory;

  private File transformedClassDirectory;

  @Before
  public void setUp_directories() throws IOException {
    classDirectory = temporaryFolderRule.newFolder("classes");
    transformedClassDirectory = temporaryFolderRule.newFolder("transformed-classes");
  }

  protected JavassistTransformerExecutor javassistTransformerExecutor() {
    return javassistTransformerExecutor(null);
  }

  protected JavassistTransformerExecutor javassistTransformerExecutor(final ClassPool classPool) {
    if (null == classPool) {
      javassistTransformerExecutor = new JavassistTransformerExecutor();
    } else {
      javassistTransformerExecutor = new JavassistTransformerExecutor(){
        @Override
        protected ClassPool buildClassPool() {
          return classPool;
        }
      };
    }
    javassistTransformerExecutor.setInputDirectory(classDirectory.getAbsolutePath());
    javassistTransformerExecutor.setOutputDirectory(transformedClassDirectory.getAbsolutePath());
    return javassistTransformerExecutor;
  }

  protected File classDirectory() {
    return classDirectory;
  }

  protected File transformedClassDirectory() {
    return transformedClassDirectory;
  }

  @SuppressWarnings("unchecked")
  protected Iterator<String> classNames(final String className) {
    final Iterator<String> classNames = mock("classNames", Iterator.class);
    expect(classNames.hasNext()).andReturn(true).times(2);
    expect(classNames.hasNext()).andReturn(false);
    expect(classNames.next()).andReturn(className);
    return classNames;
  }

  protected ClassPool configureClassPool(final ClassPool classPool) throws NotFoundException {
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

  protected CtClass initializeClass() throws NotFoundException {
    return initializeClass(mock("candidateClass", CtClass.class));
  }

  protected CtClass initializeClass(final CtClass candidateClass) throws NotFoundException {
    expect(candidateClass.getClassFile2()).andReturn(null);
    expect(candidateClass.subtypeOf(anyObject(CtClass.class))).andReturn(true);
    return candidateClass;
  }

  protected CtClass stampedClass(final String className) throws CannotCompileException,
                                                                  NotFoundException {
    final CtClass candidateClass = initializeClass(mock("candidateClass", CtClass.class));
    expect(candidateClass.getDeclaredField(startsWith(STAMP_FIELD_NAME))).andReturn(null);
    // real stamping
    expect(candidateClass.isInterface()).andReturn(false);
    expect(candidateClass.getClassFile2()).andReturn(new ClassFile(false, className, null));
    expect(candidateClass.isFrozen()).andReturn(false);
    expect(candidateClass.getName()).andReturn(className).anyTimes();
    candidateClass.addField(anyObject(CtField.class), anyObject(CtField.Initializer.class));
    return candidateClass;
  }

  @SuppressWarnings("resource")
  protected String oneTestClass() throws IOException {
    final String packageName = "test";
    final String className = "OneTest";
    final StringBuilder source = new StringBuilder().append("package ").append(packageName)
      .append(";").append("public class ").append(className).append(" { ").append("  static { ")
      .append("    System.out.println(\"hello\"); ").append("  }").append("  public ")
      .append(className).append("() { ").append("    System.out.println(\"world\"); ")
      .append("  } ").append("}");

    compileSourceFiles(writeSourceFile(className, source.toString()));
    // source file, compiled class
    assertThat("2 classes in classes directory",
               FileUtils.listFiles(classDirectory(), null, true).size(),
               is(2));
    return packageName + '.' + className;
  }

  @SuppressWarnings("resource")
  protected String[] withInnerClass() throws IOException {
    final String packageName = "test";
    final String className = "WithInnerTest";
    final String innerClassName = "InnerClass";
    final StringBuilder source = new StringBuilder().append("package ").append(packageName)
      .append(";").append("public class ").append(className).append(" { ").append("  static { ")
      .append("    System.out.println(\"hello\"); ").append("  }").append("  public ")
      .append(className).append("() { ").append("    System.out.println(\"world\"); ")
      .append("  } ").append("  class ").append(innerClassName).append(" {").append("  }")
      .append("}");

    compileSourceFiles(writeSourceFile(className, source.toString()));
    // source file, compiled class and compiled inner class
    assertThat("3 classes in classes directory",
               FileUtils.listFiles(classDirectory(), null, true).size(),
               is(3));
    return new String[] {packageName + '.' + className,
                         packageName + '.' + className + '$' + innerClassName};
  }

  private void compileSourceFiles(File ... sourceFiles) {
    // Compile source files
    JavaCompiler compiler = getSystemJavaCompiler();
    for (File sourceFile : sourceFiles) {
      assertThat(compiler.run(null, out, err, sourceFile.getPath()), is(0));
    }
  }

  private File writeSourceFile(final String className, final String sources) throws IOException {
    // Save source in .java file.
    final File sourceFile = new File(classDirectory(), "test/" + className + ".java");
    assertThat("create sorce directory incl. package structure",
               sourceFile.getParentFile().mkdirs(),
               is(true));
    new FileWriter(sourceFile).append(sources).close();
    return sourceFile;
  }

}
