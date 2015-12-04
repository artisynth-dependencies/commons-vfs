/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.commons.vfs2.provider.webdav;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.config.Lookup;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.IgnoreSpecProvider;
import org.w3c.dom.Element;

import com.github.sardine.DavAcl;
import com.github.sardine.DavPrincipal;
import com.github.sardine.DavQuota;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;
import com.github.sardine.impl.SardineRedirectStrategy;
import com.github.sardine.impl.handler.MultiStatusResponseHandler;
import com.github.sardine.impl.methods.HttpPropFind;
import com.github.sardine.impl.methods.HttpPropPatch;
import com.github.sardine.impl.methods.HttpSearch;
import com.github.sardine.model.Acl;
import com.github.sardine.model.Allprop;
import com.github.sardine.model.Displayname;
import com.github.sardine.model.Group;
import com.github.sardine.model.Multistatus;
import com.github.sardine.model.Owner;
import com.github.sardine.model.PrincipalCollectionSet;
import com.github.sardine.model.PrincipalURL;
import com.github.sardine.model.Prop;
import com.github.sardine.model.Propertyupdate;
import com.github.sardine.model.Propfind;
import com.github.sardine.model.Propstat;
import com.github.sardine.model.QuotaAvailableBytes;
import com.github.sardine.model.QuotaUsedBytes;
import com.github.sardine.model.Remove;
import com.github.sardine.model.Resourcetype;
import com.github.sardine.model.Response;
import com.github.sardine.model.SearchRequest;
import com.github.sardine.util.SardineUtil;

/**
 * Provides direct access to the client, builder, and context, and allows host-relative
 * paths (since a host is provided).
 *
 * @author Antonio
 */
public class WebdavSardine extends SardineImpl implements Sardine
{

	private static final Logger log = Logger.getLogger(DavResource.class.getName());
	private static final String UTF_8 = "UTF-8";
	
    /**
     * HTTP client implementation
     */
    private CloseableHttpClient client;

    /**
     * HTTP client builder
     */
    private HttpClientBuilder builder;

    /**
     * Local context with authentication cache. Make sure the same context is used to execute
     * logically related requests.
     */
    protected HttpClientContext context;
    
    protected HttpHost host;
    
    protected ResponseHandler<Multistatus> responseHandler;

    /**
     * Access resources with no authentication
     */
    public WebdavSardine( HttpHost host, HttpClientBuilder clientBuilder )
    {
    	this(host, clientBuilder, HttpClientContext.create());
        
    }

    public HttpClientBuilder getClientBuilder()
    {
        return builder;
    }

    public HttpClientContext getClientContext()
    {
        return context;
    }

    public void updateClient()
    {
        client = builder.build();
    }
    
    public HttpHost getHost() 
    {
    	return host;
    }
    
    public void setHost(HttpHost host)
    {
    	this.host = host;
    }

    /**
     * Access resources with no authentication
     */
    public WebdavSardine( HttpHost host, HttpClientBuilder clientBuilder, HttpClientContext context )
    {
        this.builder = clientBuilder;
        this.context = context;
        // use redirect strategy to account for potentially changed scheme
        this.builder.setRedirectStrategy(new SardineRedirectStrategy());
        this.client = this.builder.build();
        this.host = host;
        this.responseHandler = new MultiStatusResponseHandler();
    }

    /**
     * @param username    Use in authentication header credentials
     * @param password    Use in authentication header credentials
     * @param domain      NTLM authentication
     * @param workstation NTLM authentication
     */
    @Override
    public void setCredentials( String username, String password, String domain, String workstation )
    {
        this.context.setCredentialsProvider( this.getCredentialsProvider( username, password, domain, workstation ) );
        this.context.setAttribute( HttpClientContext.TARGET_AUTH_STATE, new AuthState() );
    }

    private CredentialsProvider getCredentialsProvider( String username, String password, String domain,
                                                        String workstation )
    {
        CredentialsProvider provider = new BasicCredentialsProvider();
        if ( username != null )
        {
            provider.setCredentials(
                                     new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM,
                                                    AuthSchemes.NTLM ),
                                     new NTCredentials( username, password, workstation, domain ) );
            provider.setCredentials(
                                     new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM,
                                                    AuthSchemes.BASIC ),
                                     new UsernamePasswordCredentials( username, password ) );
            provider.setCredentials(
                                     new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM,
                                                    AuthSchemes.DIGEST ),
                                     new UsernamePasswordCredentials( username, password ) );
            provider.setCredentials(
                                     new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM,
                                                    AuthSchemes.SPNEGO ),
                                     new UsernamePasswordCredentials( username, password ) );
            provider.setCredentials(
                                     new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM,
                                                    AuthSchemes.KERBEROS ),
                                     new UsernamePasswordCredentials( username, password ) );
        }
        return provider;
    }

    /**
     * Adds handling of GZIP compression to the client.
     */
    @Override
    public void enableCompression()
    {
        this.builder.addInterceptorLast( new RequestAcceptEncoding() );
        this.builder.addInterceptorLast( new ResponseContentEncoding() );
        this.client = this.builder.build();
    }

    /**
     * Disable GZIP compression header.
     */
    @Override
    public void disableCompression()
    {
        this.builder.disableContentCompression();
        this.client = this.builder.build();
    }

    /**
     * Ignores cookies by always returning the IgnoreSpecFactory regardless of the cookieSpec value being looked up.
     */
    @Override
    public void ignoreCookies()
    {
        this.builder.setDefaultCookieSpecRegistry( new Lookup<CookieSpecProvider>()
        {
            @Override
            public CookieSpecProvider lookup( String name )
            {
                return new IgnoreSpecProvider();
            }
        } );
        this.client = this.builder.build();
    }

    @Override
    public void enablePreemptiveAuthentication( URL url )
    {
        final String host = url.getHost();
        final int port = url.getPort();
        final String protocol = url.getProtocol();
        final int httpPort;
        final int httpsPort;
        if ( "https".equals( protocol ) )
        {
            httpsPort = port;
            httpPort = -1;
        }
        else if ( "http".equals( protocol ) )
        {
            httpPort = port;
            httpsPort = -1;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported protocol " + protocol );
        }
        enablePreemptiveAuthentication( host, httpPort, httpsPort );
    }

    @Override
    public void enablePreemptiveAuthentication( String hostname, int httpPort, int httpsPort )
    {
        AuthCache cache = this.context.getAuthCache();
        if ( cache == null )
        {
            // Add AuthCache to the execution context
            cache = new BasicAuthCache();
            this.context.setAuthCache( cache );

        }
        // Generate Basic preemptive scheme object and stick it to the local execution context
        BasicScheme basicAuth = new BasicScheme();
        // Configure HttpClient to authenticate preemptively by prepopulating the authentication data cache.
        cache.put( new HttpHost( hostname, httpPort, "http" ), basicAuth );
        cache.put( new HttpHost( hostname, httpsPort, "https" ), basicAuth );
    }

    @Override
    public void disablePreemptiveAuthentication()
    {
        this.context.removeAttribute( HttpClientContext.AUTH_CACHE );
    }
    
    /**
     * Validate the response using the response handler. Aborts the request if there is an exception.
     *
     * @param <T>             Return type
     * @param request         Request to execute
     * @param responseHandler Determines the return type.
     * @return parsed response
     */
    protected <T> T execute( HttpRequestBase request, ResponseHandler<T> responseHandler )
        throws IOException
    {
        try
        {
            // Clear circular redirect cache
            this.context.removeAttribute( HttpClientContext.REDIRECT_LOCATIONS );
            // Execute with response handler
            HttpHost host = getHost();
            try {
                if (host != null) {
                	return this.client.execute( host, request, responseHandler, this.context );
                } else {
                	return this.client.execute( request, responseHandler, this.context );
                }
            } catch (IOException e) {
                System.out.println( "Error on " + request.getMethod() + " " +  host.toHostString() + request.getRequestLine().getUri() );
                throw e;
            }
        }
        catch ( HttpResponseException e )
        {
            // Don't abort if we get this exception, caller may want to repeat request.
            throw e;
        }
        catch ( IOException e )
        {
            request.abort();
            throw e;
        }
    }

    /**
     * No validation of the response. Aborts the request if there is an exception.
     *
     * @param request Request to execute
     * @return The response to check the reply status code
     */
    protected HttpResponse execute( HttpRequestBase request )
        throws IOException
    {
        try
        {
            // Clear circular redirect cache
            this.context.removeAttribute( HttpClientContext.REDIRECT_LOCATIONS );
            // Execute with no response handler
            HttpHost host = getHost();
            if (host != null) {
            	return this.client.execute( host, request, this.context );
            } else {
            	return this.client.execute(request, context);
            }
        }
        catch ( HttpResponseException e )
        {
            // Don't abort if we get this exception, caller may want to repeat request.
            throw e;
        }
        catch ( IOException e )
        {
            request.abort();
            throw e;
        }
    }
    
    /**
     * 
     * @return
     */
    protected Set<QName> getDefaultProps() {

        HashSet<QName> props = new HashSet<QName>();
        
        props.add( QName.valueOf( "response-charset" ) );
        
        return props;
    }
    
    @Override
    /**
     * Request a set of default properties
     */
    public List<DavResource> list(String url, int depth, boolean allProp) throws IOException
    {
        Set<QName> defaultProps = getDefaultProps();
        
        if (allProp) {
            Propfind body = new Propfind();
            body.setAllprop(new Allprop());
            return list(url, depth, body);
        } else {
            return list(url, depth, defaultProps);
        }
    }

    @Override
    public void shutdown()
        throws IOException
    {
        this.client.close();
    }
    
    public void setMultiStatusResponseHandler(ResponseHandler<Multistatus> handler) {
    	responseHandler = handler;
    }
    
    public ResponseHandler<Multistatus> getMultiStatusResponseHandler() {
    	if (responseHandler != null) {
    		return responseHandler;
    	} else {
    		return new MultiStatusResponseHandler();
    	}
    }
    
    
    /*
     * CODE COPIED FROM SARDINEIMPL
     */
    @Override
	public DavAcl getAcl(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("0");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setOwner(new Owner());
		prop.setGroup(new Group());
		prop.setAcl(new Acl());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			return new DavAcl(responses.get(0));
		}
	}
    
    @Override
	public List<String> getPrincipalCollectionSet(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("0");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setPrincipalCollectionSet(new PrincipalCollectionSet());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			List<String> collections = new ArrayList<String>();
			for (Response r : responses)
			{
				if (r.getPropstat() != null)
				{
					for (Propstat propstat : r.getPropstat())
					{
						if (propstat.getProp() != null
								&& propstat.getProp().getPrincipalCollectionSet() != null
								&& propstat.getProp().getPrincipalCollectionSet().getHref() != null)
						{
							collections.addAll(propstat.getProp().getPrincipalCollectionSet().getHref());
						}
					}
				}
			}
			return collections;
		}
	}
    
    @Override
	public List<DavPrincipal> getPrincipals(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("1");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setDisplayname(new Displayname());
		prop.setResourcetype(new Resourcetype());
		prop.setPrincipalURL(new PrincipalURL());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			List<DavPrincipal> collections = new ArrayList<DavPrincipal>();
			for (Response r : responses)
			{
				if (r.getPropstat() != null)
				{
					for (Propstat propstat : r.getPropstat())
					{
						if (propstat.getProp() != null
								&& propstat.getProp().getResourcetype() != null
								&& propstat.getProp().getResourcetype().getPrincipal() != null)
						{
							collections.add(new DavPrincipal(DavPrincipal.PrincipalType.HREF,
									r.getHref().get(0),
									propstat.getProp().getDisplayname().getContent().get(0)));
						}
					}
				}
			}
			return collections;
		}
	}
    
    @Override
	public DavQuota getQuota(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("0");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setQuotaAvailableBytes(new QuotaAvailableBytes());
		prop.setQuotaUsedBytes(new QuotaUsedBytes());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			return new DavQuota(responses.get(0));
		}
	}
    
    @Override
    protected List<DavResource> list(String url, int depth, Propfind body) throws IOException
    {
        HttpPropFind entity = new HttpPropFind(url);
        entity.setDepth(Integer.toString(depth));
        entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
        Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
        List<Response> responses = multistatus.getResponse();
        List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response response : responses)
		{
			try
			{
				resources.add(new DavResource(response));
			}
			catch (URISyntaxException e)
			{
				log.warning(String.format("Ignore resource with invalid URI %s", response.getHref().get(0)));
			}
		}
		return resources;
	}
    
    @Override
	public List<DavResource> patch(String url, List<Element> setProps, List<QName> removeProps) throws IOException
	{
		HttpPropPatch entity = new HttpPropPatch(url);
		// Build WebDAV <code>PROPPATCH</code> entity.
		Propertyupdate body = new Propertyupdate();
		// Add properties
		{
			com.github.sardine.model.Set set = new com.github.sardine.model.Set();
			body.getRemoveOrSet().add(set);
			Prop prop = new Prop();
			// Returns a reference to the live list
			List<Element> any = prop.getAny();
			for (Element element : setProps)
			{
				any.add(element);
			}
			set.setProp(prop);
		}
		// Remove properties
		{
			Remove remove = new Remove();
			body.getRemoveOrSet().add(remove);
			Prop prop = new Prop();
			// Returns a reference to the live list
			List<Element> any = prop.getAny();
			for (QName entry : removeProps)
			{
				Element element = SardineUtil.createElement(entry);
				any.add(element);
			}
			remove.setProp(prop);
		}
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response response : responses)
		{
			try
			{
				resources.add(new DavResource(response));
			}
			catch (URISyntaxException e)
			{
				log.warning(String.format("Ignore resource with invalid URI %s", response.getHref().get(0)));
			}
		}
		return resources;
	}
    
    public List<DavResource> search(String url, String language, String query) throws IOException
	{
		HttpEntityEnclosingRequestBase search = new HttpSearch(url);
		SearchRequest searchBody = new SearchRequest(language, query);
		String body = SardineUtil.toXml(searchBody);
		search.setEntity(new StringEntity(body, UTF_8));
		Multistatus multistatus = this.execute(search, getMultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response response : responses)
		{
			try
			{
				resources.add(new DavResource(response));
			}
			catch (URISyntaxException e)
			{
				log.warning(String.format("Ignore resource with invalid URI %s", response.getHref().get(0)));
			}
		}
		return resources;
	}
    

}
