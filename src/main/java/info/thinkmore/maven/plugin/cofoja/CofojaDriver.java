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
import org.apache.maven.plugin.logging.Log;

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
    File dumpDirectory;
    private final Log logger;

    String getClasspath(){
        return classpath;
    }


    boolean exec( List<String> cmds, Map<String, String> env ){
        logger.debug( String.format( "Exec: %s", Joiner.on(" ").join( cmds ) ) );
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
                logger.debug( line );
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

        logger.debug( String.format( "Exec result: %d", result ) );
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
        cmds.add(String.format("-Acom.google.java.contract.dump=%s", dumpDirectory.getAbsolutePath() ) );
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

    static interface FileHandle{
        int handle( File entry ) throws MojoExecutionException;
    }

    int find( File dir, FileHandle fh ) throws MojoExecutionException{
        int count = 0;
        for( File entry : dir.listFiles() ){
            if( entry.isDirectory() ){
                count += find( entry, fh );
            }
            else{
                count += fh.handle( entry );
            }
        }

        return count;

    }

    int process() throws MojoExecutionException{
        //Scan all the java file and generate contracts file
        find( sourceDirectory, new FileHandle(){
            @Override
            public int handle( File src ) throws MojoExecutionException{
                Filename relativePath = Filename.of( src.getAbsolutePath().substring( sourceDirectory.getAbsolutePath().length() + 1 )  );
                logger.debug( String.format( "Find source file: %s:%s", relativePath.toString(), relativePath.getExt() ) );
                if( relativePath.getExt().equals( "java" ) ){
                    if( !generateContract( src ) ){
                        String msg = String.format( "Generate contract error for class file: %s ", src.getPath()  );
                        logger.error( msg );
                        throw new MojoExecutionException( msg );
                    }
                }
                return 1;
            }
        });

        //Scan all the contracts file and move to contract directory
        find( outputDirectory, new FileHandle(){
            @Override
            public int handle( File src ) throws MojoExecutionException{
                try{
                Filename relativePath = Filename.of( src.getAbsolutePath().substring( outputDirectory.getAbsolutePath().length() + 1 )  );
                File markFile = new File( contractDirectory, relativePath.changeExt( "contracts" ) );
                File origClassFile = new File( contractDirectory, relativePath.changeExt( "class" ) );
                logger.debug( String.format( "Find contracts file: %s:%s", relativePath.toString(), relativePath.getExt() ) );
                File path = markFile.getParentFile();
                if( !path.exists() ){
                    path.mkdirs(); 
                }

                if( relativePath.getExt().equals( "contracts" ) ){
                    logger.debug( String.format( "Move contracts file: %s:%s", relativePath.toString(), relativePath.getExt() ) );
                    src.renameTo( markFile );
                    return 1;
                }
                if( relativePath.getExt().equals( "class" ) ){
                    logger.debug( String.format( "Copy class file: %s:%s", relativePath.toString(), relativePath.getExt() ) );
                    Files.copy(src, origClassFile );
                    return 1;
                }
                return 0;
                }
                catch(IOException e){
                    throw new MojoExecutionException( String.format("IOException:%s", e.getMessage() ),e );
                }
            }
        });

        //Scan all java file and weave contracts
        find( sourceDirectory, new FileHandle(){
            @Override
            public int handle( File src ) throws MojoExecutionException{
                Filename relativePath = Filename.of( src.getAbsolutePath().substring( sourceDirectory.getAbsolutePath().length() + 1 )  );
                if( relativePath.getExt().equals( "java" ) ){
                    logger.debug( String.format( "Weave Contracts for %s:%s", relativePath.toString(), relativePath.getExt() ) );
                    File origClassFile = new File( contractDirectory, relativePath.changeExt( "class" ) );

                    //using offline writer to weave contract
                    if( origClassFile.exists() ){
                        if( !offlineWrite( origClassFile ) ){
                            String msg = "OfflineWriter error for class file:" + origClassFile.getPath();
                            logger.error( msg );
                            throw new MojoExecutionException(  msg );
                        }
                        return 1;
                    }
                }
                return 0;
            }
        });

        return 1;
    }

    boolean moveContractsAndHelpContracts( File sourceFile, File destFile ){
        if( !sourceFile.renameTo( destFile ) ){
            return false;
        }

        final String pattern = Filename.of( sourceFile.getName() ).getName() + "$";
        //System.out.println( pattern );

        File[] files = sourceFile.getParentFile().listFiles( new FilenameFilter(){
            //@Override
            public boolean accept(File dir, String name ){
                return name.startsWith( pattern ) && name.endsWith( ".contracts" );
            }
        } );

        File destPath = destFile.getParentFile();

        for( File srcFile : files ){
            //System.out.println( srcFile.getPath() );
            logger.debug( "Handle contracts file: " + srcFile.getPath() );
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
            //System.out.println( pattern );

            File[] files = sourceFile.getParentFile().listFiles( new FilenameFilter(){
                //@Override
                public boolean accept(File dir, String name ){
                    return name.startsWith( pattern ) && name.endsWith( ".class" );
                }
            } );

            File destPath = destFile.getParentFile();

            for( File srcFile : files ){
                //System.out.println( srcFile.getPath() );
                logger.debug( "Copy helper class: " + srcFile.getPath() );
                Files.copy( srcFile, new File( destPath, srcFile.getName() ) );
            }
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    int handle( File src ) throws MojoExecutionException{
        int ret = 0;
        try{
            if( info.thinkmore.SimpleAssert.enable ){
                info.thinkmore.SimpleAssert.assertTrue( src.getAbsolutePath().startsWith( sourceDirectory.getAbsolutePath() ) );
            }

            Filename relativePath = Filename.of( src.getAbsolutePath().substring( sourceDirectory.getAbsolutePath().length() + 1 )  );
            logger.debug( String.format( "Find source file: %s:%s", relativePath.toString(), relativePath.getExt() ) );

            if( relativePath.getExt().equals( "java" ) ){
                File classFile = new File( outputDirectory, relativePath.changeExt( "class" ) );
                File origClassFile = new File( contractDirectory, relativePath.changeExt( "class" ) );
                File markFile = new File( contractDirectory, relativePath.changeExt( "contracts" ) );
                File contractFile = new File( outputDirectory, relativePath.changeExt( "contracts" ) );
                logger.debug( "Test existense of class file: " + classFile.getPath() );
                //if class file exits then
                if( classFile.exists() ){
                    File path = markFile.getParentFile();
                    if( !path.exists() ){
                        path.mkdirs(); 
                    }

                    copyClassAndHelpClass(classFile, origClassFile );

                    logger.debug( "Class file exists!" );
                    //if ( mark file not exits ) or ( mark file is older than class file )
                    if( ( !markFile.exists() ) || ( markFile.lastModified() < classFile.lastModified() ) ){
                        //regenerate contract file
                        if( !generateContract( src ) ){
                            String msg = String.format( "Generate contract error for class file: %s ", src.getPath()  );
                            logger.error( msg );
                            throw new MojoExecutionException( msg );
                        }
                        //if new contract file generated
                        if( contractFile.exists() ){
                            //move contract file to contract directory
                            markFile.delete();

                            if( !moveContractsAndHelpContracts( contractFile, markFile) ){
                                String msg = String.format("Rename contracts file from %s to %s error!", contractFile.getAbsolutePath(), markFile.getAbsolutePath() );
                                logger.error( msg );
                                throw new MojoExecutionException( msg );
                            }

                            //using offline writer to weave contract
                            if( !offlineWrite( origClassFile ) ){
                                String msg = "OfflineWriter error for class file:" + origClassFile.getPath();
                                logger.error( msg );
                                throw new MojoExecutionException(  msg );
                            }

                            ret = 1;
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
            return ret;
        }
        catch( MojoExecutionException ex ){
            throw ex;
        }
        catch(Exception ex){
            logger.error( "CofojaDriver handle: Unknown exception." );
            throw new MojoExecutionException( "CofojaDriver handle: Unknown exception.", ex );
        }
    }
}
