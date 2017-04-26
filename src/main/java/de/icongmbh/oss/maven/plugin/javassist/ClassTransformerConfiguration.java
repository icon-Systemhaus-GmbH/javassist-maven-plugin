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

import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration object for {@link ClassTransformer} class.
 *
 * <pre>
 * {@code
 * 
 * <transformerClass>
 *  <className>com.domain.ToStringTransformer</className>
 *  <properties>
 *    <property>
 *      <name>append.value</name>
 *      <value> and ToStringTransformer</value>
 *    </property>
 *  </properties>
 * </transformerClass>
 * }
 * </pre>
 *
 * @since 1.1.0
 */
public class ClassTransformerConfiguration {

  @Parameter(property = "className", required = true)
  private String className;

  @Parameter(property = "properties", required = false)
  private Properties properties;

  /**
   * The transformer implementation full qualified class name.
   *
   * @return maybe {@code null}
   */
  public String getClassName() {
    return className;
  }

  /**
   * Sets the transformer implementation full qualified class name.
   *
   * @param className should not be {@code null}
   */
  public void setClassName(final String className) {
    this.className = className;
  }

  /**
   * Optional settings for the transformer class.
   *
   * @return never {@code null} but maybe empty.
   */
  public Properties getProperties() {
    return (null == properties) ? new Properties() : this.properties;
  }

  /**
   * Sets the optional settings for the transformer class.
   *
   * @param properties should not be {@code null}
   */
  public void setProperties(final Properties properties) {
    this.properties = (null == properties) ? new Properties() : (Properties)properties.clone();
  }
}
