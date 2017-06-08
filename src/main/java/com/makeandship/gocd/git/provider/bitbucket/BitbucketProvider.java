package com.makeandship.gocd.git.provider.bitbucket;

import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.provider.github.GHUtils;
import in.ashwanthkumar.gocd.github.provider.github.GitHubProvider;
import in.ashwanthkumar.gocd.github.settings.general.DefaultGeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.DefaultScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.ScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.URLUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.exec.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.makeandship.gocd.bitbucket.api.ApiClient;
import com.makeandship.gocd.bitbucket.api.Pullrequest;
import com.makeandship.gocd.bitbucket.api.Pullrequest.Response;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.ListUtil;
import com.tw.go.plugin.util.StringUtil;


public class BitbucketProvider implements Provider {

	private static final Logger LOG = LoggerFactory.getLogger(BitbucketProvider.class);
    // public static final String PR_FETCH_REFSPEC = "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*";
    // public static final String PR_MERGE_PREFIX = "refs/gh-merge/remotes/origin/";
    public static final String REF_SPEC = "%s:refs/remotes/origin/%s";
    public static final String REF_PATTERN = "refs/remotes/origin/%s";

    @Override
    public GoPluginIdentifier getPluginId() {
        return new GoPluginIdentifier("bitbucket.pr", Arrays.asList("1.0"));
    }

    @Override
    public String getName() {
        return "Bitbucket";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
        try {
            Properties props = GHUtils.readPropertyFile();
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
        	ApiClient client = new ApiClient(gitConfig.getUsername(), gitConfig.getPassword(), gitConfig.getUrl(), null);
        	client.checkConnection();
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
    	ApiClient client = new ApiClient(gitConfig.getUsername(), gitConfig.getPassword(), gitConfig.getUrl(), null);
    	
        data.put("PR_ID", prId);
        Pullrequest pr = null;
        
        boolean isDisabled = System.getProperty("go.plugin.bitbucket.pr.populate-details", "Y").equals("N");
        LOG.debug("Populating PR details is disabled");
        if (!isDisabled) {
        	Response<Pullrequest> responseApi = client.getPullrequest(prId);
        	if(responseApi != null && responseApi.getSize() > 0)
        		pr = responseApi.getValues().get(0);
        }

        if (pr != null) {
        	LOG.info("Populating PR details");
            data.put("PR_BRANCH", pr.getSource().getBranch().getName());
            data.put("TARGET_BRANCH", pr.getDestination().getBranch().getName());
            //data.put("PR_URL", String.valueOf(prStatus.getUrl()));
            data.put("PR_AUTHOR", pr.getAuthor().getUsername());
            //data.put("PR_AUTHOR_EMAIL", prStatus.getAuthorEmail());
            data.put("PR_DESCRIPTION", pr.getDescription());
            data.put("PR_TITLE", pr.getTitle());
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

}
