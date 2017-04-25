package de.icongmbh.oss.maven.plugin.javassist;

import javassist.CtClass;
import javassist.build.JavassistBuildException;

public class SampleTransformer extends ClassTransformer {

    @Override
    public void applyTransformations(CtClass classToTransform) throws JavassistBuildException {
    }

    @Override
    public boolean shouldTransform(CtClass ctClass) throws JavassistBuildException {
        return false;
    }

}
