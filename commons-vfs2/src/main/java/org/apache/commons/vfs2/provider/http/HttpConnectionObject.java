package org.apache.commons.vfs2.provider.http;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;

/**
 * Class that stores the client, manager, and context
 * @author Antonio
 *
 */
public class HttpConnectionObject 
{
	private HttpClientConnectionManager manager;
	private HttpClientContext context;
	private HttpHost host;
	private HttpClient client;
	
	public HttpConnectionObject(HttpClientConnectionManager manager, 
			HttpClientContext context, 
			HttpHost host, 
			HttpClient client) 
	{
		this.manager = manager;
		this.context = context;
		this.host = host;
		this.client = client;
	}
	
	public HttpClientConnectionManager getConnectionManager() 
	{
		return manager;
	}
	
	public HttpClientContext getContext() 
	{
		return context;
	}
	
	public HttpHost getHost() 
	{
		return host;
	}
	
	public HttpClient getClient() 
	{
		return client;
	}
	
	/**
	 * Executes an Http request
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException 
	{
		HttpResponse response;
		if (context != null) {
			response = client.execute(host, request, context);
		} else {
			response = client.execute(host, request);
		}
		
		return response;
	}
	
	/**
	 * Shuts down the connection manager to close the connection
	 */
	public void shutdown() 
	{
		if (manager != null) {
			manager.shutdown();
		}
	}
	
}
