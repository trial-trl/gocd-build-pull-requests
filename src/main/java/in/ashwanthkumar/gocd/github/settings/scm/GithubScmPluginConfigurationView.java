package in.ashwanthkumar.gocd.github.settings.scm;

import in.ashwanthkumar.gocd.github.provider.git.GitScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.FieldFactory;

import java.util.HashMap;
import java.util.Map;

public class GithubScmPluginConfigurationView extends GitScmPluginConfigurationView {
    public static final String BRANCH_BLACKLIST_PROPERTY_NAME = "branchblacklist";
    public static final String BRANCH_WHITELIST_PROPERTY_NAME = "branchwhitelist";

    @Override
    public String templateName() {
        return "/views/scm-github.template.branch.filter.html";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("url", FieldFactory.createForScm("URL", null, true, true, false, "0"));
        fields.put("username", FieldFactory.createForScm("Username", null, false, false, false, "1"));
        fields.put("password", FieldFactory.createForScm("OAuth Token", null, false, false, true, "2"));
        fields.put("defaultBranch", FieldFactory.createForScm("Default Branch", "master", false, false, false, "3"));
        fields.put("shallowClone",
                FieldFactory.createForScm("Default Clone Behavior", "false", false, false, false, "4"));
        fields.put(BRANCH_WHITELIST_PROPERTY_NAME,
                FieldFactory.createForScm("Whitelisted branches", "", true, false, false, "5"));
        fields.put(BRANCH_BLACKLIST_PROPERTY_NAME,
                FieldFactory.createForScm("Blacklisted branches", "", true, false, false, "6"));
        return fields;
    }
}
