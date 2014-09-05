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

import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides methods for extract class name from file name.
 * 
 * @author Uwe Barthel
 */
public class ClassnameExtractor {

	private ClassnameExtractor() {
		// private constructor for utility class
	}

	/**
	 * Remove passed parent directory from passed file name and replace
	 * directory separator with dots.
	 * <p>
	 * e.g.:
	 * </p>
	 * <ul>
	 * <li>parentDirectory: <code>/tmp/my/parent/src/</code></li>
	 * <li>classFile: <code>/tmp/my/parent/src/foo/bar/MyApp.class</li>
	 * </ul>
	 * returns: <code>foo.bar.MyApp</code>
	 * 
	 * @param parentDirectory
	 * @param classFile
	 * @return class name extracted from file name or <code>null</code>
	 * @throws IOException
	 */
	public static String extractClassNameFromFile(final File parentDirectory,
			final File classFile) throws IOException {
		if (null == classFile) {
			return null;
		}
		final String qualifiedFileName = parentDirectory != null ? classFile
				.getCanonicalPath().substring(
						parentDirectory.getCanonicalPath().length() + 1)
				: classFile.getCanonicalPath();
		return removeExtension(qualifiedFileName.replace(File.separator, "."));
	}

	/**
	 * @param parentDirectory
	 * @param classFiles
	 * @return iterator of full qualified class names based on passed classFiles
	 * @see #iterateClassnames(File, Iterator)
	 */
	public static Iterator<String> iterateClassnames(
			final File parentDirectory, final File... classFiles) {
		return iterateClassnames(parentDirectory, Arrays.asList(classFiles)
				.iterator());
	}

	/**
	 * Wrapping passed iterator (as reference) of class file names and extract full qualified class name on
	 * {@link Iterator#next()}.
	 * <p>
	 * It is possible that {@link Iterator#hasNext()} returns <code>true</code>
	 * and {@link Iterator#next()} returns <code>null</code>.
	 * 
	 * @param parentDirectory
	 * @param classFiles
	 * @return iterator of full qualified class names based on passed classFiles
	 *         or <code>null</code>
	 * @see #extractClassNameFromFile(File, File)
	 */
	// DANGEROUS call by reference
	public static Iterator<String> iterateClassnames(
			final File parentDirectory, final Iterator<File> classFileIterator) {
		return new Iterator<String>() {

			// @Override
			public boolean hasNext() {
				return classFileIterator == null ? false : classFileIterator
						.hasNext();
			}

			// @Override
			public String next() {
				final File classFile = classFileIterator.next();
				try {
					// possible returns null
					return extractClassNameFromFile(parentDirectory, classFile);
				} catch (final IOException e) {
					throw new RuntimeException(e.getMessage());
				}
			}

			// @Override
			public void remove() {
				classFileIterator.remove();
			}
		};
	}

	/**
	 * Wrapping passed list (as reference) of class file names and extract full qualified class name on
	 * {@link Iterator#next()}.
	 * <p>
	 * It is possible that {@link Iterator#hasNext()} returns <code>true</code>
	 * and {@link Iterator#next()} returns <code>null</code>.
	 * 
	 * @param parentDirectory
	 * @param classFiles
	 * @return list of full qualified class names based on passed classFiles or
	 *         <code>null</code>
	 * @throws IOException
	 * @see {@link #extractClassNameFromFile(File, File)}
	 */
	// DANGEROUS call by reference
	public static List<String> listClassnames(final File parentDirectory,
			final List<File> classFileList) throws IOException {
		if (null == classFileList || classFileList.isEmpty()) {
			return Collections.emptyList();
		}
		final List<String> list = new ArrayList<String>(classFileList.size());
		for (final File file : classFileList) {
			list.add(extractClassNameFromFile(parentDirectory, file));
		}
		return list;
	}

	/**
	 * @param parentDirectory
	 * @param classFiles
	 * @return list of full qualified class names based on passed classFiles or
	 *         <code>null</code>
	 * @throws IOException
	 * @see {@link #extractClassNameFromFile(File, File)}
	 */
	public static List<String> listClassnames(final File parentDirectory,
			final String... classFileList) throws IOException {
		if (null == classFileList || classFileList.length <= 0) {
			return Collections.emptyList();
		}
		final List<String> list = new ArrayList<String>(classFileList.length);
		for (final String file : classFileList) {
			list.add(extractClassNameFromFile(parentDirectory, new File(
					parentDirectory, file)));
		}
		return list;
	}

}
