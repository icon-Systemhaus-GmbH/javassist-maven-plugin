package de.icongmbh.oss.maven.plugin.javassist.stubs;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;

//TODO unused ?
@SuppressWarnings("serial")
public class SettingsStub
    extends Settings
{
    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List getProxies()
    {
        return Collections.EMPTY_LIST;
    }
}