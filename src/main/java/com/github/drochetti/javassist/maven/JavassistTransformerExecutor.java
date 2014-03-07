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
package com.github.drochetti.javassist.maven;

import static java.lang.Thread.currentThread;

import java.net.URL;
import java.net.URLClassLoader;

import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * Executer to perform the transformation by a list of {@link ClassTransformer}
 * instances.
 * 
 * @author Uwe Barthel
 */
public class JavassistTransformerExecutor {

	private URL[] additionalClassPath;
	private ClassTransformer[] transformerInstances;
	private String inputDirectory;
	private String outputDirectory;
	private ClassLoader originalContextClassLoader = null;
	private ClassLoader externalClassLoader;

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
			final ClassTransformer... transformerInstances) {
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
		for (final ClassTransformer transformer : transformerInstances) {
			execute(transformer);
		}
		currentThread().setContextClassLoader(originalContextClassLoader);
	}

	protected void execute(final ClassTransformer transformer) {
		if (null == transformer) {
			return;
		}
		transformer.transform(getInputDirectory(), getOutputDirectory());
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

}
