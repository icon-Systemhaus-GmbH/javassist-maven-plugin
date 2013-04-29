package com.github.drochetti.javassist.maven;

import static java.lang.Thread.currentThread;

import java.net.URL;
import java.net.URLClassLoader;

import javassist.ClassPool;
import javassist.LoaderClassPath;

public class JavassistTransformerExecutor {

	private URL[] additionalClassPath;
	private ClassTransformer[] transformerInstances;
	private String outputDirectory;

	public void setAdditionalClassPath(final URL... additionalClassPath) {
		this.additionalClassPath = additionalClassPath;
	}

	public void setTransformerClasses(
			final ClassTransformer... transformerInstances) {
		this.transformerInstances = transformerInstances;
	}

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void execute() throws Exception {
		loadAdditionalClassPath(additionalClassPath);
		for (final ClassTransformer transformer : transformerInstances) {
			transformer.transform(outputDirectory);
		}
	}

	private void loadAdditionalClassPath(final URL... urls) {
		final ClassLoader contextClassLoader = currentThread()
				.getContextClassLoader();
		final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(
				urls, contextClassLoader);
		ClassPool.getDefault().insertClassPath(
				new LoaderClassPath(pluginClassLoader));
		currentThread().setContextClassLoader(pluginClassLoader);
	}

}
