package in.ashwanthkumar.gocd.github.provider.github;

import com.tw.go.plugin.GitHelper;
import in.ashwanthkumar.gocd.github.util.BranchFilter;

import java.util.Map;

public class PRBranchFilter extends BranchFilter {

    public PRBranchFilter(String blacklistedBranches, String whitelistedBranches) {
        super(blacklistedBranches, whitelistedBranches);
    }

    private boolean matches(String branchName) {
        if (branchName == null) return false;
        if (this.getWhitelistedBranches().isEmpty() && this.getBlacklistedBranches().isEmpty()) return true;
        if (!this.getBlacklistedBranches().matches(branchName) && this.getWhitelistedBranches().matches(branchName)) return true;
        return false;
    }

    @Override
    public boolean isBranchValid(String branch, GitHelper git) {
        if (branch == null) return false;

        Map<String, String> prToRevision =  git.getBranchToRevisionMap(GitHubProvider.REF_PATTERN);
        Map<String, String> branchToRevision = git.getBranchLatestRevisions();

        String revision = prToRevision.get(branch);
        if (!branchToRevision.values().contains(revision)) return false;
        for (String branchName : branchToRevision.keySet()) {
            String candidateRevision = branchToRevision.get(branchName);
            if (candidateRevision.equals(revision)) {
                return this.matches(branchName);
            }
        }
        return false;
    }

}
