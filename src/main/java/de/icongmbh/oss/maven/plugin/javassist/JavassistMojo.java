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
package de.icongmbh.oss.maven.plugin.javassist;

import static java.lang.Thread.currentThread;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javassist.build.IClassTransformer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven plugin that will apply <a
 * href="http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/">Javassist</a>
 * class transformations on compiled classes (bytecode instrumentation).
 * <br/>
 * Example plugin configuration :
 * 
 * <pre>
 *   &lt;configuration&gt;
 *       &lt;skip&gt;true&lt;/skip&gt;
 *       &lt;includeTestClasses&gt;false&lt;/includeTestClasses&gt;
 *       &lt;buildDir&gt;bin/classes&lt;/buildDir&gt;
 *       &lt;testBuildDir&gt;bin/test-classes&lt;/testBuildDir&gt;
 *       &lt;transformerClasses&gt;
 *           &lt;transformerClass&gt;
 *               &lt;className&gt;de.icongmbh.oss.maven.plugin.javassist.SampleTransformer&lt;/className&gt;
 *           &lt;/transformerClass&gt;
 *       &lt;/transformerClasses&gt;
 *   &lt;/configuration&gt;
 * </pre> 
 * @author Daniel Rochetti
 * @author Uwe Barthel
 */
@Mojo(name = "javassist", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JavassistMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(JavassistMojo.class);

    private static final Class<IClassTransformer> TRANSFORMER_TYPE = IClassTransformer.class;

	@Parameter(defaultValue = "${project}", property = "javassist.project", required = true, readonly = true)
	private MavenProject project;

	/**Skips all processing performed by this goal.*/
	@Parameter(defaultValue = "false", property = "javassist.skip", required = false)
	private boolean skip;

	@Parameter(defaultValue = "true", property = "javassist.includeTestClasses", required = true)
	/**Whether or not to include test classes to be processed byt declared transformers.*/
	private Boolean includeTestClasses;

	@Parameter(property = "javassist.transformerClasses", required = true)
	private ClassTransformerConfiguration[] transformerClasses;

	/** Allows to customize the build directory of the project, used for both finding classes to transform and outputing them once transformed. 
	 * By default, equals to maven's project output directory. Path must be either absolute or relative to project base dir.*/
	@Parameter(property = "javassist.buildDir", required = false)
	private String buildDir;

	/** Allows to customize the build directory of the tests of the project, used for both finding classes to transform and outputing them once transformed. 
	 * By default, equals to maven's project test output directory. Path must be either absolute or relative to project base dir.*/
	@Parameter(property = "javassist.testBuildDir", required = false)
	private String testBuildDir;

	public void execute() throws MojoExecutionException {
		if( skip ) {
			logger.info("Skipping executing.");
			return;
		}

		final ClassLoader originalContextClassLoader =
			currentThread().getContextClassLoader();

		try {
			final List<URL> classPath = new ArrayList<URL>();

			for (final String runtimeResource : project
				.getRuntimeClasspathElements()) {
				classPath.add(resolveUrl(runtimeResource));
			}

			final String inputDirectory = buildDir == null ?
				project.getBuild().getOutputDirectory() : computeDir(buildDir);

			classPath.add(resolveUrl(inputDirectory));

			loadAdditionalClassPath(classPath);

			final JavassistTransformerExecutor executor =
				new JavassistTransformerExecutor();

			String testInputDirectory =
				testBuildDir == null ? project.getBuild()
					.getTestOutputDirectory() : computeDir(testBuildDir);

			executor.setTransformerClasses(instantiateTransformerClasses(
				currentThread().getContextClassLoader(), transformerClasses));
			executor.setInputDirectory(inputDirectory);
			executor.setOutputDirectory(inputDirectory);
			executor.execute();

			if (includeTestClasses) {
				classPath.add(resolveUrl(testInputDirectory));
				executor.setInputDirectory(testInputDirectory);
				executor.setOutputDirectory(testInputDirectory);
				executor.execute();
			}

		}
		catch (final Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
		finally {
			currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	private void loadAdditionalClassPath(final List<URL> classPath) {
		if (classPath.isEmpty()) {
			return;
		}
		final ClassLoader contextClassLoader =
			currentThread().getContextClassLoader();

		final URLClassLoader pluginClassLoader =
			URLClassLoader.newInstance(
				classPath.toArray(new URL[classPath.size()]),
				contextClassLoader);

		currentThread().setContextClassLoader(pluginClassLoader);
	}

    private String computeDir(String dir) {
        File dirFile = new File( dir );
        if( dirFile.isAbsolute() ) {
            return dirFile.getAbsolutePath();
        } else { 
            return new File(project.getBasedir(), buildDir).getAbsolutePath();
        }
    }

	/**
	 * @param contextClassLoader
	 * @param transformerClasses
	 * @return array of passed transformer class name instances
	 * @throws Exception 
	 * @see #instantiateTransformerClass(ClassLoader,
	 *      ClassTransformerConfiguration)
	 */
	protected IClassTransformer[] instantiateTransformerClasses(
			final ClassLoader contextClassLoader,
			final ClassTransformerConfiguration... transformerClasses)
			throws Exception {
		if (null == transformerClasses || transformerClasses.length <= 0) {
			throw new MojoExecutionException(
					"Invalid transformer classes passed");
		}
		final List<IClassTransformer> transformerInstances = new LinkedList<IClassTransformer>();
		for (ClassTransformerConfiguration transformerClass : transformerClasses) {
			final IClassTransformer transformerInstance = instantiateTransformerClass(
					contextClassLoader, transformerClass);
			configureTransformerInstance(transformerInstance,
					transformerClass.getProperties());
			transformerInstances.add(transformerInstance);
		}
		return transformerInstances
				.toArray(new IClassTransformer[transformerInstances.size()]);
	}

	/**
	 * Instantiate the class passed by {@link ClassTransformerConfiguration}
	 * configuration object.
	 * 
	 * @param contextClassLoader
	 * @param transformerClass
	 * @return new instance of passed transformer class name
	 * @throws ClassNotFoundException
	 * @throws NullPointerException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws MojoExecutionException
	 */
	protected IClassTransformer instantiateTransformerClass(
			final ClassLoader contextClassLoader,
			final ClassTransformerConfiguration transformerClass)
			throws ClassNotFoundException, NullPointerException,
			InstantiationException, IllegalAccessException,
			MojoExecutionException {
		if (null == transformerClass || null == transformerClass.getClassName()
				|| transformerClass.getClassName().trim().isEmpty()) {
			throw new MojoExecutionException(
					"Invalid transformer class name passed");
		}
		final Class<?> transformerClassInstance = Class.forName(
				transformerClass.getClassName().trim(), true,
				contextClassLoader);
		if (TRANSFORMER_TYPE.isAssignableFrom(transformerClassInstance)) {
			return TRANSFORMER_TYPE
					.cast(transformerClassInstance.newInstance());
		} else {
			throw new MojoExecutionException(
					"Transformer class must inherit from "
							+ TRANSFORMER_TYPE.getName());
		}
	}

	/**
	 * Configure the passed {@link ClassTransformer} instance using the passed
	 * {@link Properties}.
	 * 
	 * @param transformerInstance
	 *            - maybe <code>null</code>
	 * @param properties
	 *            - maybe <code>null</code> or empty
	 * @throws Exception 
	 */
	protected void configureTransformerInstance(
			final IClassTransformer transformerInstance,
			final Properties properties) throws Exception {
		if (null == transformerInstance || !(transformerInstance instanceof ClassTransformer)) {
			return;
		}
		((ClassTransformer)transformerInstance).configure(properties);
	}

	private URL resolveUrl(final String resource) {
		try {
			return new File(resource).toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public boolean isSkip() {
        return skip;
    }
	
	public Boolean getIncludeTestClasses() {
        return includeTestClasses;
    }
	
	public ClassTransformerConfiguration[] getTransformerClasses() {
        return transformerClasses;
    }
	
	public String getBuildDir() {
        return buildDir;
    }
	
	public String getTestBuildDir() {
        return testBuildDir;
    }

}
