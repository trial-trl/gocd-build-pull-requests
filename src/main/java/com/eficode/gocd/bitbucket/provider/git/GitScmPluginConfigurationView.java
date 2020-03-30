package com.eficode.gocd.bitbucket.provider.git;

import com.eficode.gocd.bitbucket.util.BranchFilter;
import com.eficode.gocd.bitbucket.settings.scm.DefaultScmPluginConfigurationView;
import com.eficode.gocd.bitbucket.util.FieldFactory;

import java.util.Map;

public class GitScmPluginConfigurationView extends DefaultScmPluginConfigurationView {

    public static final String BRANCH_BLACKLIST_PROPERTY_NAME = "branchblacklist";
    public static final String BRANCH_WHITELIST_PROPERTY_NAME = "branchwhitelist";

    @Override
    public String templateName() {
        return "/views/scm.template.branch.filter.html";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> fields = super.fields();
        fields.put(BRANCH_WHITELIST_PROPERTY_NAME,
                FieldFactory.createForScm("Whitelisted branches", "", true, false, false, "4"));
        fields.put(BRANCH_BLACKLIST_PROPERTY_NAME,
                FieldFactory.createForScm("Blacklisted branches", "", true, false, false, "5"));
        return fields;
    }

    @Override
    public BranchFilter getBranchFilter(Map<String, String> configuration) {
        String blacklist = configuration.get(BRANCH_BLACKLIST_PROPERTY_NAME);
        String whitelist = configuration.get(BRANCH_WHITELIST_PROPERTY_NAME);

        return new BranchFilter(blacklist, whitelist);
    }

}
