package com.makeandship.gocd.bitbucket.api;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import com.makeandship.gocd.bitbucket.api.Pullrequest.Response;
import com.makeandship.gocd.git.provider.bitbucket.BitbucketProvider;

import in.ashwanthkumar.gocd.github.GitHubPRBuildPlugin;
import in.ashwanthkumar.gocd.github.util.JSONUtils;

@SuppressWarnings("deprecation")
public class ApiClient {
	
	private HttpClientFactory factory;
	private UsernamePasswordCredentials credentials;
	private String url;
    private static Logger LOGGER = Logger.getLoggerFor(ApiClient.class);
    private String ROOT_URL_API = "https://bitbucket.org/api/2.0/repositories/%s";
    
    public <T extends HttpClientFactory> ApiClient(
            String username, String password, 
            String url,
            T httpFactory
        ) {
    		LOGGER.info("Initializing ApiClient.");
            this.credentials = new UsernamePasswordCredentials(username, password);
            this.url = String.format(ROOT_URL_API, url);       
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
		String response = null;
		String rootUrl = getUrl("/");
		LOGGER.info("Checking connection: getting URL " + rootUrl);
		LOGGER.info("Credentials: " + credentials.toString());
		try {
			response = get(rootUrl);
			LOGGER.info("Check connection web method response: " + response);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(response == null){
			throw new RuntimeException("check connection failed.");
		}
	}
	
	public Response<Pullrequest> getPullrequest(String id){
		String rootUrl = getUrl("/pullrequests/"+id);
		try {
			LOGGER.info("Calling URL: " + rootUrl);
			LOGGER.info(get(rootUrl));
			return parse(get(rootUrl), new TypeReference<Response<Pullrequest>>() {});
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
		setAuthHeader(req);
		
        try {
        	HttpResponse response = client.execute(req);
            HttpEntity entity = response.getEntity();
            checkStatusCode(response);
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            LOGGER.error("Error at send request: " + e.getMessage());
            e.printStackTrace();
        } catch (HttpException e){
        	LOGGER.error("Error at send HTTP request: " + e.getMessage());
            e.printStackTrace();
        } finally {
          req.releaseConnection();
        }
        return null;
    }
	
	private void checkStatusCode(HttpResponse response) throws HttpException {
		if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
			throw new HttpException("Error to execute HTTP request");
		}
	}

	private HttpClient getHttpClient() {
		return HttpClientBuilder.create().build();
    }
	
	private void setAuthHeader(HttpRequestBase req){
		String username;
		String password;
		try{
			Properties properties = new Properties();
	        properties.load(getClass().getResourceAsStream("/defaults.properties"));
	        username = properties.getProperty("bitbucket_username");
	        password = properties.getProperty("bitbucket_password");
		} catch (Exception e) {
	        throw new RuntimeException("could not set auth header", e);
	    }
		
		String auth = String.format("%s:%s", username, password);
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
        String authHeader = String.format("Basic %s", new String(encodedAuth));
		req.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
	}
	
	private <R> R parse(String response, TypeReference<R> ref) throws IOException {
        return new ObjectMapper().readValue(response, ref);
    }
}
