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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.build.IClassTransformer;
/**
 * Base class for class transformation logic.
 * @author Daniel Rochetti
 */
public abstract class ClassTransformer implements IClassTransformer {
    
    private static Logger logger = LoggerFactory.getLogger(ClassTransformer.class);

    /**
     * <p>Configure this instance by passing {@link Properties}.</p>
     * @param properties maybe {@code null} or empty
     * @throws Exception if configuration failed.
     */
    public void configure(final Properties properties) throws Exception {
        return;
    }

    protected static Logger getLogger() {
        return logger;
    }
    

}
