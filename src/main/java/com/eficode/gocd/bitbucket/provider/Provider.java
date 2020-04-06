package com.eficode.gocd.bitbucket.provider;

import com.eficode.gocd.bitbucket.settings.general.GeneralPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.scm.ScmPluginConfigurationView;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.model.GitConfig;

import java.util.Map;

public interface Provider {
    public GoPluginIdentifier getPluginId();

    public String getName();

    public void addConfigData(GitConfig gitConfig);

    public boolean isValidURL(String url);

    public void checkConnection(GitConfig gitConfig);

    public String getRefSpec();

    public String getRefPattern();

    public void setApiUrl(String url);

    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data);

    public ScmPluginConfigurationView getScmConfigurationView();

    public GeneralPluginConfigurationView getGeneralConfigurationView();
}
