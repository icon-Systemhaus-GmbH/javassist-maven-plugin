package de.icongmbh.oss.maven.plugin.javassist;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtField.Initializer;
import javassist.build.IClassTransformer;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class JavassistTransformerExecutorTest {
    private final static File ROOT = new File("tmp");
    
    @SuppressWarnings("resource")
    private void createOneTestClass(File root) throws IOException {
        // Save source in .java file.
        String source = "package test; public class Test { static { System.out.println(\"hello\"); } public Test() { System.out.println(\"world\"); } }";
        // Prepare source somehow.
        File sourceFile = new File(root, "test/Test.java");
        sourceFile.getParentFile().mkdirs();
        new FileWriter(sourceFile).append(source).close();

        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());
    }

    @SuppressWarnings("resource")
    private void createOneTestClassWithInner(File root) throws IOException {
        // Save source in .java file.
        String source = "package test; public class Test { static { System.out.println(\"hello\"); } public Test() { System.out.println(\"world\"); } class TestInner { } }";
        // Prepare source somehow.
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
        createOneTestClass(ROOT);

        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

        IClassTransformer mockTransformer = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer.shouldTransform((CtClass) EasyMock.anyObject())).andReturn(true);
        mockTransformer.applyTransformations((CtClass) EasyMock.anyObject());
        EasyMock.expectLastCall();
        EasyMock.replay(mockTransformer);

        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());

        //when
        executor.execute();

        //then
        EasyMock.verify( mockTransformer );
    }
    
    @Test
    public void testWithRealClass() throws Exception {
        //given
        createOneTestClass(ROOT);

        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

        Capture<CtClass> capturedClass1 = new Capture<CtClass>();
        Capture<CtClass> capturedClass2 = new Capture<CtClass>();

        IClassTransformer mockTransformer = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer.shouldTransform(EasyMock.capture(capturedClass1))).andReturn(true);
        mockTransformer.applyTransformations(EasyMock.capture(capturedClass2));
        EasyMock.expectLastCall();
        EasyMock.replay(mockTransformer);

        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());

        //when
        executor.execute();

        //then
        EasyMock.verify( mockTransformer );
        assertEquals("test.Test", capturedClass1.getValue().getName());
        assertEquals("test.Test", capturedClass2.getValue().getName());
    }
    
    @Test
    public void testWithRealClassAndInnerClass() throws Exception {
        //given
        createOneTestClassWithInner(ROOT);
        
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();
        
        IClassTransformer mockTransformer = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer.shouldTransform((CtClass)EasyMock.anyObject())).andReturn(true);
        EasyMock.expectLastCall().times(2);
        mockTransformer.applyTransformations((CtClass)EasyMock.anyObject());
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(mockTransformer);
        
        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());
        
        //when
        executor.execute();
        
        //then
        EasyMock.verify( mockTransformer );
    }
    
    @Test
    public void testStamping() throws Exception {
        //given
        createOneTestClass(ROOT);
        
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();
        
        IClassTransformer mockTransformer = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer.shouldTransform((CtClass)EasyMock.anyObject())).andReturn(true);
        EasyMock.expectLastCall().times(1);
        mockTransformer.applyTransformations((CtClass)EasyMock.anyObject());
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(mockTransformer);
        
        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());
        
        //when
        //execute twice should not applyTransformations twice, nor should it shouldTransform it twice
        executor.execute();
        executor.execute();
        
        //then
        EasyMock.verify( mockTransformer );
    }
    
    @Test
    public void testStamping_with_2_different_transformers() throws Exception {
        //given
        createOneTestClass(ROOT);
        
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();
        
        IClassTransformer mockTransformer = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer.shouldTransform((CtClass)EasyMock.anyObject())).andReturn(true);
        EasyMock.expectLastCall().times(1);
        mockTransformer.applyTransformations((CtClass)EasyMock.anyObject());
        EasyMock.expectLastCall().times(1);
        
        IClassTransformer mockTransformer2 = EasyMock.createMock(IClassTransformer.class);
        EasyMock.expect(mockTransformer2.shouldTransform((CtClass)EasyMock.anyObject())).andReturn(true);
        EasyMock.expectLastCall().times(1);
        mockTransformer2.applyTransformations((CtClass)EasyMock.anyObject());
        EasyMock.expectLastCall().times(1);
        
        EasyMock.replay(mockTransformer);
        EasyMock.replay(mockTransformer2);
        
        executor.setTransformerClasses(mockTransformer);
        File root = new File("tmp");
        executor.setInputDirectory(root.getAbsolutePath());
        executor.setOutputDirectory(root.getAbsolutePath());
        
        executor.execute();
        
        //when
        executor.setTransformerClasses(mockTransformer, mockTransformer2);
        executor.execute();
        
        //then
        EasyMock.verify( mockTransformer );
    }
    
	@Test
	public void testModifierOnStampField_class() throws CannotCompileException {
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

		final ClassFile candidateClassFile = new ClassFile(false, _ClassStub.class.getName(), null);
		final CtClass candidateClassMock = EasyMock.createMock("candidateClassMock",CtClass.class);
		EasyMock.expect(candidateClassMock.getClassFile2()).andReturn(candidateClassFile).anyTimes();
		final Capture<CtField> fieldCapture = new Capture<CtField>();
		candidateClassMock.addField(EasyMock.capture(fieldCapture), EasyMock.anyObject(Initializer.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(candidateClassMock.isInterface()).andReturn(Boolean.FALSE);
		EasyMock.expect(candidateClassMock.isFrozen()).andReturn(Boolean.FALSE);
		EasyMock.expect(candidateClassMock.getName()).andReturn(_ClassStub.class.getName()).anyTimes();

		EasyMock.replay(candidateClassMock);
		executor.applyStamp(candidateClassMock);

		EasyMock.verify(candidateClassMock);
		Assert.assertThat(fieldCapture.getValue(), CoreMatchers.notNullValue());
		final CtField ctField = fieldCapture.getValue();
		Assert.assertThat(ctField.getModifiers(), CoreMatchers.equalTo(AccessFlag.PRIVATE | AccessFlag.STATIC | AccessFlag.FINAL));
	}

	@Test
	public void testModifierOnStampField_interface() throws CannotCompileException {
        JavassistTransformerExecutor executor = new JavassistTransformerExecutor();

		final ClassFile candidateClassFile = new ClassFile(true, _InterfaceStub.class.getName(), null);
		final CtClass candidateClassMock = EasyMock.createMock("candidateClassMock",CtClass.class);
		EasyMock.expect(candidateClassMock.getClassFile2()).andReturn(candidateClassFile).anyTimes();
		final Capture<CtField> fieldCapture = new Capture<CtField>();
		candidateClassMock.addField(EasyMock.capture(fieldCapture), EasyMock.anyObject(Initializer.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(candidateClassMock.isInterface()).andReturn(Boolean.TRUE);
		EasyMock.expect(candidateClassMock.isFrozen()).andReturn(Boolean.FALSE);
		EasyMock.expect(candidateClassMock.getName()).andReturn(_InterfaceStub.class.getName()).anyTimes();

		EasyMock.replay(candidateClassMock);
		executor.applyStamp(candidateClassMock );

		EasyMock.verify(candidateClassMock);
		Assert.assertThat(fieldCapture.getValue(), CoreMatchers.notNullValue());
		final CtField ctField = fieldCapture.getValue();
		Assert.assertThat(ctField.getModifiers(), CoreMatchers.equalTo(AccessFlag.PUBLIC | AccessFlag.STATIC | AccessFlag.FINAL));
	}

	private static class _ClassStub extends Number {

		private static final long serialVersionUID = 1L;

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

		@Override
		public float floatValue() {
			return 0;
		}

		@Override
		public double doubleValue() {
			return 0;
		}
		
	}
	
	private static interface _InterfaceStub extends Serializable {
		// nothing
	}

}
