package com.eficode.gocd.bitbucket.settings.scm;

import com.eficode.gocd.bitbucket.util.BranchFilter;

import java.util.Map;

public interface ScmPluginConfigurationView extends PluginConfigurationView {

    BranchFilter getBranchFilter(Map<String, String> configuration);
}
