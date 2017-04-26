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

import java.util.Properties;

import javassist.build.IClassTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for class transformation logic.
 *
 * @since 1.1.0
 */
public abstract class ClassTransformer implements IClassTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassTransformer.class);

  /**
   * Configure this instance by passing {@link Properties}.
   *
   * @param properties maybe {@code null} or empty
   * @throws Exception if configuration failed.
   */
  public void configure(final Properties properties) throws Exception {
    //
  }

  /**
   * Returns the logger.
   * 
   * @return never {@code null}
   */
  protected static Logger getLogger() {
    return LOGGER;
  }

}
