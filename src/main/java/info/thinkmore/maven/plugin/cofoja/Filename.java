package info.thinkmore.maven.plugin.cofoja;

import lombok.*;

@Value
public class Filename{
    String name;
    String separator;
    String ext;

    public static Filename of( String path ){
        int pos = path.lastIndexOf( '.' );
        if( pos == -1 ){
            return new Filename( path,  "", "" );
        }
        else{
            return new Filename( path.substring( 0, pos ), ".", path.substring( pos+1 ) );
        }
    }

    public String toString(){
        return name + separator + ext;
    }

    public String changeExt( String newExt ){
        if( info.thinkmore.SimpleAssert.enable ){
            info.thinkmore.SimpleAssert.assertEquals( ".", separator );
        }
        return name + separator + newExt;
    }
}
