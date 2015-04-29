package info.thinkmore.maven.plugin.cofoja;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;

import org.junit.Rule;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;

public class CofojaOfflineMojoTest 
{

    @Rule
    public MojoRule rule = new MojoRule()
    {
      @Override
      protected void before() throws Throwable 
      {
      }

      @Override
      protected void after()
      {
      }
    };


    @Test
    public void testSomething() throws Exception
    {

        File pom = new File( "src/test/resources/unit/first/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        System.out.println( "here" );
        CofojaOfflineMojo myMojo = (CofojaOfflineMojo) rule.lookupMojo( "run", pom );
        assertNotNull( myMojo );
        myMojo.execute();
    }
}
