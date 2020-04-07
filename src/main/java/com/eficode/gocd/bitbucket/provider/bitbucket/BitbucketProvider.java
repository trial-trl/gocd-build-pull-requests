package com.eficode.gocd.bitbucket.provider.bitbucket;

import com.cdancy.bitbucket.rest.BitbucketClient;
import com.cdancy.bitbucket.rest.domain.pullrequest.PullRequest;
import com.eficode.gocd.bitbucket.provider.Provider;
import com.eficode.gocd.bitbucket.provider.bitbucket.model.PullRequestStatus;
import com.eficode.gocd.bitbucket.settings.general.DefaultGeneralPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.general.GeneralPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.scm.DefaultScmPluginConfigurationView;
import com.eficode.gocd.bitbucket.settings.scm.ScmPluginConfigurationView;
import com.eficode.gocd.bitbucket.util.URLUtils;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.utils.func.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class BitbucketProvider implements Provider {
    private static final Logger LOG = LoggerFactory.getLogger(BitbucketProvider.class);
    // public static final String PR_FETCH_REFSPEC = "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*";
    // public static final String PR_MERGE_PREFIX = "refs/gh-merge/remotes/origin/";
    public static final String REF_SPEC = "+refs/pull/*/head:refs/remotes/origin/pull-request/*";
    public static final String REF_PATTERN = "refs/remotes/origin/pull-request/";

    private String bitbucketUrl;
    private String projectName;

    @Override
    public void setApiUrl(String url){
        this.bitbucketUrl = url;
    }

    @Override
    public void setProjectName(String name) { this.projectName = name; }

    @Override
    public GoPluginIdentifier getPluginId() {
        return new GoPluginIdentifier("bitbucketprb.pr", Arrays.asList("1.0"));
    }

    @Override
    public String getName() {
        return "Bitbucket PRB";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
        try {
            Properties props = BitbucketUtils.readPropertyFile();
            if (StringUtil.isEmpty(gitConfig.getUsername())) {
                gitConfig.setUsername(props.getProperty("login"));
            }
            if (StringUtil.isEmpty(gitConfig.getPassword())) {
                gitConfig.setPassword(props.getProperty("password"));
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public boolean isValidURL(String url) {
        return new URLUtils().isValidURL(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        try {
            BitbucketClient.builder()
                    .endPoint(this.bitbucketUrl)
                    .credentials(gitConfig.getUsername() + ":" + gitConfig.getPassword())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(String.format("check connection failed. %s", e.getMessage()), e);
        }
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
    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data) {
        data.put("PR_ID", prId);

        PullRequestStatus prStatus = null;
        boolean isDisabled = System.getProperty("go.plugin.bitbucket.pr.populate-details", "Y").equals("N");
        LOG.debug("Populating PR details is disabled");
        if (!isDisabled) {
            prStatus = getPullRequestStatus(gitConfig, prId, prSHA);
        }

        if (prStatus != null) {
            data.put("PR_BRANCH", String.valueOf(prStatus.getPrBranch()));
            data.put("TARGET_BRANCH", String.valueOf(prStatus.getToBranch()));
            data.put("PR_URL", String.valueOf(prStatus.getUrl()));
            data.put("PR_AUTHOR", prStatus.getAuthor());
            data.put("PR_AUTHOR_EMAIL", prStatus.getAuthorEmail());
            data.put("PR_DESCRIPTION", prStatus.getDescription());
            data.put("PR_TITLE", prStatus.getTitle());
        }
    }

    @Override
    public ScmPluginConfigurationView getScmConfigurationView() {
        return new DefaultScmPluginConfigurationView();
    }

    @Override
    public GeneralPluginConfigurationView getGeneralConfigurationView() {
        return new DefaultGeneralPluginConfigurationView();
    }

    private String parseRepository(String url){
        String [] split = url.split("/");
        return split[split.length - 1];
    }

    private PullRequestStatus getPullRequestStatus(GitConfig gitConfig, String prId, String prSHA) {
        try {
            PullRequest currentPR = pullRequestFrom(gitConfig, Integer.parseInt(prId));
            return transformBBPullRequestToPullRequestStatus(prSHA).apply(currentPR);
        } catch (Exception e) {
            // ignore
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * TODO. Get projectName and repository.
     */
    private PullRequest pullRequestFrom(GitConfig gitConfig, int currentPullRequestID) throws IOException {
        return BitbucketClient.builder()
                .endPoint(this.bitbucketUrl)
                .credentials(gitConfig.getUsername() + ":" + gitConfig.getPassword())
                .build()
                .api()
                .pullRequestApi()
                .get(this.projectName, parseRepository(gitConfig.getUrl()), currentPullRequestID);
    }

    private Function<PullRequest, PullRequestStatus> transformBBPullRequestToPullRequestStatus(final String mergedSHA) {
        return new Function<PullRequest, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(PullRequest input) {
                try {
                    return new PullRequestStatus(input.id(),
                            "",
                            mergedSHA,
                            input.fromRef().id(),
                            input.toRef().id(),
                            input.links().self().get(0).get("href"),
                            input.author().user().name(),
                            input.author().user().emailAddress(),
                            input.description(),
                            input.title());
                } catch (Error e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
