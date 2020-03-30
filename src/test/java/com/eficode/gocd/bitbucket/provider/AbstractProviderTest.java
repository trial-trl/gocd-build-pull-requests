package com.eficode.gocd.bitbucket.provider;

import com.eficode.gocd.bitbucket.settings.scm.PluginConfigurationView;


public abstract class AbstractProviderTest {

    protected abstract Provider getProvider();

    protected PluginConfigurationView getScmView() {
        return getProvider().getScmConfigurationView();
    }

    protected PluginConfigurationView getGeneralView() {
        return getProvider().getGeneralConfigurationView();
    }
}
