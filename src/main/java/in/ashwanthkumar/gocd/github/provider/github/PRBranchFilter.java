package in.ashwanthkumar.gocd.github.provider.github;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.GitHelper;
import in.ashwanthkumar.gocd.github.util.BranchFilter;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

public class PRBranchFilter extends BranchFilter {

    private static Logger LOGGER = Logger.getLoggerFor(PRBranchFilter.class);

    public PRBranchFilter(String blacklistedBranches, String whitelistedBranches) {
        super(blacklistedBranches, whitelistedBranches);
    }

    private boolean matches(String branchName) {
        if (branchName == null) return false;
        LOGGER.info(String.format("Verifying that branch (%s) is whitelisted or not blacklisted", branchName));
        if (this.getWhitelistedBranches().isEmpty() && this.getBlacklistedBranches().isEmpty()) return true;
        if (!this.getBlacklistedBranches().matches(branchName) && this.getWhitelistedBranches().matches(branchName)) return true;
        return false;
    }

    @Override
    public boolean isBranchValid(String branch, GitHelper git) {
        if (branch == null) return false;

        LOGGER.info(String.format("Testing PR #: %s", branch));
        Map<String, String> prToRevision =  git.getBranchToRevisionMap(GitHubProvider.REF_PATTERN);
        Map<String, String> branchToRevision = git.getBranchLatestRevisions();

        String revision = prToRevision.get(branch);
        LOGGER.info(String.format("Finding Equivalent Branch Name forRevision: %s", revision));
        if (!branchToRevision.values().contains(revision)) {
            LOGGER.info(String.format("Failed to find equivalent item for revision in branch -> revision map"));
            MapUtils.debugPrint(System.out, "Branch2Revision", branchToRevision);
            MapUtils.debugPrint(System.out, "PRNum2Revision", prToRevision);
            return false;
        }
        for (String branchName : branchToRevision.keySet()) {
            String candidateRevision = branchToRevision.get(branchName);
            if (candidateRevision.equals(revision)) {
                return this.matches(branchName);
            }
        }
        LOGGER.info(String.format("Failed to match branch name to PR #: %s with Revision: %s", branch, revision));
        MapUtils.debugPrint(System.out, "Branch2Revision", branchToRevision);
        MapUtils.debugPrint(System.out, "PRNum2Revision", prToRevision);
        return false;
    }

}
