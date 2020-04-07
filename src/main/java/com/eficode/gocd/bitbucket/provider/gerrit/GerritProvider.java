package com.eficode.gocd.bitbucket.provider.gerrit;

import com.eficode.gocd.bitbucket.provider.Provider;
import com.eficode.gocd.bitbucket.settings.general.DefaultGeneralPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.general.GeneralPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.scm.DefaultScmPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.scm.ScmPluginConfigurationView;
import com.eficode.gocd.bitbucket.util.URLUtils;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;

import java.util.Arrays;
import java.util.Map;

public class GerritProvider implements Provider {
    public static final String REF_SPEC = "+refs/changes/*:refs/changes/*";
    public static final String REF_PATTERN = "refs/changes/";

    @Override
    public GoPluginIdentifier getPluginId() {
        return new GoPluginIdentifier("gerrit.cs", Arrays.asList("1.0"));
    }

    @Override
    public String getName() {
        return "Gerrit";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
    }

    @Override
    public void setApiUrl(String url) {
    }

    @Override
    public void setProjectName(String name) {
    }

    @Override
    public boolean isValidURL(String url) {
        return new URLUtils().isValidURL(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        HelperFactory.gitCmd(gitConfig, null).checkConnection();
    }

    @Override
    public String getRefSpec() {
        return REF_SPEC;
    }

    @Override
    public String getRefPattern() {
        return REF_PATTERN;
    }

    @Override
    public void populateRevisionData(GitConfig gitConfig, String changeId, String latestSHA, Map<String, String> data) {
        data.put("CHANGE_SET_ID", changeId);
    }

    @Override
    public ScmPluginConfigurationView getScmConfigurationView() {
        return new DefaultScmPluginConfigurationView();
    }

    @Override
    public GeneralPluginConfigurationView getGeneralConfigurationView() {
        return new DefaultGeneralPluginConfigurationView();
    }
}
