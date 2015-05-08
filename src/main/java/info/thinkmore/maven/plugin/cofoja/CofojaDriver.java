package info.thinkmore.maven.plugin.cofoja;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import lombok.*;

@Value(staticConstructor="newInstance")
public class CofojaDriver{
    File sourceDirectory;
    File outputDirectory;
    File contractDirectory;
    String classpath;

    String getClasspath(){
        return classpath;
    }


    boolean exec( List<String> cmds, Map<String, String> env ){
        System.out.println( String.format( "Exec: %s", Joiner.on(" ").join( cmds ) ) );
        ProcessBuilder pb = new ProcessBuilder( cmds ); 
        pb.redirectErrorStream( true );

        if( env != null ){
            pb.environment().putAll( env );
        }

        int result = -1;
        try{
            Process p = pb.start();
            InputStream ip = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ip));
            String line;
            while( (line = reader.readLine()) != null ){
                System.out.println( line );
            }
            reader.close();
            result = pb.start().waitFor();
        }
        catch(InterruptedException e){
            throw new RuntimeException(e);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }

        System.out.println( String.format( "Exec result: %d", result ) );
        return result == 0;

    }

    boolean compile( File src ){
        List<String> cmds = Lists.newArrayList();
        cmds.add("/usr/bin/javac");
        //cmds.add("-verbose");
        cmds.add("-source");
        cmds.add("1.7");
        cmds.add("-target");
        cmds.add("1.7");
        cmds.add("-cp");
        cmds.add(getClasspath());
        cmds.add(String.format("-Acom.google.java.contract.classpath=%s", getClasspath()));
        cmds.add(String.format("-Acom.google.java.contract.sourcepath=%s", sourceDirectory.getAbsolutePath() ) );
        cmds.add(String.format("-Acom.google.java.contract.classoutput=%s", outputDirectory.getAbsolutePath() ) );
        cmds.add("-Acom.google.java.contract.debug");
        cmds.add("-Acom.google.java.contract.dryrun");
        cmds.add("-d");
        cmds.add(outputDirectory.getAbsolutePath());
        cmds.add( src.getAbsolutePath() );

        return exec( cmds, null );
    }

    boolean generateContract( File src ){
        List<String> cmds = Lists.newArrayList();
        cmds.add("/usr/bin/javac");
        //cmds.add("-verbose");
        cmds.add("-proc:only");
        cmds.add("-source");
        cmds.add("1.7");
        cmds.add("-target");
        cmds.add("1.7");
        cmds.add("-cp");
        cmds.add(getClasspath());
        cmds.add("-processor");
        cmds.add("com.google.java.contract.core.apt.AnnotationProcessor");
        cmds.add(String.format("-Acom.google.java.contract.classpath=%s", getClasspath()));
        cmds.add(String.format("-Acom.google.java.contract.sourcepath=%s", sourceDirectory.getAbsolutePath() ) );
        cmds.add(String.format("-Acom.google.java.contract.classoutput=%s", outputDirectory.getAbsolutePath() ) );
        cmds.add("-Acom.google.java.contract.debug");
        cmds.add("-Acom.google.java.contract.realwork");
        cmds.add( src.getAbsolutePath() );

        //Map<String,String> env = Maps.newHashMap();
        //env.put( "CLASSPATH", getClasspath() );

        return exec( cmds, null );
    }

    boolean offlineWrite( File classFile ){
        List<String> cmds = Lists.newArrayList();
        cmds.add("/usr/bin/java");
        //cmds.add("-verbose");
        cmds.add("-cp");
        cmds.add(getClasspath());
        cmds.add(String.format("-Dcom.google.java.contract.classpath=%s", getClasspath()));
        cmds.add(String.format("-Dcom.google.java.contract.sourcepath=%s", sourceDirectory.getAbsolutePath() ) );
        cmds.add(String.format("-Dcom.google.java.contract.classoutput=%s", outputDirectory.getAbsolutePath() ) );
        //cmds.add("-Dcom.google.java.contract.debug");
        cmds.add("com.google.java.contract.core.agent.PreMain");
        cmds.add( classFile.getAbsolutePath() );

        //System.out.println( String.format( "Exec: %s", Joiner.on(" ").join( cmds ) ) );
        return exec( cmds, null );
        //return true;
    }

    void find( File dir ){
        for( File entry : dir.listFiles() ){
            if( entry.isDirectory() ){
                find( entry );
            }
            else{
                handle( entry );
            }
        }
    }

    boolean moveContractsAndHelpContracts( File sourceFile, File destFile ){
        if( !sourceFile.renameTo( destFile ) ){
            return false;
        }

        final String pattern = Filename.of( sourceFile.getName() ).getName() + "$";
        System.out.println( pattern );

        File[] files = sourceFile.getParentFile().listFiles( new FilenameFilter(){
            //@Override
            public boolean accept(File dir, String name ){
                return name.startsWith( pattern ) && name.endsWith( ".contracts" );
            }
        } );

        File destPath = destFile.getParentFile();

        for( File srcFile : files ){
            System.out.println( srcFile.getPath() );
            if( !srcFile.renameTo( new File( destPath, srcFile.getName() ) ) ){
                return false;
            }
        }

        return true;
    }

    void copyClassAndHelpClass( File sourceFile, File destFile ){
        try{
            Files.copy(sourceFile, destFile );
            final String pattern = Filename.of( sourceFile.getName() ).getName() + "$";
            System.out.println( pattern );

            File[] files = sourceFile.getParentFile().listFiles( new FilenameFilter(){
                //@Override
                public boolean accept(File dir, String name ){
                    return name.startsWith( pattern ) && name.endsWith( ".class" );
                }
            } );

            File destPath = destFile.getParentFile();

            for( File srcFile : files ){
                System.out.println( srcFile.getPath() );
                Files.copy( srcFile, new File( destPath, srcFile.getName() ) );
            }
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    void handle( File src ){
        try{
            if( info.thinkmore.SimpleAssert.enable ){
                info.thinkmore.SimpleAssert.assertTrue( src.getAbsolutePath().startsWith( sourceDirectory.getAbsolutePath() ) );
            }

            Filename relativePath = Filename.of( src.getAbsolutePath().substring( sourceDirectory.getAbsolutePath().length() + 1 )  );
            System.out.println( String.format( "Meet file here: %s:%s", relativePath.toString(), relativePath.getExt() ) );

            if( relativePath.getExt().equals( "java" ) ){
                System.out.println( "1" );
                File classFile = new File( outputDirectory, relativePath.changeExt( "class" ) );
                File origClassFile = new File( contractDirectory, relativePath.changeExt( "class" ) );
                File markFile = new File( contractDirectory, relativePath.changeExt( "contracts" ) );
                File contractFile = new File( outputDirectory, relativePath.changeExt( "contracts" ) );
                System.out.println( classFile.getPath() );
                //if class file exits then
                if( classFile.exists() ){
                    System.out.println( "2" );
                    File path = markFile.getParentFile();
                    if( !path.exists() ){
                        path.mkdirs(); 
                    }

                    copyClassAndHelpClass(classFile, origClassFile );

                    System.out.println( "Class file exists!" );
                    //if ( mark file not exits ) or ( mark file is older than class file )
                    if( ( !markFile.exists() ) || ( markFile.lastModified() < classFile.lastModified() ) ){
                        //regenerate contract file
                        if( !generateContract( src ) ){
                            System.out.println( "Generate contract error!" );
                            throw new MojoExecutionException(  "Generate contract error!" );
                        }
                        //if new contract file generated
                        if( contractFile.exists() ){
                            //move contract file to contract directory
                            markFile.delete();

                            if( !moveContractsAndHelpContracts( contractFile, markFile) ){
                                System.out.println( String.format("Renmae contracts file from %s to %s error!", contractFile.getAbsolutePath(), markFile.getAbsolutePath() ) );
                            }

                            //using offline writer to weave contract
                            if( !offlineWrite( origClassFile ) ){
                                System.out.println( "OfflineWriter error!" );
                                throw new MojoExecutionException(  "OfflineWriter error!" );
                            }

                        }
                        //else
                        else{
                            //direct touch a contract file in contract directory
                            Files.touch(markFile);
                            //end
                        }
                        //end
                    }
                    //else since we don't have class file means we don't need to process, just leave it as
                    //end
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace( new PrintStream(System.out) );
        }
    }
}
