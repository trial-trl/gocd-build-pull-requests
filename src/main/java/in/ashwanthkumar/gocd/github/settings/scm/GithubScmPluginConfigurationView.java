package in.ashwanthkumar.gocd.github.settings.scm;

import in.ashwanthkumar.gocd.github.provider.github.PRBranchFilter;
import in.ashwanthkumar.gocd.github.util.BranchFilter;
import in.ashwanthkumar.gocd.github.util.FieldFactory;

import java.util.Map;

public class GithubScmPluginConfigurationView extends DefaultScmPluginConfigurationView {
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
                FieldFactory.createForScm("Whitelisted branches", "", true, false, false, "5"));
        fields.put(BRANCH_BLACKLIST_PROPERTY_NAME,
                FieldFactory.createForScm("Blacklisted branches", "", true, false, false, "6"));
        return fields;
    }

    @Override
    public BranchFilter getBranchFilter(Map<String, String> configuration) {
        String blacklist = configuration.get(BRANCH_BLACKLIST_PROPERTY_NAME);
        String whitelist = configuration.get(BRANCH_WHITELIST_PROPERTY_NAME);

        return new PRBranchFilter(blacklist, whitelist);
    }
}
