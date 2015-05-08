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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
//import org.apache.maven.artifact.DependencyResolutionRequiredException;
//import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.*;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.*;
//import com.jcabi.aether.Classpath;

@Mojo( 
    name = "run", 
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE, 
    threadSafe = true 
    )
@Execute( 
    goal = "run",
    phase = LifecyclePhase.COMPILE,
    lifecycle = "cofojainject"
    )
public class CofojaOfflineMojo extends AbstractMojo
{
    @Parameter( property = "outputDirectory", defaultValue = "${project.build.outputDirectory}" )
    private File outputDirectory;

    @Parameter( property = "outputDirectory", defaultValue = "${project.build.directory}/contracts" )
    private File contractDirectory;

    @Parameter( property = "sourceDirectory", defaultValue = "${project.build.sourceDirectory}" )
    private File sourceDirectory;

    @Component
    private MavenProject project;

    @Parameter( defaultValue="${localRepository}" )
    private ArtifactRepository localRepository;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter( defaultValue="${classpath}")
    private String classpath;


    private Set<Artifact> getDependencyArtifacts(Artifact root) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(root)
            .setResolveTransitively(true)
            .setLocalRepository(localRepository);
        return repositorySystem.resolve(request).getArtifacts();
    }

    boolean isScopeCompile( Artifact art ){
        Filename fn = Filename.of( art.getFile().getAbsolutePath() );
        if( fn.getName().contains( "jre" ) ){
            return false;
        }
        if( fn.getExt().equals("zip") ){
            return false;
        }
        return  art.getScope().equals(Artifact.SCOPE_COMPILE ) ||  art.getScope().equals(Artifact.SCOPE_COMPILE_PLUS_RUNTIME ) ||  art.getScope().equals(Artifact.SCOPE_PROVIDED ) ||  art.getScope().equals(Artifact.SCOPE_SYSTEM );
    }

    String getClasspath2(){
        return classpath + ":" + outputDirectory.getAbsolutePath();
    }

    String getClasspath(){
        Set<String> paths = new HashSet<String>();
        for( Artifact art : project.getDependencyArtifacts() ){
            if( isScopeCompile( art ) ){
                paths.add( art.getFile().getAbsolutePath() );
                //System.out.println( String.format( "Got direct dependency artifact:%s", art.getFile().getAbsoluteFile() ) );
                for( Artifact dep : getDependencyArtifacts( art ) ){
                    if( isScopeCompile( dep ) ){
                        paths.add( dep.getFile().getAbsolutePath() );
                        //System.out.println( String.format( "=======>:%s", dep.getFile().getAbsoluteFile() ) );
                    }
                }
            }
        }

        paths.add( outputDirectory.getAbsolutePath() );
        return Joiner.on(":").join( paths );
    }

    public void execute() throws MojoExecutionException
    {
        //if( localRepository == null ){
        //throw new MojoExecutionException( "localRepository is not setted" );
        //}

        try{
            if( outputDirectory == null ){
                throw new MojoExecutionException( "outputDirectory is not setted" );
            }

            if( sourceDirectory == null ){
                throw new MojoExecutionException( "sourceDirectory is not setted" );
            }

            if( contractDirectory == null ){
                throw new MojoExecutionException( "sourceDirectory is not setted" );
            }

            if( project == null ){
                throw new MojoExecutionException( "project is not setted" );
            }

            File f = outputDirectory;
            if ( !f.exists() ){
                f.mkdirs();
            }

            f = contractDirectory;
            if ( !f.exists() ){
                f.mkdirs();
            }

            System.out.println( String.format( "Class path: %s", getClasspath() ) );

            CofojaDriver cmd = CofojaDriver.newInstance( sourceDirectory, outputDirectory, contractDirectory, getClasspath() );

            cmd.find( sourceDirectory );



            //catch ( IOException e )
            //{
            //throw new MojoExecutionException( "Error creating file " + touch, e );
            //}
        }
        catch(Exception ex){
            ex.printStackTrace( new PrintStream(System.out) );
        }
    }
}
