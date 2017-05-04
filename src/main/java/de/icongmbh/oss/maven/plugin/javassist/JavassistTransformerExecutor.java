/*
 * Copyright 2013 https://github.com/barthel
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

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.CtField.Initializer;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

/**
 * Executor to perform the transformation by a list of {@link IClassTransformer} instances.
 *
 * @since 1.1.0
 */
public class JavassistTransformerExecutor {

  /**
   * Stamp field name prefix.
   */
  static final String STAMP_FIELD_NAME = "__TRANSFORMED_BY_JAVASSIST_MAVEN_PLUGIN__";

  private IClassTransformer[] transformerInstances = new IClassTransformer[0];

  private String inputDirectory;

  private String outputDirectory;

  private static final Logger LOGGER = LoggerFactory.getLogger(JavassistTransformerExecutor.class);

  public JavassistTransformerExecutor() {
    super();
  }

  /**
   * Configure class transformer instances for use with this executor.
   *
   * @param transformerInstances must not be {@code null}
   *
   * @throws NullPointerException if passed {@code transformerInstances} is {@code null}
   */
  public void setTransformerClasses(final IClassTransformer ... transformerInstances) {
    this.transformerInstances = transformerInstances.clone();
  }

  /**
   * Sets the output directory where the transformed classes will stored.
   * <p>
   * The configured input directory will used if this directory is {@code null} or empty.
   * </p>
   *
   * @param outputDirectory could be {@code null} or empty.
   *
   * @see #setInputDirectory(String)
   */
  public void setOutputDirectory(final String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  /**
   * Returns the output directory where the transformed classes will stored.
   *
   * @return maybe {@code null} or empty
   */
  protected String getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * Sets the input directory where the classes to transform will selected from.
   * <p>
   * Nothing will transformed if this directory is {@code null} or empty.
   * </p>
   *
   * @param inputDirectory could be {@code null} or empty.
   */
  public void setInputDirectory(final String inputDirectory) {
    this.inputDirectory = inputDirectory;
  }

  /**
   * Returns the input directory where the the classes to transform will selected from.
   *
   * @return maybe {@code null} or empty
   */
  protected String getInputDirectory() {
    return inputDirectory;
  }

  /**
   * Executes all configured {@link IClassTransformer}.
   *
   * @see #setTransformerClasses(IClassTransformer...)
   * @see #execute(IClassTransformer)
   */
  public void execute() {
    for (final IClassTransformer transformer : transformerInstances) {
      execute(transformer);
    }
  }

  /**
   * Execute transformation with passed {@link IClassTransformer}.
   *
   * <p>
   * This method uses {@link #getInputDirectory() } and {@link #getOutputDirectory() } and calls
   * {@link #transform(IClassTransformer, String, String)}.
   * </p>
   * <p>
   * If the passed {@code transformer} is {@code null} nothing will transformed.
   * </p>
   *
   * @param transformer the transformer that will apply transformations could be {@code null}.
   *
   * @see #getInputDirectory()
   * @see #getOutputDirectory()
   * @see #transform(IClassTransformer, String, String)
   */
  protected void execute(final IClassTransformer transformer) {
    transform(transformer, getInputDirectory(), getOutputDirectory());
  }

  /**
   * Search for class files on the passed directory name ({@link #iterateClassnames(String)}) and
   * apply transformation to each one (
   * {@link #transform(IClassTransformer, String, String, Iterator)}).
   *
   * <p>
   * <strong>Limitation:</strong> do not search inside .jar files.
   * </p>
   * <p>
   * If the passed {@code transformer} is {@code null} or the passed {@code directory} is
   * {@code null} or empty nothing will transformed.
   * </p>
   *
   * @param transformer the transformer that will apply transformations could be {@code null}.
   * @param directory could be {@code null} or empty. The input and output directory are the same.
   *
   * @see #iterateClassnames(String)
   * @see #transform(IClassTransformer, String, String, Iterator)
   *
   */
  public final void transform(final IClassTransformer transformer, final String directory) {
    transform(transformer, directory, directory, iterateClassnames(directory));
  }

  /**
   * Search for class files on the passed input directory ({@link #iterateClassnames(String)}) and
   * apply transformation to each one (
   * {@link #transform(IClassTransformer, String, String, Iterator)}).
   * <p>
   * <strong>Limitation:</strong> do not search inside .jar files.
   * </p>
   *
   * @param transformer The transformer that will apply transformations could be {@code null}.
   * @param inputDir The root directory where the classes to transform will selected from could
   *          be {@code null} or empty. If it is {@code null} or empty nothing will be transformed.
   * @param outputDir The output directory where the transformed classes will stored could be
   *          {@code null} or empty. If it is {@code null} or empty the {@code inputDir} will be
   *          used.
   *
   * @see #iterateClassnames(String)
   * @see #transform(IClassTransformer, String, String, Iterator)
   */
  public void transform(final IClassTransformer transformer,
                        final String inputDir,
                        final String outputDir) {
    transform(transformer, inputDir, outputDir, iterateClassnames(inputDir));
  }

  /**
   * Transform each class passed via {@link Iterator} of class names.
   * <p>
   * Use the passed {@code className} iterator, load each one as {@link CtClass}, filter the valid
   * candidates and apply transformation to each one.
   * </p>
   * <p>
   * <strong>Limitation:</strong> do not search inside .jar files.
   * </p>
   * <p>
   * Any unexpected (internal catched) {@link Exception} will be re-thrown in an
   * {@link RuntimeException}.
   * </p>
   *
   * @param transformer The transformer that will apply transformations could be {@code null}.
   * @param inputDir The root directory where the classes to transform will selected from could
   *          be {@code null} or empty. If it is {@code null} or empty nothing will be transformed.
   * @param outputDir The output directory where the transformed classes will stored could be
   *          {@code null} or empty. If it is {@code null} or empty the {@code inputDir} will be
   *          used.
   * @param classNames could be {@code null} or empty. If it is {@code null} or empty nothing will
   *          be transformed.
   *
   * @see #initializeClass(ClassPool, CtClass)
   * @see IClassTransformer#shouldTransform(CtClass)
   * @see IClassTransformer#applyTransformations(CtClass)
   */
  public final void transform(final IClassTransformer transformer,
                              final String inputDir,
                              final String outputDir,
                              final Iterator<String> classNames) {
    if (null == transformer) {
      return;
    }
    if (null == inputDir || inputDir.trim().isEmpty()) {
      return;
    }
    if (null == classNames || !classNames.hasNext()) {
      return;
    }
    final String inDirectory = inputDir.trim();
    try {
      final ClassPool classPool = configureClassPool(buildClassPool(), inDirectory);
      final String outDirectory = evaluateOutputDirectory(outputDir, inDirectory);
      int classCounter = 0;
      while (classNames.hasNext()) {
        final String className = classNames.next();
        if (null == className) {
          continue;
        }
        try {
          LOGGER.debug("Got class name {}", className);
          classPool.importPackage(className);
          final CtClass candidateClass = classPool.get(className);
          initializeClass(classPool, candidateClass);
          if (!hasStamp(candidateClass) && transformer.shouldTransform(candidateClass)) {
            transformer.applyTransformations(candidateClass);
            applyStamp(candidateClass);
            candidateClass.writeFile(outDirectory);
            LOGGER.debug("Class {} instrumented by {}", className, getClass().getName());
            ++classCounter;
          }
        } catch (final NotFoundException e) {
          LOGGER.warn("Class {} could not be resolved due to dependencies not found on "
                      + "current classpath (usually your class depends on \"provided\""
                      + " scoped dependencies).", className);
        } catch (final IOException ex) { // EOFException ...
          LOGGER.error("Class {} could not be instrumented due to initialize FAILED.",
                       className,
                       ex);
        } catch (final CannotCompileException ex) {
          LOGGER.error("Class {} could not be instrumented due to initialize FAILED.",
                       className,
                       ex);
        } catch (final JavassistBuildException ex) {
          LOGGER.error("Class {} could not be instrumented due to initialize FAILED.",
                       className,
                       ex);
        }
      }
      LOGGER.info("#{} classes instrumented by {}", classCounter, getClass().getName());
    } catch (final NotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Evaluates and returns the output directory.
   *
   * <p>
   * If the passed {@code outputDir} is {@code null} or empty, the passed {@code inputDir} otherwise
   * the {@code outputDir} will returned.
   *
   * @param outputDir could be {@code null} or empty
   * @param inputDir must not be {@code null}
   *
   * @return never {@code null}
   *
   * @throws NullPointerException if passed {@code inputDir} is {@code null}
   *
   * @since 1.2.0
   */
  protected String evaluateOutputDirectory(final String outputDir, final String inputDir) {
    return outputDir != null && !outputDir.trim().isEmpty() ? outputDir : inputDir.trim();
  }

  /**
   * Creates a new instance of a {@link ClassPool}.
   *
   * @return never {@code null}
   *
   * @since 1.2.0
   */
  protected ClassPool buildClassPool() {
    // create new classpool for transform; don't blow up the default
    return new ClassPool(ClassPool.getDefault());
  }

  /**
   * Configure the passed instance of a {@link ClassPool} and append required class pathes on it.
   *
   * @param classPool must not be {@code null}
   * @param inputDir must not be {@code null}
   *
   * @return never {@code null}
   *
   * @throws NotFoundException if passed {@code classPool} is {@code null} or if passed
   *           {@code inputDir} is a JAR or ZIP and not found.
   * @throws NullPointerException if passed {@code inputDir} is {@code null}
   *
   * @since 1.2.0
   */
  protected ClassPool configureClassPool(final ClassPool classPool,
                                         final String inputDir) throws NotFoundException {
    classPool.childFirstLookup = true;
    classPool.appendClassPath(inputDir);
    classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
    classPool.appendSystemPath();
    debugClassLoader(classPool);
    return classPool;
  }

  /**
   * Search for class files (file extension: {@code .class}) on the passed {@code directory}.
   * <p>
   * Note: The passed directory name must exist and readable.
   * </p>
   *
   * @param directory must nor be {@code null}
   * @return iterator of full qualified class names and never {@code null}
   *
   * @throws NullPointerException if passed {@code directory} is {@code null}.
   *
   * @see SuffixFileFilter
   * @see TrueFileFilter
   * @see FileUtils#iterateFiles(File, IOFileFilter, IOFileFilter)
   * @see ClassnameExtractor#iterateClassnames(File, Iterator)
   */
  protected Iterator<String> iterateClassnames(final String directory) {
    final File dir = new File(directory);
    final String[] extensions = {".class"};
    final IOFileFilter fileFilter = new SuffixFileFilter(extensions);
    final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
    return ClassnameExtractor.iterateClassnames(dir,
                                                FileUtils.iterateFiles(dir, fileFilter, dirFilter));
  }

  /**
   * Apply a "stamp" to a class to indicate it has been modified.
   * <p>
   * By default, this method uses a boolean field named {@value #STAMP_FIELD_NAME} as the stamp. Any
   * class overriding this method should also override {@link #hasStamp(CtClass)}.
   * </p>
   *
   * @param candidateClass the class to mark/stamp must not be {@code null}.
   *
   * @throws NullPointerException if passed {@code candidateClass} is {@code null}.
   * @throws CannotCompileException by {@link CtClass#addField(CtField, CtField.Initializer)}
   *
   * @see #createStampField(CtClass)
   * @see CtClass#addField(CtField, CtField.Initializer)
   */
  protected void applyStamp(CtClass candidateClass) throws CannotCompileException {
    candidateClass.addField(createStampField(candidateClass), Initializer.constant(true));
  }

  /**
   * Remove a "stamp" from a class if the "stamp" field is available.
   * <p>
   * By default, this method removes a boolean field named {@value #STAMP_FIELD_NAME}. Any class
   * overriding this method should also override {@link #hasStamp(CtClass)}.
   * </p>
   *
   * @param candidateClass the class to remove the mark/stamp from must not be {@code null}
   *
   * @throws NullPointerException if passed {@code candidateClass} is {@code null}.
   * @throws CannotCompileException by {@link CtClass#removeField(CtField)}
   *
   * @see #createStampField(CtClass)
   * @see CtClass#removeField(CtField)
   */
  protected void removeStamp(CtClass candidateClass) throws CannotCompileException {
    try {
      candidateClass.removeField(createStampField(candidateClass));
    } catch (final NotFoundException e) {
      // ignore; mission accomplished.
    }
  }

  /**
   * Indicates whether a class holds a stamp or not.
   * <p>
   * By default, this method uses a boolean field named {@value #STAMP_FIELD_NAME} as the stamp. Any
   * class overriding this method should also override {@link #applyStamp(CtClass)} and
   * {@link #removeStamp(CtClass) }.
   * </p>
   *
   * @param candidateClass the class to check must not be {@code null}.
   *
   * @return {@code true} if the class owns the stamp, otherwise {@code false}.
   *
   * @throws NullPointerException if passed {@code candidateClass} is {@code null}.
   * @see CtClass#getDeclaredField(String)
   */
  protected boolean hasStamp(CtClass candidateClass) {
    boolean hasStamp;
    try {
      hasStamp = null != candidateClass.getDeclaredField(createStampFieldName());
    } catch (NotFoundException e) {
      hasStamp = false;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stamp {}{} found in class {}",
                   createStampFieldName(),
                   (hasStamp ? "" : " NOT"),
                   candidateClass.getName());
    }
    return hasStamp;
  }

  /**
   * Creates the name of the stamp field.
   * <p>
   * This implementation appends {@value #STAMP_FIELD_NAME} with the full qualified class name and
   * replaces all non-word characters (like '.') with '_'.
   * </p>
   *
   * @return never {@code null} or empty.
   */
  private String createStampFieldName() {
    return STAMP_FIELD_NAME + getClass().getName().replaceAll("\\W", "_");
  }

  /**
   * Creates a {@link CtField} instance associated with the passed {@code candidateClass}.
   *
   * @param candidateClass must not be {@code null}
   *
   * @return never {@code null}
   *
   * @throws NullPointerException if passed {@code candidateClass} is {@code null}.
   * @throws CannotCompileException field could not created
   *
   * @see CtField
   */
  private CtField createStampField(final CtClass candidateClass) throws CannotCompileException {
    int stampModifiers = AccessFlag.STATIC | AccessFlag.FINAL;
    if (!candidateClass.isInterface()) {
      stampModifiers |= AccessFlag.PRIVATE;
    } else {
      stampModifiers |= AccessFlag.PUBLIC;
    }
    final CtField stampField = new CtField(CtClass.booleanType,
                                           createStampFieldName(),
                                           candidateClass);
    stampField.setModifiers(stampModifiers);
    return stampField;
  }

  private void initializeClass(final ClassPool classPool,
                               final CtClass candidateClass) throws NotFoundException {
    debugClassFile(candidateClass.getClassFile2());
    // TODO hack to initialize class to avoid further NotFoundException (what's the right way of
    // doing this?)
    candidateClass.subtypeOf(classPool.get(Object.class.getName()));
  }

  private void debugClassFile(final ClassFile classFile) {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug(" - class: {}", classFile.getName());
    LOGGER.debug(" -- Java version: {}.{}",
                 classFile.getMajorVersion(),
                 classFile.getMinorVersion());
    LOGGER.debug(" -- interface: {} abstract: {} final: {}",
                 classFile.isInterface(),
                 classFile.isAbstract(),
                 classFile.isFinal());
    LOGGER.debug(" -- extends class: {}", classFile.getSuperclass());
    LOGGER.debug(" -- implements interfaces: {}", Arrays.deepToString(classFile.getInterfaces()));
  }

  private void debugClassLoader(final ClassPool classPool) {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug(" - classPool: {}", classPool.toString());
    ClassLoader classLoader = classPool.getClassLoader();
    while (classLoader != null) {
      LOGGER.debug(" -- {}: {}", classLoader.getClass().getName(), classLoader.toString());
      if (classLoader instanceof URLClassLoader) {
        LOGGER.debug(" --- urls: {}", Arrays.deepToString(((URLClassLoader)classLoader).getURLs()));
      }
      classLoader = classLoader.getParent();
    }
  }

}
