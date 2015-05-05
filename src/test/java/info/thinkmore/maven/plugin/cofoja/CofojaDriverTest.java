package info.thinkmore.maven.plugin.cofoja;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;

import static org.junit.Assert.*;

import org.junit.Test;

public class CofojaDriverTest 
{
    File sourceDirectory = new File( "src/test/resources/unit/cofoja-maven-plugin-test/src/main" );
    File outputDirectory = new File( "target/test-result/classes" );
    File contractDirectory = new File( "target/test-result/contract" );
    String classpath = "src/test/resources/lib/cofoja-1.2-SNAPSHOT.jar:src/test/resources/lib/asm-all-5.0.3.jar";

    File sourceFile = new File( sourceDirectory, "java/info/thinkmore/maven/plugin/cofoja/test/App.java" );
    File classFile = new File( outputDirectory, "info/thinkmore/maven/plugin/cofoja/test/App.class" );
    File contractFile = new File( outputDirectory, "info/thinkmore/maven/plugin/cofoja/test/App.contracts" );
    File markFile = new File( contractDirectory, "info/thinkmore/maven/plugin/cofoja/test/App.contracts" );

    @Before
    public void makeAndCleanDirectory(){
        outputDirectory.mkdirs();
        contractDirectory.mkdirs();
        deleteDirectoryContent(outputDirectory);
        deleteDirectoryContent(contractDirectory);
    }

    void deleteDirectoryContent(File dir){
        for( File entry : dir.listFiles() ){
            if( entry.isDirectory() ){
                deleteDirectoryContent( entry );
            }
            entry.delete();
        }
    }
    
    @Test
    public void testSomething() throws Exception{
        CofojaDriver driver = CofojaDriver.newInstance( sourceDirectory, outputDirectory, contractDirectory, classpath );
        assertTrue( ! classFile.exists() );
        assertTrue( ! contractFile.exists() );
        assertTrue( ! markFile.exists() );
        assertTrue( driver.compile( sourceFile ) );
        assertTrue( classFile.exists() );
        assertTrue( ! contractFile.exists() );
        assertTrue( ! markFile.exists() );
        assertTrue( driver.generateContract( sourceFile ) );
        assertTrue( classFile.exists() );
        assertTrue( contractFile.exists() );
        assertTrue( ! markFile.exists() );
        assertTrue( driver.offlineWrite( classFile ) );
    }
}
