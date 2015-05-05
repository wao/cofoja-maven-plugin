package info.thinkmore.maven.plugin.cofoja.test;

import com.google.java.contract.Requires;

public class App 
{
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
