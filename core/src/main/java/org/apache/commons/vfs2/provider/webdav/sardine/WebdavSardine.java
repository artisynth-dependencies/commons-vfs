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
 * 
 *
 * Copyright 2009-2011 Jon Stevens et al.
 */
package org.apache.commons.vfs2.provider.webdav.sardine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.config.Lookup;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.IgnoreSpecProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Element;

import com.github.sardine.DavAce;
import com.github.sardine.DavAcl;
import com.github.sardine.DavPrincipal;
import com.github.sardine.DavQuota;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineRedirectStrategy;
import com.github.sardine.impl.handler.ExistsResponseHandler;
import com.github.sardine.impl.handler.LockResponseHandler;
import com.github.sardine.impl.handler.MultiStatusResponseHandler;
import com.github.sardine.impl.handler.VoidResponseHandler;
import com.github.sardine.impl.methods.HttpAcl;
import com.github.sardine.impl.methods.HttpCopy;
import com.github.sardine.impl.methods.HttpLock;
import com.github.sardine.impl.methods.HttpMkCol;
import com.github.sardine.impl.methods.HttpMove;
import com.github.sardine.impl.methods.HttpPropFind;
import com.github.sardine.impl.methods.HttpPropPatch;
import com.github.sardine.impl.methods.HttpSearch;
import com.github.sardine.impl.methods.HttpUnlock;
import com.github.sardine.model.Ace;
import com.github.sardine.model.Acl;
import com.github.sardine.model.Displayname;
import com.github.sardine.model.Exclusive;
import com.github.sardine.model.Group;
import com.github.sardine.model.Lockinfo;
import com.github.sardine.model.Lockscope;
import com.github.sardine.model.Locktype;
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
import com.github.sardine.model.Write;
import com.github.sardine.util.SardineUtil;

/**
 * Implementation of Sardine, heavily based on that by Jon Stevens et al.
 * Modified to allow relative requests, custom handlers, and versioning.
 * @author Antonio
 *
 */
public class WebdavSardine
implements Sardine, SardineExtended, HttpAwareSardine
{

    protected static final String UTF_8 = "UTF-8";

    // default settings
    protected static final int DEFAULT_LIST_DEPTH = 1;

    protected static final boolean DEFAULT_LIST_ALLPROP = false;

    protected static final boolean DEFAULT_LIST_PRETTY = false;

    protected static final int DEFAULT_PROPFIND_DEPTH = 0;

    protected static final boolean DEFAULT_PROPFIND_ALLPROP = false;

    protected static final boolean DEFAULT_PROPFIND_PRETTY = false;

    protected static final boolean DEFAULT_MOVE_OVERWRITE = true;

    protected static final boolean DEFAULT_COPY_OVERWRITE = true;

    // default handlers
    protected ResponseHandler<List<DavResource>> davResourceResponseHandler;

    protected ResponseHandler<HttpResponseInputStream> inputStreamResponseHandler;

    protected ResponseHandler<Void> voidResponseHandler;

    protected ResponseHandler<StatusLine> statusResponseHandler;

    protected ResponseHandler<Map<URI, Map<QName, String>>> propertyResponseHandler;

    /**
     * Custom Credentials Provider
     */
    CredentialsProvider customCredentials;

    /**
     * HTTP client builder
     */
    protected HttpClientBuilder builder;

    /**
     * HTTP client implementation
     */
    protected CloseableHttpClient client;

    /**
     * Local context with authentication cache. Make sure the same context is used to execute
     * logically related requests.
     */
    protected HttpClientContext context;

    /**
     * HttpHost for relative requests
     */
    protected HttpHost host;

    /**
     * Set of default properties to request
     */
    protected Set<QName> defaultProps;

    /**
     * Constructor with default context
     * @param host default host to use for relative url requests
     * @param clientBuilder builder to generate http clients
     */
    public WebdavSardine( HttpHost host, HttpClientBuilder clientBuilder )
    {
        this( host, clientBuilder, HttpClientContext.create() );
    }

    /**
     * Main constructor
     * @param host default hose for relative url requests
     * @param clientBuilder builder to generate http clients
     * @param context context to use for all connections
     */
    public WebdavSardine( HttpHost host, HttpClientBuilder clientBuilder, HttpClientContext context )
    {
        this.builder = clientBuilder;
        this.context = context;
        // use redirect strategy to account for potentially changed scheme
        this.builder.setRedirectStrategy( new SardineRedirectStrategy() );
        this.client = this.builder.build();
        this.host = host;
        this.defaultProps = new HashSet<QName>();
    }

    /**
     * Sets a default host to allow relative url requests
     * @param host default host
     */
    public void setDefaultHost( HttpHost host )
    {
        this.host = host;
    }

    /**
     * Returns the default host for relative url requests
     * @return the currently set default host
     */
    public HttpHost getDefaultHost()
    {
        return host;
    }

    /**
     * MUST BE CALLED IF CONTEXT CHANGES
     */
    private void updateCredentials()
    {
        if ( customCredentials != null )
        {
            context.setCredentialsProvider( customCredentials );
            context.setAttribute( HttpClientContext.TARGET_AUTH_STATE, new AuthState() );
        }
    }

    /**
     * MUST BE CALLED IF BUILDER CHANGES
     */
    private void refreshClient()
    {
        this.client = this.builder.build();
    }

    /**
     * @return Returns the custom credentials provider created using {@link #setCredentials(String, String, String, String)}
     */
    public CredentialsProvider getCredentialsProvider()
    {
        return customCredentials;
    }

    /**
     * Allows a custom set of default properties to request, always
     * @return the currently set default properties
     */
    public Set<QName> getDefaultProperties()
    {
        return defaultProps;
    }

    /**
     * Allows a custom set of default properties to request, always
     * @param set the set of default properties
     */
    public void setDefaultProperties( Set<QName> set )
    {
        defaultProps = new HashSet<QName>( set );
    }

    /**
     * Handler responsible for resource list responses.  Override to customize handler.
     * @return a handler for creating resource lists
     */
    protected ResponseHandler<List<DavResource>> getDavResourceResponseHandler()
    {
        if ( davResourceResponseHandler == null )
        {
            davResourceResponseHandler = new DavResourceResponseHandler();
        }
        return davResourceResponseHandler;
    }

    /**
     * Handler responsible for stream responses.  Override to customize handler.
     * @return a handler for extracting an input stream
     */
    protected ResponseHandler<HttpResponseInputStream> getInputStreamResponseHandler()
    {
        if ( inputStreamResponseHandler == null )
        {
            inputStreamResponseHandler = new InputStreamResponseHandler();
        }
        return inputStreamResponseHandler;
    }

    /**
     * Handler responsible for void responses.  Override to customize handler.
     * @return a handler for checking valid response, but not returning anything
     */
    protected ResponseHandler<Void> getVoidResponseHandler()
    {
        if ( voidResponseHandler == null )
        {
            voidResponseHandler = new VoidResponseHandler();
        }
        return voidResponseHandler;
    }

    /**
     * Handler responsible for resource status responses.  Override to customize handler.
     * @return a handler that returns the status line of a response
     */
    protected ResponseHandler<StatusLine> getStatusResponseHandler()
    {
        if ( statusResponseHandler == null )
        {
            statusResponseHandler = new StatusResponseHandler();
        }
        return statusResponseHandler;
    }

    /**
     * Handler for creating a map of resource to properties. Override to customize handler.
     * @return a handler that returns a collection of property maps
     */
    protected ResponseHandler<Map<URI, Map<QName, String>>> getPropertyResponseHandler()
    {
        if ( propertyResponseHandler == null )
        {
            propertyResponseHandler = new DavPropertiesResponseHandler();
        }
        return propertyResponseHandler;
    }

    @Override
    public <T> T list( String url, ResponseHandler<T> handler )
        throws IOException
    {
        return list( url, DEFAULT_LIST_DEPTH, handler );
    }

    @Override
    public <T> T list( String url, int depth, ResponseHandler<T> handler )
        throws IOException
    {
        return list( url, depth, getDefaultProperties(), handler );
    }

    @Override
    public <T> T list( String url, int depth, Set<QName> props, ResponseHandler<T> handler )
        throws IOException
    {
        return list( url, depth, props, DEFAULT_LIST_ALLPROP, handler );
    }

    /**
     * Gets a directory listing using WebDAV <code>PROPFIND</code>.
     *
     * @param url   Path to the resource including protocol and hostname
     * @param depth The depth to look at (use 0 for single ressource, 1 for directory listing)
     * @param props Additional properties which should be requested (excluding default props).
     * @param allProp whether to include the &lt;allprop/&gt; element in request body
     * @param handler for the returned <code>PROPFIND</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T list( String url, int depth, Set<QName> props, boolean allProp, ResponseHandler<T> handler )
        throws IOException
    {

        // create an HTML Body
        //        Propfind body = new Propfind();
        //        if (allProp) {
        //            body.setAllprop(new Allprop());
        //        } else {
        //            Prop prop = new Prop();
        //            body.setProp( prop );
        //
        //            // default properties required by all objects
        //            ObjectFactory objectFactory = new ObjectFactory();
        //            prop.setGetcontentlength(objectFactory.createGetcontentlength());
        //            prop.setGetlastmodified(objectFactory.createGetlastmodified());
        //            prop.setCreationdate(objectFactory.createCreationdate());
        //            prop.setDisplayname(objectFactory.createDisplayname());
        //            prop.setGetcontenttype(objectFactory.createGetcontenttype());
        //            prop.setResourcetype(objectFactory.createResourcetype());
        //            prop.setGetetag(objectFactory.createGetetag());
        //            
        //            // add custom properties
        //            if (props == null) {
        //                props = getDefaultProperties();
        //            } else {
        //                Set<QName> defaultProps = getDefaultProperties();
        //                if (defaultProps != null && defaultProps.size() > 0) {
        //                    // make copy and add defaults as well
        //                    props = new HashSet<QName>(props);
        //                    props.addAll( defaultProps );
        //                }
        //                
        //            }
        //            
        //            if (props != null && props.size() > 0) {
        //                List<Element> any = prop.getAny();
        //                for (QName entry : props) {
        //                    Element element = SardineUtil.createElement(entry);
        //                    any.add(element);
        //                }
        //            }
        //
        //        }
        //
        // String xml = SardineUtil.toXml( body );

        // build up properties
        HashSet<QName> nprops = new HashSet<QName>();
        Set<QName> dprops = getDefaultProperties();
        if ( dprops != null )
        {
            nprops.addAll( dprops );
        }
        if ( props != null )
        {
            nprops.addAll( props );
        }

        // we need at least the defaults to build up a resource
        if ( !allProp )
        {
            nprops.addAll( Arrays.asList( new QName[] {
                DavResources.GETCONTENTLENGTH,
                DavResources.GETLASTMODIFIED,
                DavResources.CREATIONDATE,
                DavResources.DISPLAYNAME,
                DavResources.GETCONTENTTYPE,
                DavResources.RESOURCETYPE,
                DavResources.GETETAG } ) );

        }

        String xml = WebdavXMLBuilder.buildPropfind( allProp, nprops, DEFAULT_LIST_PRETTY );

        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( Integer.toString( depth ) );
        entity.setEntity( new StringEntity( xml, UTF_8 ) );
        return execute( entity, handler );
    }

    @Override
    public List<DavResource> list( String url )
        throws IOException
    {
        return list( url, DEFAULT_LIST_DEPTH );
    }

    @Override
    public List<DavResource> list( String url, int depth )
        throws IOException
    {
        return list( url, depth, DEFAULT_LIST_ALLPROP );
    }

    @Override
    public List<DavResource> list( String url, int depth, Set<QName> props )
        throws IOException
    {
        ResponseHandler<List<DavResource>> handler = getDavResourceResponseHandler();
        return list( url, depth, props, DEFAULT_LIST_ALLPROP, handler );
    }

    @Override
    public List<DavResource> list( String url, int depth, boolean allProp )
        throws IOException
    {
        ResponseHandler<List<DavResource>> handler = getDavResourceResponseHandler();
        return list( url, depth, null, allProp, handler );
    }

    /**
     * Gets a directory listing using WebDAV <code>PROPFIND</code>.
     *
     * @param url   Path to the resource including protocol and hostname
     * @param depth The depth to look at (use 0 for single ressource, 1 for directory listing)
     * @param props Additional properties which should be requested (excluding default props).
     * @param allProp whether to include the &lt;allprop/&gt; element in request body
     * @return corresponding resource list
     * @throws IOException I/O error or HTTP response validation failure
     */
    public List<DavResource> list( String url, int depth, Set<QName> props, boolean allProp )
        throws IOException
    {
        ResponseHandler<List<DavResource>> handler = getDavResourceResponseHandler();
        return list( url, depth, props, allProp, handler );
    }

    /**
     * Executes the <code>PROPFIND</code> request to find only particular properties
     * @param url Path to the resource including protocol and hostname
     * @param props set of properties to request
     * @param allprop adds the &lt;allprop/&gt; element to <code>PROPFIND</code>
     * @param handler for the returned <code>PROPFIND</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T getProperties( String url, Set<QName> props, boolean allprop, ResponseHandler<T> handler )
        throws IOException
    {
        //        // create an HTML Body
        //        Propfind body = new Propfind();
        //
        //        if ( allprop )
        //        {
        //            body.setAllprop( new Allprop() );
        //        }
        //        else
        //        {
        //            Prop prop = null;
        //
        //            // add custom properties
        //            if ( props == null )
        //            {
        //                props = getDefaultProperties();
        //            }
        //            else
        //            {
        //                Set<QName> defaultProps = getDefaultProperties();
        //                if ( defaultProps != null && defaultProps.size() > 0 )
        //                {
        //                    // make copy and add defaults as well
        //                    props = new HashSet<QName>( props );
        //                    props.addAll( defaultProps );
        //                }
        //            }
        //
        //            if ( props != null && props.size() > 0 )
        //            {
        //                if ( prop == null )
        //                {
        //                    prop = new Prop();
        //                    body.setProp( prop );
        //                }
        //                List<Element> any = prop.getAny();
        //                for ( QName entry : props )
        //                {
        //                    Element element = SardineUtil.createElement( entry );
        //                    any.add( element );
        //                }
        //            }
        //        }
        //       String xml = SardineUtil.toXml( body );

        HashSet<QName> nprops = new HashSet<QName>();
        Set<QName> dprops = getDefaultProperties();
        if ( dprops != null )
        {
            nprops.addAll( dprops );
        }
        if ( props != null )
        {
            nprops.addAll( props );
        }
        String xml = WebdavXMLBuilder.buildPropfind( allprop, nprops, DEFAULT_PROPFIND_PRETTY );

        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( "0" );
        entity.setEntity( new StringEntity( xml, UTF_8 ) );
        return execute( entity, handler );
    }

    /**
     * Check if the two URIs have same path/query, indicating identical files
     * We have to account for a potential discrepancy with a trailing slash
     * @param a first URI
     * @param b second URI
     * @return true if corresponding paths and queries equal (up to trailing slash)
     */
    private boolean pathsEqual( URI a, URI b )
    {

        if ( a.equals( b ) )
        {
            return true;
        }

        String pa = a.getPath();
        String pb = b.getPath();

        // maybe add slash
        if ( !pa.endsWith( "/" ) && pb.endsWith( "/" ) )
        {
            pa = pa + "/";
        }
        else if ( !pb.endsWith( "/" ) && pa.endsWith( "/" ) )
        {
            pb = pb + "/";
        }

        if ( !pa.equals( pb ) )
        {
            return false;
        }
        String qa = a.getQuery();
        String qb = b.getQuery();

        if ( qa != null && !qa.equals( qb ) )
        {
            return false;
        }
        else if ( qb != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public Map<QName, String> getProperties( String url, Set<QName> props )
        throws IOException
    {
        return getProperties( url, props, DEFAULT_PROPFIND_ALLPROP );
    }

    @Override
    public Map<QName, String> getProperties( String url, boolean allprop )
        throws IOException
    {
        return getProperties( url, null, allprop );
    }

    public Map<QName, String> getProperties( String url, Set<QName> props, boolean allprop )
        throws IOException
    {
        Map<URI, Map<QName, String>> propMap = getProperties( url, props, allprop, getPropertyResponseHandler() );

        // find the one related to provided resource
        if ( propMap.size() == 1 )
        {
            return propMap.entrySet().iterator().next().getValue();
        }

        try
        {
            URI inputURI = new URI( url );
            // look for one related to provided url
            for ( Entry<URI, Map<QName, String>> entry : propMap.entrySet() )
            {
                if ( pathsEqual( inputURI, entry.getKey() ) )
                {
                    return entry.getValue();
                }
            }
        }
        catch ( URISyntaxException e )
        {
        }

        // try with strings
        for ( Entry<URI, Map<QName, String>> entry : propMap.entrySet() )
        {
            String keyStr = entry.getKey().toString();
            if ( url.endsWith( "/" ) && !keyStr.endsWith( "/" ) )
            {
                keyStr = keyStr + "/";
            }
            else if ( !url.endsWith( "/" ) && keyStr.endsWith( "/" ) )
            {
                keyStr = keyStr.substring( 0, keyStr.length() - 1 );
            }

            if ( keyStr.endsWith( url ) )
            {
                return entry.getValue();
            }
        }

        return null;
    }

    @Override
    public <T> T search( String url, String language, String query, ResponseHandler<T> handler )
        throws IOException
    {
        HttpSearch search = new HttpSearch( url );
        SearchRequest searchBody = new SearchRequest( language, query );
        String body = SardineUtil.toXml( searchBody );
        search.setEntity( new StringEntity( body, UTF_8 ) );

        return execute( search, handler );
    }

    @Override
    public List<DavResource> search( String url, String language, String query )
        throws IOException
    {
        ResponseHandler<List<DavResource>> handler = getDavResourceResponseHandler();
        return search( url, language, query, handler );
    }

    @Override
    public <T> T patch( String url, Map<QName, String> setProps, ResponseHandler<T> handler )
        throws IOException
    {
        return patch( url, setProps, null, handler );
    }

    @Override
    public <T> T patch( String url, Map<QName, String> setProps, List<QName> removeProps, ResponseHandler<T> handler )
        throws IOException
    {
        List<Element> setPropsElements = new ArrayList<Element>();

        if ( setProps != null )
        {
            for ( Entry<QName, String> entry : setProps.entrySet() )
            {
                Element element = SardineUtil.createElement( entry.getKey() );
                element.setTextContent( entry.getValue() );
                setPropsElements.add( element );
            }
        }
        return patch( url, setPropsElements, removeProps, handler );
    }

    @Override
    public <T> T patch( String url, List<Element> setProps, List<QName> removeProps, ResponseHandler<T> handler )
        throws IOException
    {
        HttpPropPatch proppatch = new HttpPropPatch( url );

        // Build WebDAV <code>PROPPATCH</code> entity.
        Propertyupdate body = new Propertyupdate();
        // Add properties
        if ( setProps != null )
        {
            com.github.sardine.model.Set set = new com.github.sardine.model.Set();
            body.getRemoveOrSet().add( set );
            Prop prop = new Prop();
            // Returns a reference to the live list
            List<Element> any = prop.getAny();
            for ( Element element : setProps )
            {
                any.add( element );
            }
            set.setProp( prop );
        }

        // Remove properties
        if ( removeProps != null )
        {
            Remove remove = new Remove();
            body.getRemoveOrSet().add( remove );
            Prop prop = new Prop();
            // Returns a reference to the live list
            List<Element> any = prop.getAny();
            for ( QName entry : removeProps )
            {
                Element element = SardineUtil.createElement( entry );
                any.add( element );
            }
            remove.setProp( prop );
        }
        proppatch.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );

        return execute( proppatch, handler );

    }

    @Override
    public List<DavResource> patch( String url, Map<QName, String> setProps )
        throws IOException
    {
        return patch( url, setProps, (List<QName>) null );
    }

    @Override
    public List<DavResource> patch( String url, Map<QName, String> setProps, List<QName> removeProps )
        throws IOException
    {
        List<Element> setPropsElements = new ArrayList<Element>();

        if ( setProps != null )
        {
            for ( Entry<QName, String> entry : setProps.entrySet() )
            {
                Element element = SardineUtil.createElement( entry.getKey() );
                element.setTextContent( entry.getValue() );
                setPropsElements.add( element );
            }
        }

        return patch( url, setPropsElements, removeProps );
    }

    @Override
    public List<DavResource> patch( String url, List<Element> setProps, List<QName> removeProps )
        throws IOException
    {
        ResponseHandler<List<DavResource>> handler = getDavResourceResponseHandler();
        return patch( url, setProps, removeProps, handler );
    }

    @Override
    public <T> T get( String url, ResponseHandler<T> handler )
        throws IOException
    {
        return get( url, (List<Header>) null, handler );
    }

    @Override
    public <T> T get( String url, Map<String, String> headers, ResponseHandler<T> handler )
        throws IOException
    {
        List<Header> list = new ArrayList<Header>();
        if ( headers != null )
        {
            for ( Map.Entry<String, String> h : headers.entrySet() )
            {
                list.add( new BasicHeader( h.getKey(), h.getValue() ) );
            }
        }
        return get( url, list, handler );
    }

    public <T> T get( String url, List<Header> headers, ResponseHandler<T> handler )
        throws IOException
    {

        HttpGet get = new HttpGet( url );
        if ( headers != null )
        {
            for ( Header header : headers )
            {
                get.addHeader( header );
            }
        }

        T out = null;
        try
        {
            // execute without handler so the client's "execute" doesn't consume entire response
            HttpResponse response = execute( get );

            // now handle response separately
            out = handler.handleResponse( response );
        }
        catch ( IOException e )
        {
            get.abort();
            throw e;
        }

        return out;
    }

    @Override
    public HttpResponseInputStream get( String url )
        throws IOException
    {
        return get( url, (Map<String, String>) null );
    }

    @Override
    public HttpResponseInputStream get( String url, Map<String, String> headers )
        throws IOException
    {
        ResponseHandler<HttpResponseInputStream> handler = getInputStreamResponseHandler();
        return get( url, headers, handler );
    }

    @Override
    public <T> T put( String url, byte[] data, ResponseHandler<T> handler )
        throws IOException
    {
        return put( url, data, (String) null, handler );
    }

    @Override
    public <T> T put( String url, byte[] data, String contentType, ResponseHandler<T> handler )
        throws IOException
    {
        ByteArrayEntity entity = new ByteArrayEntity( data );
        return put( url, entity, contentType, true, handler );
    }

    @Override
    public <T> T put( String url, InputStream dataStream, ResponseHandler<T> handler )
        throws IOException
    {
        return put( url, dataStream, (String) null, handler );
    }

    @Override
    public <T> T put( String url, InputStream dataStream, String contentType, ResponseHandler<T> handler )
        throws IOException
    {
        return put( url, dataStream, contentType, true, handler );
    }

    @Override
    public <T> T put( String url, InputStream dataStream, String contentType, boolean expectContinue,
                      ResponseHandler<T> handler )
                          throws IOException
    {
        // A length of -1 means "go until end of stream"
        return put( url, dataStream, contentType, expectContinue, -1, handler );
    }

    @Override
    public <T> T put( String url, InputStream dataStream, String contentType, boolean expectContinue,
                      long contentLength, ResponseHandler<T> handler )
                          throws IOException
    {
        InputStreamEntity entity = new InputStreamEntity( dataStream, contentLength );
        return put( url, entity, contentType, expectContinue, handler );
    }

    @Override
    public <T> T put( String url, InputStream dataStream, Map<String, String> headers, ResponseHandler<T> handler )
        throws IOException
    {
        List<Header> list = new ArrayList<Header>();
        if ( headers != null )
        {
            for ( Map.Entry<String, String> h : headers.entrySet() )
            {
                list.add( new BasicHeader( h.getKey(), h.getValue() ) );
            }
        }
        return put( url, dataStream, list, handler );
    }

    public <T> T put( String url, InputStream dataStream, List<Header> headers, ResponseHandler<T> handler )
        throws IOException
    {
        // A length of -1 means "go until end of stream"
        InputStreamEntity entity = new InputStreamEntity( dataStream, -1 );
        return put( url, entity, headers, handler );
    }

    @Override
    public <T> T put( String url, File localFile, String contentType, ResponseHandler<T> handler )
        throws IOException
    {
        return put( url, localFile, contentType, false, handler );
    }

    @Override
    public <T> T put( String url, File localFile, String contentType, boolean expectContinue,
                      ResponseHandler<T> handler )
                          throws IOException
    {
        FileEntity content = new FileEntity( localFile );
        return put( url, content, contentType, expectContinue, handler );
    }

    public <T> T put( String url, HttpEntity entity, String contentType, boolean expectContinue,
                      ResponseHandler<T> handler )
                          throws IOException
    {
        List<Header> headers = new ArrayList<Header>();
        if ( contentType != null )
        {
            headers.add( new BasicHeader( HttpHeaders.CONTENT_TYPE, contentType ) );
        }
        if ( expectContinue )
        {
            headers.add( new BasicHeader( HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE ) );
        }
        return put( url, entity, headers, handler );
    }

    @Override
    public void put( String url, byte[] data )
        throws IOException
    {
        put( url, data, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, InputStream dataStream )
        throws IOException
    {
        put( url, dataStream, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, byte[] data, String contentType )
        throws IOException
    {
        put( url, data, contentType, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, InputStream dataStream, String contentType )
        throws IOException
    {
        put( url, dataStream, contentType, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, InputStream dataStream, String contentType, boolean expectContinue )
        throws IOException
    {
        put( url, dataStream, contentType, expectContinue, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, InputStream dataStream, String contentType, boolean expectContinue,
                     long contentLength )
                         throws IOException
    {
        put( url, dataStream, contentType, expectContinue, contentLength, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, InputStream dataStream, Map<String, String> headers )
        throws IOException
    {
        put( url, dataStream, headers, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, File localFile, String contentType )
        throws IOException
    {
        put( url, localFile, contentType, getVoidResponseHandler() );
    }

    @Override
    public void put( String url, File localFile, String contentType, boolean expectContinue )
        throws IOException
    {
        put( url, localFile, contentType, expectContinue, getVoidResponseHandler() );
    }

    /**
     * Executes a <code>PUT</code> request with a given entity
     * @param url Path to the resource including protocol and hostname
     * @param entity HTML body for request
     * @param headers set of custom headers on request
     * @param handler for the returned <code>PROPFIND</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T put( String url, HttpEntity entity, List<Header> headers, ResponseHandler<T> handler )
        throws IOException
    {
        HttpPut put = new HttpPut( url );
        put.setEntity( entity );
        for ( Header header : headers )
        {
            put.addHeader( header );
        }
        if ( entity.getContentType() == null && !put.containsHeader( HttpHeaders.CONTENT_TYPE ) )
        {
            put.addHeader( HttpHeaders.CONTENT_TYPE, HTTP.DEF_CONTENT_CHARSET.name() );
        }
        try
        {
            return this.execute( put, handler );
        }
        catch ( HttpResponseException e )
        {
            if ( e.getStatusCode() == HttpStatus.SC_EXPECTATION_FAILED )
            {
                // Retry with the Expect header removed
                put.removeHeaders( HTTP.EXPECT_DIRECTIVE );
                if ( entity.isRepeatable() )
                {
                    // try again
                    return this.execute( put, handler );
                }
            }
            throw e;
        }
    }

    @Override
    public void delete( String url )
        throws IOException
    {
        delete( url, getVoidResponseHandler() );
    }

    @Override
    public <T> T delete( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpDelete delete = new HttpDelete( url );
        return execute( delete, handler );
    }

    @Override
    public void createDirectory( String url )
        throws IOException
    {
        createDirectory( url, getVoidResponseHandler() );
    }

    @Override
    public <T> T createDirectory( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpMkCol mkcol = new HttpMkCol( url );
        return execute( mkcol, handler );
    }

    @Override
    public <T> T move( String sourceUrl, String destinationUrl, ResponseHandler<T> handler )
        throws IOException
    {
        return move( sourceUrl, destinationUrl, DEFAULT_MOVE_OVERWRITE, handler );
    }

    @Override
    public void move( String sourceUrl, String destinationUrl )
        throws IOException
    {
        move( sourceUrl, destinationUrl, getVoidResponseHandler() );
    }

    @Override
    public void move( String sourceUrl, String destinationUrl, boolean overwrite )
        throws IOException
    {
        move( sourceUrl, destinationUrl, overwrite, getVoidResponseHandler() );
    }

    @Override
    public <T> T move( String sourceUrl, String destinationUrl, boolean overwrite, ResponseHandler<T> handler )
        throws IOException
    {
        HttpMove move = new HttpMove( sourceUrl, destinationUrl, overwrite );
        return execute( move, handler );
    }

    @Override
    public <T> T copy( String sourceUrl, String destinationUrl, ResponseHandler<T> handler )
        throws IOException
    {
        return copy( sourceUrl, destinationUrl, DEFAULT_COPY_OVERWRITE, handler );
    }

    @Override
    public void copy( String sourceUrl, String destinationUrl )
        throws IOException
    {
        copy( sourceUrl, destinationUrl, getVoidResponseHandler() );
    }

    @Override
    public void copy( String sourceUrl, String destinationUrl, boolean overwrite )
        throws IOException
    {
        copy( sourceUrl, destinationUrl, overwrite, getVoidResponseHandler() );
    }

    @Override
    public <T> T copy( String sourceUrl, String destinationUrl, boolean overwrite, ResponseHandler<T> handler )
        throws IOException
    {
        HttpCopy copy = new HttpCopy( sourceUrl, destinationUrl, overwrite );
        return execute( copy, handler );
    }

    @Override
    public <T> T execute( HttpRequest request, ResponseHandler<T> handler )
        throws IOException
    {
        try
        {
            // Clear circular redirect cache
            this.context.removeAttribute( HttpClientContext.REDIRECT_LOCATIONS );
            // Execute with response handler
            HttpHost host = getDefaultHost();
            return this.client.execute( host, request, handler, this.context );
        }
        catch ( HttpResponseException e )
        {
            // Don't abort if we get this exception, caller may want to repeat request.
            throw e;
        }
        catch ( IOException e )
        {
            if ( request instanceof HttpUriRequest )
            {
                ( (HttpUriRequest) request ).abort();
            }
            throw e;
        }
    }

    @Override
    public HttpResponse execute( HttpRequest request )
        throws IOException
    {
        try
        {
            // Clear circular redirect cache
            this.context.removeAttribute( HttpClientContext.REDIRECT_LOCATIONS );
            // Execute with response handler
            HttpHost host = getDefaultHost();
            return this.client.execute( host, request, this.context );
        }
        catch ( HttpResponseException e )
        {
            // Don't abort if we get this exception, caller may want to repeat request.
            throw e;
        }
        catch ( IOException e )
        {
            if ( request instanceof HttpUriRequest )
            {
                ( (HttpUriRequest) request ).abort();
            }
            throw e;
        }
    }

    @Override
    public boolean createVersion( String url )
    {
        try
        {
            StatusLine status = createVersion( url, getStatusResponseHandler() );
            return status.getStatusCode() == HttpStatus.SC_OK;
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    
    /**
     * Creates a version-controlled resource at the requested url by sending
     * a <code>VERSION-CONTROL</code> request
     * @param url Path to the resource including protocol and hostname
     * @param handler for the <code>VERSION-CONTROL</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T createVersion( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpVersionControl request = new HttpVersionControl( url );
        return execute( request, handler );
    }

    @Override
    public void checkout( String url )
        throws IOException
    {
        checkout( url, getVoidResponseHandler() );
    }

    /**
     * Checkout a version of a resource by sending
     * a <code>CHECKOUT</code> request
     * @param url Path to the resource including protocol and hostname
     * @param handler for the <code>CHECKOUT</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T checkout( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpCheckout request = new HttpCheckout( url );
        return execute( request, handler );
    }

    @Override
    public void uncheckout( String url )
        throws IOException
    {
        uncheckout( url, getVoidResponseHandler() );
    }

    /**
     * Uncheckout a version of a resource by sending
     * a <code>UNCHECKOUT</code> request
     * @param url Path to the resource including protocol and hostname
     * @param handler for the <code>UNCHECKOUT</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T uncheckout( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpUncheckout request = new HttpUncheckout( url );
        return execute( request, handler );
    }

    @Override
    public void checkin( String url )
        throws IOException
    {
        checkin( url, getVoidResponseHandler() );
    }

    /**
     * Checkin a version of a resource by sending
     * a <code>CHECKIN</code> request
     * @param url Path to the resource including protocol and hostname
     * @param handler for the <code>CHECKIN</code> response
     * @param <T> return type for the handler
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T checkin( String url, ResponseHandler<T> handler )
        throws IOException
    {
        HttpCheckin request = new HttpCheckin( url );
        return execute( request, handler );
    }

    @Override
    public void setCredentials( String username, String password )
    {
        setCredentials( username, password, "", "" );
    }

    @Override
    public void setCredentials( String username, String password, String domain, String workstation )
    {
        customCredentials = createCredentialsProvider( username, password, domain, workstation );
        updateCredentials();
    }

    /**
     * Creates a new credentials provider
     * @param username username
     * @param password password
     * @param domain workspace domain
     * @param workstation workspace station
     * @return a new credentials provider with the supplied stats
     */
    protected CredentialsProvider createCredentialsProvider( String username, String password, String domain,
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

    @Override
    @Deprecated
    public List<DavResource> getResources( String url )
        throws IOException
    {
        return list( url );
    }

    @Override
    @Deprecated
    public void setCustomProps( String url, Map<String, String> setProps, List<String> removeProps )
        throws IOException
    {
        patch( url, SardineUtil.toQName( setProps ), SardineUtil.toQName( removeProps ) );
    }

    @Override
    public boolean exists( String url )
        throws IOException
    {
        // PropFind (owncloud 8 chokes on Head, Ubuntu seems to only send PropFind)
        //    HttpHead request = new HttpHead( url );
        HttpPropFind request = new HttpPropFind( url );
        return this.execute( request, new ExistsResponseHandler() );
    }

    @Override
    public String lock( String url )
        throws IOException
    {
        HttpLock entity = new HttpLock( url );
        Lockinfo body = new Lockinfo();
        Lockscope scopeType = new Lockscope();
        scopeType.setExclusive( new Exclusive() );
        body.setLockscope( scopeType );
        Locktype lockType = new Locktype();
        lockType.setWrite( new Write() );
        body.setLocktype( lockType );
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        // Return the lock token
        return this.execute( entity, new LockResponseHandler() );
    }

    @Override
    public String refreshLock( String url, String token, String file )
        throws IOException
    {
        HttpLock entity = new HttpLock( url );
        entity.setHeader( "If", "<" + file + "> (<" + token + ">)" );
        return this.execute( entity, new LockResponseHandler() );
    }

    @Override
    public void unlock( String url, String token )
        throws IOException
    {
        HttpUnlock entity = new HttpUnlock( url, token );
        Lockinfo body = new Lockinfo();
        Lockscope scopeType = new Lockscope();
        scopeType.setExclusive( new Exclusive() );
        body.setLockscope( scopeType );
        Locktype lockType = new Locktype();
        lockType.setWrite( new Write() );
        body.setLocktype( lockType );
        this.execute( entity, getVoidResponseHandler() );
    }

    @Override
    public DavAcl getAcl( String url )
        throws IOException
    {
        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( "0" );
        Propfind body = new Propfind();
        Prop prop = new Prop();
        prop.setOwner( new Owner() );
        prop.setGroup( new Group() );
        prop.setAcl( new Acl() );
        body.setProp( prop );
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        Multistatus multistatus = this.execute( entity, new MultiStatusResponseHandler() );
        List<Response> responses = multistatus.getResponse();
        if ( responses.isEmpty() )
        {
            return null;
        }
        else
        {
            return new DavAcl( responses.get( 0 ) );
        }
    }

    @Override
    public DavQuota getQuota( String url )
        throws IOException
    {
        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( "0" );
        Propfind body = new Propfind();
        Prop prop = new Prop();
        prop.setQuotaAvailableBytes( new QuotaAvailableBytes() );
        prop.setQuotaUsedBytes( new QuotaUsedBytes() );
        body.setProp( prop );
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        Multistatus multistatus = this.execute( entity, new MultiStatusResponseHandler() );
        List<Response> responses = multistatus.getResponse();
        if ( responses.isEmpty() )
        {
            return null;
        }
        else
        {
            return new DavQuota( responses.get( 0 ) );
        }
    }

    @Override
    public void setAcl( String url, List<DavAce> aces )
        throws IOException
    {
        HttpAcl entity = new HttpAcl( url );
        // Build WebDAV <code>ACL</code> entity.
        Acl body = new Acl();
        body.setAce( new ArrayList<Ace>() );
        for ( DavAce davAce : aces )
        {
            // protected and inherited acl must not be part of ACL http request
            if ( davAce.getInherited() != null || davAce.isProtected() )
            {
                continue;
            }
            Ace ace = davAce.toModel();
            body.getAce().add( ace );
        }
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        execute( entity, getVoidResponseHandler() );
    }

    @Override
    public List<DavPrincipal> getPrincipals( String url )
        throws IOException
    {
        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( "1" );
        Propfind body = new Propfind();
        Prop prop = new Prop();
        prop.setDisplayname( new Displayname() );
        prop.setResourcetype( new Resourcetype() );
        prop.setPrincipalURL( new PrincipalURL() );
        body.setProp( prop );
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        Multistatus multistatus = this.execute( entity, new MultiStatusResponseHandler() );
        List<Response> responses = multistatus.getResponse();
        if ( responses.isEmpty() )
        {
            return null;
        }
        else
        {
            List<DavPrincipal> collections = new ArrayList<DavPrincipal>();
            for ( Response r : responses )
            {
                if ( r.getPropstat() != null )
                {
                    for ( Propstat propstat : r.getPropstat() )
                    {
                        if ( propstat.getProp() != null && propstat.getProp().getResourcetype() != null
                            && propstat.getProp().getResourcetype().getPrincipal() != null )
                        {
                            collections
                            .add( new DavPrincipal( DavPrincipal.PrincipalType.HREF, r.getHref().get( 0 ),
                                                    propstat.getProp().getDisplayname().getContent().get( 0 ) ) );
                        }
                    }
                }
            }
            return collections;
        }
    }

    @Override
    public List<String> getPrincipalCollectionSet( String url )
        throws IOException
    {
        HttpPropFind entity = new HttpPropFind( url );
        entity.setDepth( "0" );
        Propfind body = new Propfind();
        Prop prop = new Prop();
        prop.setPrincipalCollectionSet( new PrincipalCollectionSet() );
        body.setProp( prop );
        entity.setEntity( new StringEntity( SardineUtil.toXml( body ), UTF_8 ) );
        Multistatus multistatus = this.execute( entity, new MultiStatusResponseHandler() );
        List<Response> responses = multistatus.getResponse();
        if ( responses.isEmpty() )
        {
            return null;
        }
        else
        {
            List<String> collections = new ArrayList<String>();
            for ( Response r : responses )
            {
                if ( r.getPropstat() != null )
                {
                    for ( Propstat propstat : r.getPropstat() )
                    {
                        if ( propstat.getProp() != null && propstat.getProp().getPrincipalCollectionSet() != null
                            && propstat.getProp().getPrincipalCollectionSet().getHref() != null )
                        {
                            collections.addAll( propstat.getProp().getPrincipalCollectionSet().getHref() );
                        }
                    }
                }
            }
            return collections;
        }
    }

    @Override
    public void enableCompression()
    {
        builder.addInterceptorLast( new RequestAcceptEncoding() );
        builder.addInterceptorLast( new ResponseContentEncoding() );
        refreshClient();
    }

    @Override
    public void disableCompression()
    {
        builder.disableContentCompression();
        refreshClient();
    }

    @Override
    public void ignoreCookies()
    {
        builder.setDefaultCookieSpecRegistry( new Lookup<CookieSpecProvider>()
        {
            @Override
            public CookieSpecProvider lookup( String name )
            {
                return new IgnoreSpecProvider();
            }
        } );
        refreshClient();
    }

    @Override
    public void enablePreemptiveAuthentication( String hostname )
    {
        enablePreemptiveAuthentication( hostname, -1, -1 );
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

    @Override
    public void shutdown()
        throws IOException
    {
        this.client.close();
    }

}
