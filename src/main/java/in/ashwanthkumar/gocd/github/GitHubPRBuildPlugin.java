package in.ashwanthkumar.gocd.github;

import com.makeandship.gocd.bitbucket.api.ApiClient;
import com.makeandship.gocd.bitbucket.api.Pullrequest;
import com.makeandship.gocd.bitbucket.api.Pullrequest.Response;
import com.makeandship.gocd.git.provider.bitbucket.GitCmdHelperBitBucket;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.ModifiedFile;
import com.tw.go.plugin.model.Revision;
import com.tw.go.plugin.util.ListUtil;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.settings.scm.PluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.BranchFilter;
import in.ashwanthkumar.gocd.github.util.GitFactory;
import in.ashwanthkumar.gocd.github.util.GitFolderFactory;
import in.ashwanthkumar.gocd.github.util.JSONUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;

import static in.ashwanthkumar.gocd.github.util.JSONUtils.fromJSON;
import static java.util.Arrays.asList;

@Extension
public class GitHubPRBuildPlugin implements GoPlugin {
    private static Logger LOGGER = Logger.getLoggerFor(GitHubPRBuildPlugin.class);

    public static final String EXTENSION_NAME = "scm";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_SCM_CONFIGURATION = "scm-configuration";
    public static final String REQUEST_SCM_VIEW = "scm-view";
    public static final String REQUEST_VALIDATE_SCM_CONFIGURATION = "validate-scm-configuration";
    public static final String REQUEST_CHECK_SCM_CONNECTION = "check-scm-connection";
    public static final String REQUEST_PLUGIN_CONFIGURATION = "go.plugin-settings.get-configuration";
    public static final String REQUEST_PLUGIN_VIEW = "go.plugin-settings.get-view";
    public static final String REQUEST_VALIDATE_PLUGIN_CONFIGURATION = "go.plugin-settings.validate-configuration";

    public static final String GET_PLUGIN_SETTINGS = "go.processor.plugin-settings.get";

    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISIONS_SINCE = "latest-revisions-since";
    public static final String REQUEST_CHECKOUT = "checkout";

    public static final String BRANCH_TO_REVISION_MAP = "BRANCH_TO_REVISION_MAP";
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int NOT_FOUND_RESPONSE_CODE = 404;
    public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;

    private Provider provider;
    private GitFactory gitFactory;
    private GitFolderFactory gitFolderFactory;
    private GoApplicationAccessor goApplicationAccessor;
    
    private String URL_ROOT = "https://bitbucket.org/%s";

    public GitHubPRBuildPlugin() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/defaults.properties"));

            Class<?> providerClass = Class.forName(properties.getProperty("provider"));
            Constructor<?> constructor = providerClass.getConstructor();
            provider = (Provider) constructor.newInstance();
            gitFactory = new GitFactory();
            gitFolderFactory = new GitFolderFactory();
        } catch (Exception e) {
            throw new RuntimeException("could not create provider", e);
        }
    }

    public GitHubPRBuildPlugin(Provider provider, GitFactory gitFactory, GitFolderFactory gitFolderFactory, GoApplicationAccessor goApplicationAccessor) {
        this.provider = provider;
        this.gitFactory = gitFactory;
        this.gitFolderFactory = gitFolderFactory;
        this.goApplicationAccessor = goApplicationAccessor;
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        this.goApplicationAccessor = goApplicationAccessor;
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
    	LOGGER.info("Start handling request: " + goPluginApiRequest.requestName());
        if (goPluginApiRequest.requestName().equals(REQUEST_SCM_CONFIGURATION)) {
            return handleSCMConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_SCM_VIEW)) {
            try {
                return handleSCMView();
            } catch (IOException e) {
                String message = "Failed to find template: " + e.getMessage();
                return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, message);
            }
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PLUGIN_CONFIGURATION)) {
            return handlePluginConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PLUGIN_VIEW)) {
            try {
                return handlePluginView();
            } catch (IOException e) {
                String message = "Failed to find template: " + e.getMessage();
                return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, message);
            }
        }  else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_PLUGIN_CONFIGURATION)) {
            return handlePluginValidation(goPluginApiRequest);
        }  else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_SCM_CONFIGURATION)) {
            return handleSCMValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_SCM_CONNECTION)) {
            return handleSCMCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION)) {
            return handleGetLatestRevision(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISIONS_SINCE)) {
            return handleLatestRevisionSince(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECKOUT)) {
            return handleCheckout(goPluginApiRequest);
        }
        return renderJSON(NOT_FOUND_RESPONSE_CODE, null);
    }

    private GoPluginApiResponse handlePluginValidation(GoPluginApiRequest goPluginApiRequest) {
        return renderJSON(SUCCESS_RESPONSE_CODE, Collections.emptyList());
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_NAME, goSupportedVersions);
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    private GoPluginApiResponse handlePluginView() throws IOException {
        return getPluginView(provider, provider.getGeneralConfigurationView());
    }

    private GoPluginApiResponse handlePluginConfiguration() {
        return getPluginConfiguration(provider.getGeneralConfigurationView());
    }

    private GoPluginApiResponse handleSCMView() throws IOException {
        return getPluginView(provider, provider.getScmConfigurationView());
    }

    private GoPluginApiResponse handleSCMConfiguration() {
        return getPluginConfiguration(provider.getScmConfigurationView());
    }

    private GoPluginApiResponse getPluginView(Provider provider, PluginConfigurationView view) throws IOException {
        if (view.hasConfigurationView()) {
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("displayValue", provider.getName());
            response.put("template", getFileContents(view.templateName()));
            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } else {
            return renderJSON(NOT_FOUND_RESPONSE_CODE, null);
        }
    }

    private GoPluginApiResponse getPluginConfiguration(PluginConfigurationView view) {
        Map<String, Object> response = view.fields();
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMValidation(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        final Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        final GitConfig gitConfig = getGitConfig(configuration);

        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        validate(response, new FieldValidator() {
            @Override
            public void validate(Map<String, Object> fieldValidation) {
                validateUrl(gitConfig, fieldValidation);
            }
        });
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String pathUrl = configuration.get("url");
        gitConfig.setUrl(pathUrl);

        Map<String, Object> response = new HashMap<String, Object>();
        List<String> messages = new ArrayList<String>();

        checkConnection(gitConfig, response, messages);

        if (response.get("status") == null) {
            response.put("status", "success");
            messages.add("Could connect to URL successfully");
        }
        response.put("messages", messages);
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }
    
    private void ResetCredentials(GitConfig gitConfig){
    	gitConfig.setUsername("");
        gitConfig.setPassword("");
    }

    public GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String pathUrl = configuration.get("url");
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.info(String.format("Flyweight: %s", flyweightFolder));

        try {
        	GitCmdHelperBitBucket git = new GitCmdHelperBitBucket(gitConfig, gitFolderFactory.create(flyweightFolder));
            ApiClient client = new ApiClient(gitConfig.getUsername(), gitConfig.getPassword(), pathUrl, null);
            Response<Pullrequest> responseApi = client.getPullrequest("");
            
            //realizar foreach
            /*for(Pullrequest pr : responseApi.getValues()){
            	String sourceBranchName = pr.getSource().getBranch().getName();
            	
            }*/
            
            if(responseApi.getValues() == null || responseApi.getValues().size() == 0){
            	LOGGER.info("No active PRs found.");
            	return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }
            
            Pullrequest pr = responseApi.getValues().get(0);
            String sourceBranchName = pr.getSource().getBranch().getName();
            
            ResetCredentials(gitConfig);
            //gitConfig.setUrl("https://bitbucket.org/easynvest/easynvest.framework");
            
            String refSpec = String.format(provider.getRefSpec(), sourceBranchName, sourceBranchName);
            gitConfig.setBranch(sourceBranchName);
            
            LOGGER.info("Cloning repository");
            git.cloneOrFetch(refSpec);
            LOGGER.info("Repository has been cloned");
            
            String refPattern = String.format(provider.getRefPattern(), sourceBranchName);
            Map<String, String> branchToRevisionMap = git.getBranchToRevisionMap(refPattern);
            LOGGER.info("branchToRevisionMap " + branchToRevisionMap);
            
            Revision revision = git.getLatestRevision();
            LOGGER.info("Get Latest Revision " + revision.getRevision());

            Map<String, Object> response = new HashMap<String, Object>();
            Map<String, Object> revisionMap = getRevisionMap(gitConfig, "develop", revision, pathUrl);
            LOGGER.info("getRevisionMap");
            
            response.put("revision", revisionMap);
            Map<String, String> scmDataMap = new HashMap<String, String>();
            scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(branchToRevisionMap));
            response.put("scm-data", scmDataMap);
            LOGGER.info(String.format("Triggered build for %s with head at %s", sourceBranchName, revision.getRevision()));
            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("get latest revision: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    public GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String pathUrl = configuration.get("url");
        Map<String, String> scmData = (Map<String, String>) requestBodyMap.get("scm-data");
        Map<String, String> oldBranchToRevisionMap = (Map<String, String>) fromJSON(scmData.get(BRANCH_TO_REVISION_MAP));
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.debug(String.format("Fetching latest for: %s", gitConfig.getUrl()));

        try {
        	LOGGER.info("flyweightFolder: " + flyweightFolder);
        	GitCmdHelperBitBucket git = new GitCmdHelperBitBucket(gitConfig, gitFolderFactory.create(flyweightFolder));
            ApiClient client = new ApiClient(gitConfig.getUsername(), gitConfig.getPassword(), pathUrl, null);
            
            LOGGER.info("Getting pull requests");
            Response<Pullrequest> responseApi = client.getPullrequest("");
            LOGGER.info("Pull requests: " + responseApi.getSize());
            
            if(responseApi.getValues() == null || responseApi.getValues().size() == 0){
            	LOGGER.info("No active PRs found.");
            	return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }
            
            Pullrequest pr = responseApi.getValues().get(0);
            String prId = pr.getId();
            String sourceBranchName = pr.getSource().getBranch().getName();
            
            ResetCredentials(gitConfig);
            //gitConfig.setUrl("https://bitbucket.org/easynvest/easynvest.framework");
            
            String refSpec = String.format(provider.getRefSpec(), sourceBranchName, sourceBranchName);
            gitConfig.setBranch(sourceBranchName);
            
            LOGGER.info("Cloning repository");
            git.cloneOrFetch(refSpec);
            LOGGER.info("Repository has been cloned");
            
            String refPattern = String.format(provider.getRefPattern(), sourceBranchName);
            Map<String, String> newBranchToRevisionMap = git.getBranchToRevisionMap(refPattern);
            LOGGER.info("branchToRevisionMap " + newBranchToRevisionMap);

            if (newBranchToRevisionMap.isEmpty()) {
                LOGGER.info("No active PRs found.");
                Map<String, Object> response = new HashMap<String, Object>();
                Map<String, String> scmDataMap = new HashMap<String, String>();
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(newBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }

            Map<String, String> newerRevisions = new HashMap<String, String>();

            BranchFilter branchFilter = provider
                    .getScmConfigurationView()
                    .getBranchFilter(configuration);

            for (String branch : newBranchToRevisionMap.keySet()) {
                if (branchFilter.isBranchValid(branch)) {
                    if (branchHasNewChange(oldBranchToRevisionMap.get(branch), newBranchToRevisionMap.get(branch))) {
                        // If there are any changes we should return the only one of them.
                        // Otherwise Go.CD skips other changes (revisions) in this call.
                        // You can think about it like if we always return a minimum item
                        // of a set with comparable items.
                        String newValue = newBranchToRevisionMap.get(branch);
                        newerRevisions.put(branch, newValue);
                        oldBranchToRevisionMap.put(branch, newValue);
                        break;
                    }
                } else {
                    LOGGER.debug(String.format("Branch %s is filtered by branch matcher", branch));
                }
            }

            if (newerRevisions.isEmpty()) {
                LOGGER.info(String.format("No updated PRs found. Old: %s New: %s", oldBranchToRevisionMap, newBranchToRevisionMap));

                Map<String, Object> response = new HashMap<String, Object>();
                Map<String, String> scmDataMap = new HashMap<String, String>();
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(newBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            } else {
                LOGGER.info(String.format("new commits: %d", newerRevisions.size()));
                
                List<Map> revisions = new ArrayList<Map>();
                for (String branch : newerRevisions.keySet()) {
                    String latestSHA = newerRevisions.get(branch);
                    Revision revision = git.getDetailsForRevision(latestSHA);

                    Map<String, Object> revisionMap = getRevisionMap(getGitConfig(configuration), prId, revision, pathUrl);
                    revisions.add(revisionMap);
                }
                Map<String, Object> response = new HashMap<String, Object>();
                response.put("revisions", revisions);
                Map<String, String> scmDataMap = new HashMap<String, String>();
                // We shouldn't return any new branches from newBranchToRevisionMap.
                // Instead of that, we can always return the previously modified map
                // (with a newly added or with changed and existing branch), because
                // it will be the same as there are no any changes
                // (see if (newerRevisions.isEmpty()) { ... } clause)
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(oldBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revisions since: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private boolean branchHasNewChange(String previousSHA, String latestSHA) {
        return previousSHA == null || !previousSHA.equals(latestSHA);
    }

    private GoPluginApiResponse handleCheckout(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String pathUrl = configuration.get("url");
        String destinationFolder = (String) requestBodyMap.get("destination-folder");
        Map<String, Object> revisionMap = (Map<String, Object>) requestBodyMap.get("revision");
        String revision = (String) revisionMap.get("revision");
        LOGGER.info(String.format("destination: %s. commit: %s", destinationFolder, revision));

        try {
        	GitCmdHelperBitBucket git = new GitCmdHelperBitBucket(gitConfig, gitFolderFactory.create(destinationFolder));
            ApiClient client = new ApiClient(gitConfig.getUsername(), gitConfig.getPassword(), pathUrl, null);
            Response<Pullrequest> responseApi = client.getPullrequest("");
            
            //realizar foreach
            /*for(Pullrequest pr : responseApi.getValues()){
            	String sourceBranchName = pr.getSource().getBranch().getName();
            	
            }*/
            
            if(responseApi.getValues() == null || responseApi.getValues().size() == 0){
            	LOGGER.info("No active PRs found.");
            	return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }
            
            Pullrequest pr = responseApi.getValues().get(0);
            String sourceBranchName = pr.getSource().getBranch().getName();
            
            ResetCredentials(gitConfig);
            //gitConfig.setUrl("https://bitbucket.org/easynvest/easynvest.framework");
            
            String refSpec = String.format(provider.getRefSpec(), sourceBranchName, sourceBranchName);
            gitConfig.setBranch(sourceBranchName);
            git.cloneOrFetch(refSpec);
            git.resetHard(revision);

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("status", "success");
            response.put("messages", Arrays.asList(String.format("Checked out to revision %s", revision)));

            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("checkout: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    public GitConfig getGitConfig(Map<String, String> configuration) {
        GitConfig gitConfig = new GitConfig(buildUrl(configuration.get("url")), configuration.get("username"), configuration.get("password"), null);
        provider.addConfigData(gitConfig);
        return gitConfig;
    }
    
    private String buildUrl(String path){
    	return String.format(URL_ROOT, path);
    }

    private void validate(List<Map<String, Object>> response, FieldValidator fieldValidator) {
        Map<String, Object> fieldValidation = new HashMap<String, Object>();
        fieldValidator.validate(fieldValidation);
        if (!fieldValidation.isEmpty()) {
            response.add(fieldValidation);
        }
    }

    public Map<String, Object> getRevisionMap(GitConfig gitConfig, String prId, Revision revision, String pathUrl) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("revision", revision.getRevision());
        response.put("user", revision.getUser());
        response.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(revision.getTimestamp()));
        response.put("revisionComment", revision.getComment());
        List<Map> modifiedFilesMapList = new ArrayList<Map>();
        if (!ListUtil.isEmpty(revision.getModifiedFiles())) {
            for (ModifiedFile modifiedFile : revision.getModifiedFiles()) {
                Map<String, String> modifiedFileMap = new HashMap<String, String>();
                modifiedFileMap.put("fileName", modifiedFile.getFileName());
                modifiedFileMap.put("action", modifiedFile.getAction());
                modifiedFilesMapList.add(modifiedFileMap);
            }
        }
        response.put("modifiedFiles", modifiedFilesMapList);
        Map<String, String> customDataBag = new HashMap<String, String>();
        gitConfig.setUrl(pathUrl);
        provider.populateRevisionData(gitConfig, prId, revision.getRevision(), customDataBag);
        response.put("data", customDataBag);
        return response;
    }

    private Map<String, String> keyValuePairs(Map<String, Object> requestBodyMap, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        Map<String, Object> fieldsMap = (Map<String, Object>) requestBodyMap.get(mainKey);
        for (String field : fieldsMap.keySet()) {
            Map<String, Object> fieldProperties = (Map<String, Object>) fieldsMap.get(field);
            String value = (String) fieldProperties.get("value");
            keyValuePairs.put(field, value);
        }
        return keyValuePairs;
    }

    public void validateUrl(GitConfig gitConfig, Map<String, Object> fieldMap) {
        if (StringUtil.isEmpty(gitConfig.getUrl())) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "URL is a required field");
        } else if (!provider.isValidURL(gitConfig.getUrl())) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "Invalid URL");
        }
    }

    public void checkConnection(GitConfig gitConfig, Map<String, Object> response, List<String> messages) {
    	LOGGER.info(String.format("Checking connection with URL: %s", gitConfig.getUrl()));
        if (StringUtil.isEmpty(gitConfig.getUrl())) {
            response.put("status", "failure");
            messages.add("URL is empty");
        } else {
            try {
                provider.checkConnection(gitConfig);
            } catch (Exception e) {
            	LOGGER.error("Error to check connection", e);
                response.put("status", "failure");
                messages.add(e.getMessage());
            }
        }
    }

    private String getFileContents(String filePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filePath), "UTF-8");
    }

    GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : JSONUtils.toJSON(response);
        return new GoPluginApiResponse() {
            @Override
            public int responseCode() {
                return responseCode;
            }

            @Override
            public Map<String, String> responseHeaders() {
                return null;
            }

            @Override
            public String responseBody() {
                return json;
            }
        };
    }

}
