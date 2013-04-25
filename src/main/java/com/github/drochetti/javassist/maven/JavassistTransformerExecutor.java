package com.github.drochetti.javassist.maven;

import static java.lang.Thread.currentThread;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javassist.ClassPool;
import javassist.LoaderClassPath;

public class JavassistTransformerExecutor {

	private List<URL> classPath;
	private ClassTransformer[] transformerInstances;
	private String outputDirectory;

	public void setClassPath(final List<URL> classPath) {
		this.classPath = classPath;
	}

	public void setTransformarClasses(
			final ClassTransformer... transformerInstances) {
		this.transformerInstances = transformerInstances;

	}

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void execute() throws Exception {
		loadClassPath(classPath.toArray(new URL[classPath.size()]));
		for (final ClassTransformer transformer : transformerInstances) {
			transformer.transform(outputDirectory);
		}
	}

	private void loadClassPath(final URL... urls) {
		final ClassLoader contextClassLoader = currentThread()
				.getContextClassLoader();
		final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(
				urls, contextClassLoader);
		ClassPool.getDefault().insertClassPath(
				new LoaderClassPath(pluginClassLoader));
		currentThread().setContextClassLoader(pluginClassLoader);
	}

}
