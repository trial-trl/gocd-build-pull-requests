package in.ashwanthkumar.gocd.github.provider.github;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.GitHelper;
import in.ashwanthkumar.gocd.github.util.BranchFilter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.UnhandledException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class PRBranchFilter extends BranchFilter {

    private static Logger LOGGER = Logger.getLoggerFor(PRBranchFilter.class);

    public PRBranchFilter(String blacklistedBranches, String whitelistedBranches) {
        super(blacklistedBranches, whitelistedBranches);
    }

    private boolean matches(String branchName, GitHelper git) {
        if (branchName == null) return false;
        LOGGER.info(String.format("Verifying that branch (%s) is whitelisted or not blacklisted for %s",
                branchName, git.workingRepositoryUrl()));
        if (this.getWhitelistedBranches().isEmpty() && this.getBlacklistedBranches().isEmpty()) {
            LOGGER.info(String.format("No whitelist/blacklist entries present. Match is true for branch: %s for %s",
                    branchName, git.workingRepositoryUrl()));
            return true;
        }
        if (!this.getBlacklistedBranches().matches(branchName) && this.getWhitelistedBranches().matches(branchName)) {
            LOGGER.info(String.format("Successfully matched branch (%s) for %s!",
                    branchName, git.workingRepositoryUrl()));
            return true;
        }
        return false;
    }

    private void logRevisionMaps(Map<String, String> branchToRevision, Map<String, String> prToRevision) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        MapUtils.debugPrint(ps, "Branch2Revision", branchToRevision);
        MapUtils.debugPrint(ps, "PRNum2Revision", prToRevision);
        try {
            LOGGER.info(os.toString("UTF8"));
        } catch (UnsupportedEncodingException e) {}
    }

    @Override
    public boolean isBranchValid(String branch, GitHelper git) {
        if (branch == null) return false;

        LOGGER.info(String.format("Testing PR #: %s for %s", branch, git.workingRepositoryUrl()));
        Map<String, String> prToRevision =  git.getBranchToRevisionMap(GitHubProvider.REF_PATTERN);
        Map<String, String> branchToRevision = git.getBranchLatestRevisions();
        if (branchToRevision.size() == 0) {
            LOGGER.info(String.format("Could not develop map of branch -> revision for PR # %s of %s", branch, git.workingRepositoryUrl()));
        }

        String revision = prToRevision.get(branch);
        LOGGER.info(String.format("Finding Equivalent Branch Name for Revision: %s for %s", revision, git.workingRepositoryUrl()));
        if (!branchToRevision.values().contains(revision)) {
            LOGGER.info(String.format("Failed to find equivalent item for revision %s in branch -> revision map for %s", revision, git.workingRepositoryUrl()));
            logRevisionMaps(branchToRevision, prToRevision);
            return false;
        }
        for (String branchName : branchToRevision.keySet()) {
            String candidateRevision = branchToRevision.get(branchName);
            if (candidateRevision.equals(revision)) {
                return this.matches(branchName, git);
            }
        }
        LOGGER.info(String.format("Failed to match branch name to PR #: %s with Revision: %s", branch, revision));
        logRevisionMaps(branchToRevision, prToRevision);
        return false;
    }

}
