package de.icongmbh.oss.maven.plugin.javassist;

import java.io.Serializable;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtField.Initializer;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ClassTransformerTest {

	@Test
	public void testModifierOnStampField_class() throws CannotCompileException {
		final SampleTransformer sampleTransformer = new SampleTransformer();
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
		sampleTransformer.applyStamp(candidateClassMock);

		EasyMock.verify(candidateClassMock);
		Assert.assertThat(fieldCapture.getValue(), CoreMatchers.notNullValue());
		final CtField ctField = fieldCapture.getValue();
		Assert.assertThat(ctField.getModifiers(), CoreMatchers.equalTo(AccessFlag.PRIVATE | AccessFlag.STATIC | AccessFlag.FINAL));
	}

	@Test
	public void testModifierOnStampField_interface() throws CannotCompileException {
		final SampleTransformer sampleTransformer = new SampleTransformer();
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
		sampleTransformer.applyStamp(candidateClassMock );

		EasyMock.verify(candidateClassMock);
		Assert.assertThat(fieldCapture.getValue(), CoreMatchers.notNullValue());
		final CtField ctField = fieldCapture.getValue();
		Assert.assertThat(ctField.getModifiers(), CoreMatchers.equalTo(AccessFlag.PUBLIC | AccessFlag.STATIC | AccessFlag.FINAL));
	}

	private static class _ClassStub extends Number {

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
