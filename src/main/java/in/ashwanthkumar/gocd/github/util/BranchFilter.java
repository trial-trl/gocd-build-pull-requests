package in.ashwanthkumar.gocd.github.util;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.GitHelper;

public class BranchFilter {

    private static Logger LOGGER = Logger.getLoggerFor(BranchFilter.class);

    public static final String NO_BRANCHES = "";
    private final BranchMatcher blacklistedBranches;
    private final BranchMatcher whitelistedBranches;

    public BranchFilter() {
        this(NO_BRANCHES, NO_BRANCHES);
    }

    public BranchFilter(String blacklistOption, String whitelistOption) {
        this.blacklistedBranches = new BranchMatcher(blacklistOption, BranchMatcher.Mode.FAIL_EMPTY);
        this.whitelistedBranches = new BranchMatcher(whitelistOption, BranchMatcher.Mode.PASS_EMPTY);
    }

    protected BranchMatcher getBlacklistedBranches() {
        return this.blacklistedBranches;
    }

    protected BranchMatcher getWhitelistedBranches() {
        return this.whitelistedBranches;
    }

    public boolean isBranchValid(String branch, GitHelper git) {
        if (branch == null) {
            return false;
        } else if (whitelistedBranches.isEmpty() && blacklistedBranches.isEmpty()) {
            return true;
        } else if (whitelistedBranches.matches(branch) && !blacklistedBranches.matches(branch)) {
            return true;
        } else {
            return false;
        }
    }

}
