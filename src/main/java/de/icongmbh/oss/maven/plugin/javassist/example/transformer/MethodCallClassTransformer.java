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
package de.icongmbh.oss.maven.plugin.javassist.example.transformer;

import java.util.Properties;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtField.Initializer;
import javassist.NotFoundException;
import javassist.build.JavassistBuildException;
import javassist.bytecode.AccessFlag;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;

/**
 * Example of {@link ClassTransformer} implementation.
 *
 */
public class MethodCallClassTransformer extends ClassTransformer {

    public static final char METHOD_TOKEN = '#';
    public static final char JAVASSIST_STATEMENT_START_TOKEN = '{';
    public static final char JAVASSIST_STATEMENT_END_TOKEN = '}';

    public static final String ALREADY_INTROSPECTED_FIELD_NAME = "__introspected__"
            + MethodCallClassTransformer.class.getSimpleName();
    private Properties properties;

    /**
     * <p>
     * {@link Properties} entries like:
     * </p>
     * <ul>
     * <li>name: full qualified class name and method name separated by '
     * {@value #METHOD_TOKEN}'
     * <li>value: javassist statement - starts with '
     * {@value #JAVASSIST_STATEMENT_START_TOKEN}' and ends with '
     * {@value #JAVASSIST_STATEMENT_END_TOKEN}'
     * </ul>
     * e.g.
     *
     * <pre>
     * my.example.App#doSomthing={ $2="injected value for sec. parameter"; $_ = $proceed($$); }
     * </pre>
     *
     * @param properties maybe {@code null}
     * @throws Exception provided by interface
     */
    // TODO: validate input; don't store properties directly (outside
    // modification)
    @Override
    public void configure(final Properties properties) throws Exception {
        this.properties = null == properties ? new Properties() : properties;
    }

    @Override
    public boolean shouldTransform(final CtClass candidateClass) throws JavassistBuildException {
        return candidateClass != null && !isIntrospected(candidateClass);
    }

    @Override
    public void applyTransformations(final CtClass classToTransform) throws JavassistBuildException {
        if (null == classToTransform) {
            return;
        }
        try {
            classToTransform.instrument(new ExprEditor() {
                @Override
                public void edit(final MethodCall m) throws CannotCompileException {
                    final String statement = getStatement(m.getClassName(),
                            m.getMethodName());
                    if (statement != null) {
                        try {
                            m.replace(statement);
                        } catch (final CannotCompileException e) {
                            throw new CannotCompileException(String.format(
                                    "Compile statement '%1$s' FAILED with: %2$s",
                                    statement, e.getMessage()), e);
                        }
                    }
                }
            });
            // insert internal introspection state field
            final CtField introspectedField = new CtField(CtClass.booleanType,
                    ALREADY_INTROSPECTED_FIELD_NAME, classToTransform);
            introspectedField.setModifiers(AccessFlag.PUBLIC | AccessFlag.STATIC
                    | AccessFlag.FINAL);
            classToTransform
                    .addField(introspectedField, Initializer.constant(true));
        } catch (CannotCompileException e) {
            throw new JavassistBuildException(e);
        }
    }

    private boolean isIntrospected(final CtClass candidateClass) {
        try {
            candidateClass.getField(ALREADY_INTROSPECTED_FIELD_NAME);
            return true;
        } catch (final NotFoundException e) {
            return false;
        }
    }

    // TODO: find better implementation
    private String getStatement(final String className, final String methodName) {
        if (null == properties || (null == className && null == methodName)) {
            return null;
        }
        String statement = this.properties.getProperty(className + METHOD_TOKEN
                + methodName);
        if (null == statement) {
            statement = this.properties.getProperty(className + METHOD_TOKEN);
        }
        return null == statement ? this.properties.getProperty(METHOD_TOKEN
                + methodName) : statement;
    }
}
