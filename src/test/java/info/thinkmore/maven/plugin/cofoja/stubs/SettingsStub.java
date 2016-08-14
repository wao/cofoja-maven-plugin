package info.thinkmore.maven.plugin.cofoja.stubs;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;

public class SettingsStub extends Settings
{
    static final long serialVersionUID = 0x12345;

    /** {@inheritDoc} */
    public List<Proxy> getProxies()
    {
        return Collections.emptyList();
    }
}
