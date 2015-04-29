package info.thinkmore.maven.plugin.cofoja;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.project.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.google.common.base.*;

/**
 * @goal run
 * @phase process-classes
 */
public class CofojaOfflineMojo extends AbstractMojo
{
    /**
     * Location of the output files.
     * @parameter property="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Location of the source files.
     * @parameter property="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * Current Maven Project
     * @parameter default-value="${project}"
     * @required
     */
    private MavenProject project;

    void find( File dir ){
        for( File entry : dir.listFiles() ){
            if( entry.isDirectory() ){
                find( entry );
            }
            else{
                System.out.println( String.format( "Meet file here: %s", entry.getName() ) );
            }
        }
    }

    String getClasspath(){
        try{
            return Joiner.on(":").join( project.getCompileClasspathElements() );
        }catch(DependencyResolutionRequiredException e){
            throw new RuntimeException(e);
        }
    }

    public void execute()
        throws MojoExecutionException
    {
        File f = outputDirectory;

        if ( !f.exists() ){
            f.mkdirs();
        }
        
        find( sourceDirectory );

        //if( project == null ){
            //throw new RuntimeException( "project is not setted" );
        //}

        //System.out.println( String.format( "Class path: %s", getClasspath() ) );


        //catch ( IOException e )
        //{
            //throw new MojoExecutionException( "Error creating file " + touch, e );
        //}
    }
}
