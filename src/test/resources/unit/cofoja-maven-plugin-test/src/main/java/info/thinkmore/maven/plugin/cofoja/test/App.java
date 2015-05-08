package info.thinkmore.maven.plugin.cofoja.test;

import com.google.java.contract.Requires;
import com.google.java.contract.Invariant;

public class App 
{

    @Invariant( "j > 10" )
    static class In{
        int j;
    }

    @Requires("x>3")
    public int  shouldFailed(int x){
        return x;
    }

    public static void main( String[] args )
    {
        App a = new App();
        a.shouldFailed(1);
    }
}
