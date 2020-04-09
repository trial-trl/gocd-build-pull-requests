package com.eficode.gocd.bitbucket.settings.scm;

import com.eficode.gocd.bitbucket.util.BranchFilter;
import com.eficode.gocd.bitbucket.util.FieldFactory;

import java.util.HashMap;
import java.util.Map;

public class BitbucketScmPluginConfigurationView implements ScmPluginConfigurationView {
    @Override
    public String templateName() {
        return "/views/scm-bitbucket.template.html";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("url", FieldFactory.createForScm("URL", null, true, true, false, "0"));
        response.put("username", FieldFactory.createForScm("Username", null, false, false, false, "1"));
        response.put("password", FieldFactory.createForScm("Password", null, false, false, true, "2"));
        response.put("apiUrl",  FieldFactory.createForScm("Bitbucket Server URL", null, false, true, false, "3"));
        response.put("projectName",  FieldFactory.createForScm("Project name", null, false, true, false, "4"));
        response.put("defaultBranch", FieldFactory.createForScm("Default Branch", "master", false, false, false, "5"));
        response.put("shallowClone", FieldFactory.createForScm("Default Clone Behavior", "false", false, false, false, "6"));
        return response;
    }

    @Override
    public BranchFilter getBranchFilter(Map<String, String> configuration) {
        return new BranchFilter();
    }

    @Override
    public boolean hasConfigurationView() {
        return true;
    }
}
