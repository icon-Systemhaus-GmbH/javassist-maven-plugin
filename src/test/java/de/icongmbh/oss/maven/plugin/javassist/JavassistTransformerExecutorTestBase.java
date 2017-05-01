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

import static java.lang.System.err;
import static java.lang.System.out;
import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javassist.ClassPool;
import javassist.NotFoundException;
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
  public void setUp_subjectUnderTest() throws IOException {
    classDirectory = temporaryFolderRule.newFolder("classes");
    transformedClassDirectory = temporaryFolderRule.newFolder("transformed-classes");

    if (null == classPool()) {
      javassistTransformerExecutor = new JavassistTransformerExecutor();
    } else {
      javassistTransformerExecutor = new JavassistTransformerExecutor(){
        @Override
        protected ClassPool buildClassPool(final String inputDir) throws NotFoundException {
          return classPool();
        }
      };
    }
    javassistTransformerExecutor.setInputDirectory(classDirectory.getAbsolutePath());
    javassistTransformerExecutor.setOutputDirectory(transformedClassDirectory.getAbsolutePath());

  }

  protected JavassistTransformerExecutor javassistTransformerExecutor() {
    return javassistTransformerExecutor;
  }

  protected File classDirectory() {
    return classDirectory;
  }

  protected File transformedClassDirectory() {
    return transformedClassDirectory;
  }

  protected ClassPool classPool() {
    return null;
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
  protected String withInnerClass() throws IOException {
    final String packageName = "test";
    final String className = "WithInnerTest";
    final StringBuilder source = new StringBuilder().append("package ").append(packageName)
      .append(";").append("public class ").append(className).append(" { ").append("  static { ")
      .append("    System.out.println(\"hello\"); ").append("  }").append("  public ")
      .append(className).append("() { ").append("    System.out.println(\"world\"); ")
      .append("  } ").append("  class InnerClass {").append("  }").append("}");

    compileSourceFiles(writeSourceFile(className, source.toString()));
    // source file, compiled class and compiled inner class
    assertThat("3 classes in classes directory",
               FileUtils.listFiles(classDirectory(), null, true).size(),
               is(3));
    return packageName + '.' + className;
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
