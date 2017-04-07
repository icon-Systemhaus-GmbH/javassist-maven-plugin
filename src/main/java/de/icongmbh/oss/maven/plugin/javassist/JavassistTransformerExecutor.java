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
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

/**
 * Executer to perform the transformation by a list of {@link ClassTransformer}
 * instances.
 * 
 */
public class JavassistTransformerExecutor {

    private static final String STAMP_FIELD_NAME = "__TRANSFORMED_BY_JAVASSIST_MAVEN_PLUGIN__";

	private IClassTransformer[] transformerInstances;
	private String inputDirectory;
	private String outputDirectory;

    private static Logger logger = LoggerFactory.getLogger(JavassistTransformerExecutor.class);

	public JavassistTransformerExecutor() {
	}

	/**
	 * Configure class transformer instances for use with this executor
	 * 
	 * @param transformerInstances must not be {@code null}
	 */
	public void setTransformerClasses(
			final IClassTransformer... transformerInstances) {
		this.transformerInstances = transformerInstances;
	}

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	protected String getOutputDirectory() {
		return outputDirectory;
	}

	public void setInputDirectory(final String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	protected String getInputDirectory() {
		return inputDirectory;
	}

	public void execute() throws Exception {
		for (final IClassTransformer transformer : transformerInstances) {
			execute(transformer);
		}
	}

	protected void execute(final IClassTransformer transformer) {
		if (null == transformer) {
			return;
		}
		transform(transformer, getInputDirectory(), getOutputDirectory());
	}
	
    /**
     * Search for class files on the passed directory name ({@link #iterateClassnames(String)})
     * and apply transformation to each one ({@link #transform(IClassTransformer, String, String, Iterator)}).
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param transformer the transformer that will apply transformations.
     * @param dir root directory -input and output directory are the same.
     * @see #iterateClassnames(String)
     * @see #transform(IClassTransformer, String, String, Iterator)
     * 
     */
    public final void transform(final IClassTransformer transformer, final String dir) {
        if( null == dir || dir.trim().isEmpty()) {
            return;
        }
        transform(transformer, dir,dir,iterateClassnames(dir));
    }

    /**
     * Search for class files on the passed input directory ({@link #iterateClassnames(String)})
     * and apply transformation to each one ({@link #transform(IClassTransformer, String, String, Iterator)}).
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param transformer the transformer that will apply transformations.
     * @param inputDir root directory - maybe {@code null} - if {@code null} or empty nothing will be transformed. 
     * @param outputDir maybe {@code null} - if {@code null} or empty the {@code inputDir} will be used.
     * @see #iterateClassnames(String)
     * @see #transform(IClassTransformer, String, String, Iterator)
     */
    public void transform(final IClassTransformer transformer, final String inputDir, final String outputDir) {
        if( null == inputDir || inputDir.trim().isEmpty()) {
            return;
        }
        final String outDirectory = outputDir != null && !outputDir.trim().isEmpty() ? outputDir:inputDir;
        transform(transformer, inputDir, outDirectory,iterateClassnames(inputDir));
    }

    /**
     * Transform each class passed via {@link Iterator} of class names.
     * <p>
     * Use the passed {@code className} iterator, load each one as {@link CtClass}, filter
     * the valid candidates and apply transformation to each one.
     * </p>
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param transformer must not be {@code null}
     * @param inputDir root directory - maybe {@code null} - if {@code null} or empty nothing will be transformed 
     * @param outputDir must be not {@code null}
     * @param classNames maybe {@code null} - if {@code null} or empty nothing will be transformed
     * @see #initializeClass(CtClass)
     * @see IClassTransformer#shouldTransform(CtClass)
     * @see IClassTransformer#applyTransformations(CtClass)
     */
    public final void transform(IClassTransformer transformer, final String inputDir,final String outputDir, final Iterator<String> classNames) {
        if( null == classNames || !classNames.hasNext()) {
            return;
        }
        try {
            // create new classpool for transform; don't blow up the default
            final ClassPool classPool = new ClassPool(ClassPool.getDefault());
            classPool.childFirstLookup = true;
            classPool.appendClassPath(inputDir);
            classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            classPool.appendSystemPath();
            debugClassLoader(classPool);
            int i = 0;
            while (classNames.hasNext()) {
                final String className = classNames.next();
                if( null == className) {
                    continue;
                }
                try {
                    logger.debug("Got class name {}", className);
                    classPool.importPackage(className);
                    final CtClass candidateClass = classPool.get(className);
                    initializeClass(candidateClass);
                    if ( !hasStamp(candidateClass) && transformer.shouldTransform(candidateClass) ) {
                    	transformer.applyTransformations(candidateClass);
                        applyStamp(candidateClass);
                        candidateClass.writeFile(outputDir);
			for (CtClass nestedClass : candidateClass.getNestedClasses()) {
				if (nestedClass.isModified) {
					applyStamp(nestedClass);
					nestedClass.writeFile(outputDir);
				}
			}
			logger.debug("Class {} instrumented by {}", className, getClass().getName());
                        ++i;
                    }
                } catch (final NotFoundException e) {
                    logger.warn("Class {} could not be resolved due to dependencies not found on " +
                            "current classpath (usually your class depends on \"provided\" scoped dependencies).",
                            className);
                    continue;
                } catch ( final Exception ex) { // EOFException ...
                    logger.error("Class {} could not be instrumented due to initialize FAILED.",className, ex);
                    continue;
                }
            }
            logger.info("#{} classes instrumented by {}",i,getClass().getName());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected Iterator<String> iterateClassnames(final String dir) {
        final String[] extensions = { ".class" };
        final File directory = new File(dir);
        IOFileFilter fileFilter = new SuffixFileFilter(extensions);
        final IOFileFilter dirFilter = TrueFileFilter.INSTANCE;
        return ClassnameExtractor.iterateClassnames(directory, FileUtils.iterateFiles(directory, fileFilter, dirFilter));
    }

    /**
     * Apply a "stamp" to a class to indicate it has been modified.
     * <p>
     * By default, this method uses a boolean field named 
     * {@value #STAMP_FIELD_NAME} as the stamp. 
     * Any class overriding this method should also override {@link #hasStamp(CtClass)}.
     * </p>
     * @param candidateClass the class to mark/stamp.
     * @throws CannotCompileException by {@link CtClass#addField(CtField, CtField.Initializer)}
     * @see #createStampField(CtClass)
     * @see CtClass#addField(CtField, CtField.Initializer)
     */
    protected void applyStamp(CtClass candidateClass) throws CannotCompileException {
        candidateClass.addField(createStampField(candidateClass),Initializer.constant(true));
    }

    /**
     * Remove a "stamp" from a class if the "stamp" field is available.
     * <p>
     * By default, this method removes a boolean field named {@value #STAMP_FIELD_NAME}. 
     * Any class overriding this method should also override {@link #hasStamp(CtClass)}.
     * </p>
     * @param candidateClass the class to remove the mark/stamp from.
     * @throws CannotCompileException by {@link CtClass#removeField(CtField)}
     * @see #createStampField(CtClass)
     * @see CtClass#removeField(CtField)
     */
    protected void removeStamp(CtClass candidateClass) throws CannotCompileException {
        try {
            candidateClass.removeField(createStampField(candidateClass));
        } catch (final NotFoundException e) {
            // ignore; 
        }
    }

    /**
     * Indicates whether a class holds a stamp or not.
     * <p>
     * By default, this method uses a boolean field named 
     * {@value #STAMP_FIELD_NAME} as the stamp. 
     * Any class overriding this method should also override {@link #applyStamp(CtClass)}.
     * </p>
     * @param candidateClass the class to check must not be {@code null}.
     * @return {@code true} if the class owns the stamp, otherwise {@code false}.
     * @see #createStampFieldName()
     * @see CtClass#getDeclaredField(String)
     */
    protected boolean hasStamp(CtClass candidateClass) {
    	boolean hasStamp = false;
        try {
            hasStamp = null != candidateClass.getDeclaredField(createStampFieldName());
        } catch (NotFoundException e) {
            hasStamp = false;
        }
        if( logger.isDebugEnabled() ) {
            logger.debug("Stamp {}{} found in class {}", createStampFieldName(),(hasStamp?"":" NOT"),candidateClass.getName());
        }
        return hasStamp;
    }
    
    private String createStampFieldName() {
        return STAMP_FIELD_NAME+getClass().getName().replaceAll("\\W", "_");
    }

    private CtField createStampField(CtClass candidateClass) throws CannotCompileException {
        int stampModifiers = AccessFlag.STATIC | AccessFlag.FINAL;
        if (!candidateClass.isInterface()) {
          stampModifiers |= AccessFlag.PRIVATE;
        } else {
          stampModifiers |= AccessFlag.PUBLIC;
        }
        final CtField stampField = new CtField(CtClass.booleanType, createStampFieldName(),candidateClass);
        stampField.setModifiers(stampModifiers);
        return stampField;
    }

    private void initializeClass(final CtClass candidateClass) throws NotFoundException {
        debugClassFile(candidateClass.getClassFile2());
        // TODO hack to initialize class to avoid further NotFoundException (what's the right way of doing this?)
        candidateClass.subtypeOf(ClassPool.getDefault().get(Object.class.getName()));
    }

    private void debugClassFile(final ClassFile classFile) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug(" - class: {}",classFile.getName());
        logger.debug(" -- Java version: {}.{}", classFile.getMajorVersion(), classFile.getMinorVersion());
        logger.debug(" -- interface: {} abstract: {} final: {}", classFile.isInterface(), classFile.isAbstract(), classFile.isFinal());
        logger.debug(" -- extends class: {}",classFile.getSuperclass());
        logger.debug(" -- implements interfaces: {}", Arrays.deepToString(classFile.getInterfaces()));
    }

    private void debugClassLoader(final ClassPool classPool) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug(" - classPool: {}", classPool.toString());
        ClassLoader classLoader = classPool.getClassLoader();
        while (classLoader != null) {
            logger.debug(" -- {}: {}", classLoader.getClass().getName(), classLoader.toString());
            if (classLoader instanceof URLClassLoader) {
                logger.debug(" --- urls: {}", Arrays.deepToString(((URLClassLoader) classLoader).getURLs()));
            }
            classLoader = classLoader.getParent();
        }
    }

}
