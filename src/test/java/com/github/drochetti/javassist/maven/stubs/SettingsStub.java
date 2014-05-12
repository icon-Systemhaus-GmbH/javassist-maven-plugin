package com.github.drochetti.javassist.maven.stubs;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;

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