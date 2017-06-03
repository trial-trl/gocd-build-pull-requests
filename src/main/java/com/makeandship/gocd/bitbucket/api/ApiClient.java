package com.makeandship.gocd.bitbucket.api;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import com.thoughtworks.go.plugin.api.logging.Logger;

import com.google.gson.JsonObject;
import com.makeandship.gocd.git.provider.bitbucket.BitbucketProvider;

import in.ashwanthkumar.gocd.github.GitHubPRBuildPlugin;
import in.ashwanthkumar.gocd.github.util.JSONUtils;

@SuppressWarnings("deprecation")
public class ApiClient {
	
	private HttpClientFactory factory;
	private UsernamePasswordCredentials credentials;
	private String url;
    private static Logger LOGGER = Logger.getLoggerFor(ApiClient.class);
    
    
    public <T extends HttpClientFactory> ApiClient(
            String username, String password, 
            String url,
            T httpFactory
        ) {
    		LOGGER.info(String.format("Initializing ApiClient. Username: %s Password: %s URL: %s", username, password, url));
            this.credentials = new UsernamePasswordCredentials(username, password);
            this.url = url;       
            this.factory = httpFactory != null ? httpFactory : HttpClientFactory.INSTANCE;
        }
    
    public static class HttpClientFactory {    
        public static final HttpClientFactory INSTANCE = new HttpClientFactory();
        private static final int DEFAULT_TIMEOUT = 60000;
        
        
		public DefaultHttpClient getInstanceHttpClient(Credentials credentials) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, DEFAULT_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, DEFAULT_TIMEOUT);

            //logger.log(Level.INFO, "Using proxy authentication (user={0})", username);
            
            DefaultHttpClient client = new DefaultHttpClient(params);
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
            
            return client;
        }
    }
	
	public void checkConnection(){		
		String rootUrl = getUrl("/");
		LOGGER.info("Checking connection: getting URL " + rootUrl);
		LOGGER.info("Credentials: " + credentials.toString());
		try {
			String response = get(rootUrl);
			LOGGER.info("Check connection web method response: " + response);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Pullrequest getPullrequest(String id){
		String rootUrl = getUrl("/pullrequests/"+id);
		try {
			return parse(get(rootUrl), new TypeReference<Pullrequest>() {});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getUrl(String path) {
		return url + path;
	}
	
	private String get(String path) {
        return send(new HttpGet(path));
    }
	
	private String send(HttpRequestBase req) {
		HttpClient client = getHttpClient();
		setBasicAuth(req);
		
        try {
        	HttpResponse response = client.execute(req);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            LOGGER.error("Error at send request: " + e.getMessage());
            e.printStackTrace();
        } finally {
          req.releaseConnection();
        }
        return null;
    }
	
	private HttpClient getHttpClient() {
		return HttpClientBuilder.create().build();
    }
	
	private void setBasicAuth(HttpRequestBase req){
		req.addHeader(HttpHeaders.AUTHORIZATION, "Basic bHVpei5hc3N1bmNhb0BlYXN5bnZlc3QuY29tLmJyOlRpdHVsb0AyMDE2MQ==");
	}
	
	private <R> R parse(String response, TypeReference<R> ref) throws IOException {
        return new ObjectMapper().readValue(response, ref);
    }
}
