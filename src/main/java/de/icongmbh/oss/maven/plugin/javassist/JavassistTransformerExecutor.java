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

import static java.lang.Thread.currentThread;

import java.io.File;
import java.net.URL;
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
 * @author Uwe Barthel
 */
public class JavassistTransformerExecutor {

    private static final String STAMP_FIELD_NAME = "__TRANSFORMED_BY_JAVASSIST_MAVEN_PLUGIN__";

	private URL[] additionalClassPath;
	private IClassTransformer[] transformerInstances;
	private String inputDirectory;
	private String outputDirectory;
	private ClassLoader originalContextClassLoader = null;
	private ClassLoader externalClassLoader;

    private static Logger logger = LoggerFactory.getLogger(JavassistTransformerExecutor.class);

	public JavassistTransformerExecutor() {
		this((ClassLoader) null);
	}

	/**
	 * Use passed class loader as context class loader before {@link #execute()}
	 * and reset afterwards.
	 * 
	 * @param classLoader
	 */
	public JavassistTransformerExecutor(final ClassLoader classLoader) {
		this.externalClassLoader = classLoader;
	}

	/**
	 * Passed {@link URL}s will be enriching the class path for transforming
	 * 
	 * @param additionalClassPath
	 */
	public void setAdditionalClassPath(final URL... additionalClassPath) {
		this.additionalClassPath = additionalClassPath;
	}

	/**
	 * Configure class transformer instances for use with this executor
	 * 
	 * @param transformerInstances
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
		originalContextClassLoader = currentThread().getContextClassLoader();
		if (externalClassLoader != null
				&& externalClassLoader != originalContextClassLoader) {
			currentThread().setContextClassLoader(externalClassLoader);
		}
		loadAdditionalClassPath(additionalClassPath);
		for (final IClassTransformer transformer : transformerInstances) {
			execute(transformer);
		}
		currentThread().setContextClassLoader(originalContextClassLoader);
	}

	protected void execute(final IClassTransformer transformer) {
		if (null == transformer) {
			return;
		}
		transform(transformer, getInputDirectory(), getOutputDirectory());
	}
	
    /**
     * <p>
     * Search for class files on the passed directory, load each one as {@link CtClass}, filter
     * the valid candidates (using {@link #filter(CtClass)}) and apply transformation to each one
     * ({@link #applyTransformations(CtClass)}).
     * </p>
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param transformer the transformer that will apply transformations.
     * @param dir root directory -input and output directory are the same.
     * @see #iterateClassnames(String)
     * @see #transform(Iterator, String)
     * @see #applyTransformations(CtClass)
     * 
     */
    public final void transform(final IClassTransformer transformer, final String dir) {
        if( null == dir || dir.trim().isEmpty()) {
            return;
        }
        transform(transformer, dir,dir,iterateClassnames(dir));
    }

    /**
     * <p>
     * Search for class files on the passed input directory, load each one as {@link CtClass}, filter
     * the valid candidates (using {@link #filter(CtClass)}) and apply transformation to each one
     * ({@link #applyTransformations(CtClass)}).
     * </p>
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param transformer the transformer that will apply transformations.
     * @param inputDir root directory - required - if <code>null</code> or empty nothing will be transformed. 
     * @param outputDir if <code>null</code> or empty the inputDir will be used.
     * @see #iterateClassnames(String)
     * @see #transform(Iterator, String)
     * @see #applyTransformations(CtClass)
     */
    public void transform(final IClassTransformer transformer, final String inputDir, final String outputDir) {
        if( null == inputDir || inputDir.trim().isEmpty()) {
            return;
        }
        final String outDirectory = outputDir != null && !outputDir.trim().isEmpty() ? outputDir:inputDir;
        transform(transformer, inputDir, outDirectory,iterateClassnames(inputDir));
    }

    /**
     * <p>
     * Use the passed className iterator, load each one as {@link CtClass}, filter
     * the valid candidates (using {@link #filter(CtClass)}) and apply transformation to each one
     * ({@link #applyTransformations(CtClass)}).
     * </p>
     * <p>
     * <strong>Limitation:</strong> do not search inside .jar files yet.
     * </p>
     * @param inputDir root directory - required - if <code>null</code> or empty nothing will be transformed 
     * @param outputDir must be not <code>null</code>
     * @see #applyTransformations(CtClass)
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

	private void loadAdditionalClassPath(final URL... urls) {
		if (null == urls || urls.length <= 0) {
			return;
		}
		final ClassLoader contextClassLoader = currentThread()
				.getContextClassLoader();
		final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(
				urls, contextClassLoader);
		ClassPool.getDefault().insertClassPath(
				new LoaderClassPath(pluginClassLoader));
		currentThread().setContextClassLoader(pluginClassLoader);
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
     * By default, this method uses a boolean field named 
     * {@value #STAMP_FIELD_NAME} as the stamp. 
     * Any class overriding this method should also override {@link #hasStamp(CtClass)}. 
     * @param candidateClass the class to mark/stamp.
     * @throws CannotCompileException 
     * @see {@link #hasStamp(CtClass)}
     */
    protected void applyStamp(CtClass candidateClass) throws CannotCompileException {
        candidateClass.addField(createStampField(candidateClass),Initializer.constant(true));
    }

    /**
     * Remove a "stamp" from a class if the "stamp" field is available.
     * By default, this method removes a boolean field named {@value #STAMP_FIELD_NAME}. 
     * Any class overriding this method should also override {@link #hasStamp(CtClass)}. 
     * @param candidateClass the class to remove the mark/stamp from.
     * @throws CannotCompileException 
     * @see {@link #hasStamp(CtClass)}
     * @see {@link #applyStamp(CtClass)}
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
     * By default, this method uses a boolean field named 
     * {@value #STAMP_FIELD_NAME} as the stamp. 
     * Any class overriding this method should also override {@link #applyStamp(CtClass)}.
     * @param candidateClass the class to check.
     * @return true if the class owns the stamp, otherwise false.
     * @see #applyStamp(CtClass)
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
