package de.icongmbh.oss.maven.plugin.javassist.it.transform;

/**
 * This is a class for use integration tests only.
 *
 * @since 1.2.0
 */
public class ProjectClass {
  /**
   * This method should be modified by javassist.
   * <p>
   * The return value of {@link #toString() } will replace with a constant value provided by javassist statement in POM
   * file.
   * </p>
   *
   * @return maybe the {@link #toString() } value or maybe not, but hopefully never {@code null}.
   */
  public String getName() {
    return toString();
  }

  @Override
  public String toString() {
    return getClass().getName();
  }

}
