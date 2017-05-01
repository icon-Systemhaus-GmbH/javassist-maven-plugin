
package de.icongmbh.oss.maven.plugin.javassist;

import javassist.build.IClassTransformer;

import static org.easymock.EasyMock.createMock;

import java.util.Iterator;
import javassist.ClassPool;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the main steps in
 * {@link JavassistTransformerExecutor#transform(IClassTransformer, String, String, Iterator) }.
 *
 * The source files are fresh compiled and don't transform before, so there is no stamp in it.
 *
 * @throws Exception
 */
public class TestJavassistTransformerExecutor_transform
        extends JavassistTransformerExecutorTestBase {

  private ClassPool classPool;

  @Before
  public void setUp_ClassPool() {
    classPool = createMock("classPool", ClassPool.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void do_nothing_if_transformer_is_null() throws Exception {
    // given
    final Iterator<String> classNames = createMock("classNames", Iterator.class);

    final JavassistTransformerExecutor sut = javassistTransformerExecutor();

    // when
    sut.transform(null,
                  classDirectory().getAbsolutePath(),
                  transformedClassDirectory().getAbsolutePath(),
                  classNames);

    // then

  }

  @Override
  protected ClassPool classPool() {
    return this.classPool;
  }

}
