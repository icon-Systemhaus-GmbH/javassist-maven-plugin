package de.icongmbh.oss.maven.plugin.javassist.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

public class Project1Stub extends MavenProjectStub
{

  /**
   * Default constructor
   */
  public Project1Stub()
  {
    MavenXpp3Reader pomReader = new MavenXpp3Reader();
    Model model;
    try
    {
      model = pomReader.read( ReaderFactory.newXmlReader( new File( getBasedir(), "pom.xml" ) ) );
      setModel( model );
    }
    catch ( Exception e )
    {
      throw new RuntimeException( e );
    }

    setGroupId( model.getGroupId() );
    setArtifactId( model.getArtifactId() );
    setVersion( model.getVersion() );
    setName( model.getName() );
    setUrl( model.getUrl() );
    setPackaging( model.getPackaging() );

    Build build = new Build();
    build.setFinalName( model.getArtifactId() );
    build.setDirectory( getBasedir() + "/target" );
    build.setSourceDirectory( getBasedir() + "/src/main/java" );
    build.setOutputDirectory( getBasedir() + "/target/classes" );
    build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
    build.setTestOutputDirectory( getBasedir() + "/target/test-classes" );
    setBuild( build );

    List<String> compileSourceRoots = new ArrayList<String>();
    compileSourceRoots.add( getBasedir() + "/src/main/java" );
    setCompileSourceRoots( compileSourceRoots );

    List<String> testCompileSourceRoots = new ArrayList<String>();
    testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
    setTestCompileSourceRoots( testCompileSourceRoots );

    setRuntimeClasspathElements( new ArrayList<String>() );
  }

  /**
   * {@inheritDoc}
   */
  public File getBasedir()
  {
    return new File( super.getBasedir() + "/src/test/resources/project1/" );
  }

}
