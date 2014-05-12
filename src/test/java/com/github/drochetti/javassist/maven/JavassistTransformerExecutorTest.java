package com.github.drochetti.javassist.maven;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

import javassist.CtClass;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class JavassistTransformerExecutorTest {

    @SuppressWarnings("resource")
    @Before
    public void setUp() throws Exception {
        // Prepare source somehow.
        String source = "package test; public class Test { static { System.out.println(\"hello\"); } public Test() { System.out.println(\"world\"); } }";

        // Save source in .java file.
        File root = new File("tmp"); // On Windows running on C:\, this is C:\java.
        File sourceFile = new File(root, "test/Test.java");
        sourceFile.getParentFile().mkdirs();
        new FileWriter(sourceFile).append(source).close();

        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());
    }
    
    @After
    public void tearDown() throws IOException {
        File root = new File("tmp");
        FileUtils.deleteDirectory(root);
    }

    @Test
    public void test() throws Exception {
        //given
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

        ClassTransformer mockTransformer = EasyMock.createMock(ClassTransformer.class);
        mockTransformer.transform(EasyMock.anyString(), EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();
        EasyMock.replay(mockTransformer);

        executor.setTransformerClasses(mockTransformer);

        //when
        executor.execute();

        //then
        EasyMock.verify( mockTransformer );
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testWithRealClass() throws Exception {
        //given
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

        Method methodFilter = ClassTransformer.class.getDeclaredMethod("filter", CtClass.class); 
        Method methodApplyTransformation = ClassTransformer.class.getDeclaredMethod("applyTransformations", CtClass.class);
        
        Capture<CtClass> capturedClass1 = new Capture<CtClass>();
        Capture<CtClass> capturedClass2 = new Capture<CtClass>();

        ClassTransformer mockTransformer = EasyMock.createMock(ClassTransformer.class, methodFilter, methodApplyTransformation);
        EasyMock.expect(mockTransformer.filter(EasyMock.capture(capturedClass1))).andReturn(true);
        EasyMock.expectLastCall();
        mockTransformer.applyTransformations(EasyMock.capture(capturedClass2));
        EasyMock.expectLastCall();
        EasyMock.replay(mockTransformer);

        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setAdditionalClassPath(root.toURL());
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());

        //when
        executor.execute();

        //then
        EasyMock.verify( mockTransformer );
        assertEquals("test.Test", capturedClass1.getValue().getName());
        assertEquals("test.Test", capturedClass2.getValue().getName());
    }

}
