package com.makeandship.gocd.bitbucket.api;
import java.io.IOException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class ApiClient {
	
	private HttpClientFactory factory;
	private Credentials credentials;
	private String owner;
    private String repositoryName;
    private static final String V2_API_BASE_URL = "https://bitbucket.org/api/2.0/repositories/";
    
    
    public <T extends HttpClientFactory> ApiClient(
            String username, String password, 
            String owner, String repositoryName,
            T httpFactory
        ) {
            this.credentials = new UsernamePasswordCredentials(username, password);
            this.owner = owner;
            this.repositoryName = repositoryName;       
            this.factory = httpFactory != null ? httpFactory : HttpClientFactory.INSTANCE;
        }
    
    public static class HttpClientFactory {    
        public static final HttpClientFactory INSTANCE = new HttpClientFactory();
        private static final int DEFAULT_TIMEOUT = 60000;
        
        
        public HttpClient getInstanceHttpClient(Credentials credentials) {
            HttpClient client = new HttpClient();

            HttpClientParams params = client.getParams();
            params.setConnectionManagerTimeout(DEFAULT_TIMEOUT);
            params.setSoTimeout(DEFAULT_TIMEOUT);

            //logger.log(Level.INFO, "Using proxy authentication (user={0})", username);
            client.getState().setProxyCredentials(AuthScope.ANY,
                		credentials);
            
            return client;
        }
    }
	
	public void checkConnection(){		
		String rootUrl = v2("/");
		try {
			Pullrequest.Repository repository = parse(get(rootUrl), new TypeReference<Pullrequest.Repository>() {});
			if (!this.repositoryName.equals(repository.getName())){
				throw new Exception("Invalid repository name");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Pullrequest getPullrequest(String id){
		String rootUrl = v2("/pullrequests/"+id);
		try {
			return parse(get(rootUrl), new TypeReference<Pullrequest>() {});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String v2(String path) {
		return v2(this.owner, this.repositoryName, path);
	}

	private String v2(String owner, String repositoryName, String path) {
        return V2_API_BASE_URL + owner + "/" + repositoryName + path;
    }
	
	private String get(String path) {
        return send(new GetMethod(path));
    }
	
	private String send(HttpMethodBase req) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);
        try {
            client.executeMethod(req);
            return req.getResponseBodyAsString();
        } catch (HttpException e) {
            //logger.log(Level.WARNING, "Failed to send request.", e);
            e.printStackTrace();
        } catch (IOException e) {
            //logger.log(Level.WARNING, "Failed to send request.", e);
            e.printStackTrace();
        } finally {
          req.releaseConnection();
        }
        return null;
    }
	
	private HttpClient getHttpClient() {
        return this.factory.getInstanceHttpClient(credentials);
    }
	
	private <R> R parse(String response, TypeReference<R> ref) throws IOException {
        return new ObjectMapper().readValue(response, ref);
    }
}
