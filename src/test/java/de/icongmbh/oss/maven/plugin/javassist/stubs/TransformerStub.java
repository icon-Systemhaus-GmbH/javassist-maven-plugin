package de.icongmbh.oss.maven.plugin.javassist.stubs;

import java.util.ArrayList;
import java.util.List;

import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import javassist.CtClass;

public class TransformerStub extends ClassTransformer {
  private List<CtClass> transformed = new ArrayList<>();

  @Override
  public void applyTransformations(final CtClass ctClass) {
    transformed.add(ctClass);
  }

  @Override
  public boolean shouldTransform(final CtClass ctClass) {
    return true;
  }

  public List<CtClass> getTransformed() {
    return transformed;
  }
}
