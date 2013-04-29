/*
 * Copyright 2012 http://github.com/drochetti/
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
package com.github.drochetti.javassist.maven;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.util.Iterator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for class transformation logic.
 * @author Daniel Rochetti
 */
public abstract class ClassTransformer {

	private static Logger logger = LoggerFactory.getLogger(ClassTransformer.class);

	/**
	 * <p>Concrete implementations must implement all transformations on this method.
	 * You can use Javassist API to add/remove/replace methods, attributes and more.
	 * Only classes approved by {@link #filter(CtClass)} are considered.</p>
	 *
	 * @param classToTransform The class to transform.
	 * @see #filter(CtClass)
	 * @throws Exception if any error occur during the transformation.
	 */
	protected abstract void applyTransformations(CtClass classToTransform) throws Exception;

	/**
	 * <p>Test if the given class is suitable for applying transformations or not.</p>
	 * <p>For example, if the class is a specific type:</p>
	 * <code><pre>
	 * CtClass myInterface = ClassPool.getDefault().get(MyInterface.class.getName());
	 * return candidateClass.subtypeOf(myInterface);
	 * </pre></code>
	 *
	 * @param candidateClass
	 * @return {@code true} if the Class should be transformed; {@code false} otherwise.
	 * @throws Exception
	 */
	protected boolean filter(CtClass candidateClass) throws Exception {
		return true;
	}

	/**
	 * <p>
	 * Search for class files on a directory, load each one as {@link CtClass}, filter
	 * the valid candidates (using {@link #filter(CtClass)}) and apply transformation to each one
	 * ({@link #applyTransformations(CtClass)}).
	 * </p>
	 * <p>
	 * <strong>Limitation:</strong> do not search inside .jar files yet.
	 * </p>
	 * @param dir root directory.
	 * @see #applyTransformations(CtClass)
	 */
	public final void transform(String dir) {
		try {
			final ClassPool classPool = ClassPool.getDefault();
			final String[] extensions = { "class" };
			Iterator<File> classFiles = iterateFiles(new File(dir), extensions, true);
			while (classFiles.hasNext()) {
				File classFile = classFiles.next();
				String qualifiedFileName = classFile.getCanonicalPath().substring(dir.length() + 1);
				String className = removeExtension(qualifiedFileName.replace(File.separator, "."));
				CtClass candidateClass = null;
				try {
					candidateClass = classPool.get(className);
					initializeClass(candidateClass);
				} catch (NotFoundException e) {
					logger.warn("Class {} could not not be resolved due to dependencies not found on " +
							"current classpath (usually your class depends on \"provided\" scoped dependencies).",
							className);
					continue;
				} catch ( Exception ex) { // EOFException ...
					logger.error("Class {} could not not be instrumented due to initialize FAILED.",className, ex);
					continue;
				}
				if (filter(candidateClass)) {
					applyTransformations(candidateClass);
					candidateClass.writeFile(dir);
					logger.info("Class {} instrumented by {}", className, getClass().getName());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void initializeClass(CtClass candidateClass) throws NotFoundException {
		// TODO hack to initialize class to avoid further NotFoundException (what's the right way of doing this?)
		candidateClass.subtypeOf(ClassPool.getDefault().get(Object.class.getName()));
	}

}
