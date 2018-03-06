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
package org.apache.commons.vfs2.provider.http;

import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

/**
 * Create a manager for client connections
 */
public final class HttpClientManagerFactory
{
    private HttpClientManagerFactory()
    {
    }

    public static HttpConnectionClientManager createConnectionManager( final String scheme, final String hostname,
                                                                       final int port, final String username,
                                                                       final String password,
                                                                       final FileSystemOptions fileSystemOptions )
                                                                           throws FileSystemException
    {
        return createConnectionManager( HttpFileSystemConfigBuilder.getInstance(), scheme, hostname, port, username,
                                        password, fileSystemOptions );
    }

    /**
     * Creates a new connection builder for generating clients to the server.
     * @param builder The HttpFileSystemConfigBuilder.
     * @param scheme The protocol.
     * @param hostname The hostname.
     * @param port The port number.
     * @param username The username.
     * @param password The password
     * @param fileSystemOptions The file system options.
     * @return a new HttpClient connection.
     * @throws FileSystemException if an error occurs.
     * @since 2.0
     */
    public static HttpConnectionClientManager createConnectionManager( HttpFileSystemConfigBuilder builder,
                                                                       String scheme, String hostname, int port,
                                                                       String username, String password,
                                                                       FileSystemOptions fileSystemOptions )
                                                                           throws FileSystemException
    {

        HttpConnectionClientManager clientManager = null;

        try
        {
            HttpClientBuilder clientBuilder = HttpClients.custom(); // custom builder for client

            // credentials for either proxy or host
            HttpClientContext context = new HttpClientContext(); // execution context

            CredentialsProvider credentials = new BasicCredentialsProvider();
            context.setCredentialsProvider( credentials );

            // registry
            RegistryBuilder<ConnectionSocketFactory> socketRegistryBuilder = RegistryBuilder.create();
            socketRegistryBuilder.register( "http", PlainConnectionSocketFactory.getSocketFactory() );
            socketRegistryBuilder.register( "https", SSLConnectionSocketFactory.getSocketFactory() );

            if ( fileSystemOptions != null )
            {

                // proxy and potential authentication
                String proxyHost = builder.getProxyHost( fileSystemOptions );
                int proxyPort = builder.getProxyPort( fileSystemOptions );

                if ( proxyHost != null && proxyHost.length() > 0 && proxyPort > 0 )
                {
                    HttpHost proxy = new HttpHost( proxyHost, proxyPort );
                    DefaultProxyRoutePlanner proxyPlanner = new DefaultProxyRoutePlanner( proxy );
                    clientBuilder.setRoutePlanner( proxyPlanner );

                    UserAuthenticator proxyAuth = builder.getProxyAuthenticator( fileSystemOptions );
                    if ( proxyAuth != null )
                    {
                        UserAuthenticationData authData = UserAuthenticatorUtils
                            .authenticate( proxyAuth, new UserAuthenticationData.Type[] {
                                UserAuthenticationData.USERNAME,
                                UserAuthenticationData.PASSWORD } );

                        if ( authData != null )
                        {
                            String proxyuser = UserAuthenticatorUtils.toString( UserAuthenticatorUtils
                                .getData( authData, UserAuthenticationData.USERNAME, null ) );
                            String proxypass = UserAuthenticatorUtils.toString( UserAuthenticatorUtils
                                .getData( authData, UserAuthenticationData.PASSWORD, null ) );
                            final UsernamePasswordCredentials proxyCreds = new UsernamePasswordCredentials( proxyuser,
                                                                                                            proxypass );

                            AuthScope scope = new AuthScope( proxyHost, AuthScope.ANY_PORT );

                            // add credentials for proxy
                            credentials.setCredentials( scope, proxyCreds );

                        }

                        if ( builder.isPreemptiveAuth( fileSystemOptions ) )
                        {
                            // authentication cache
                            AuthCache authCache = new BasicAuthCache();
                            BasicScheme basicAuth = new BasicScheme();
                            authCache.put( proxy, basicAuth );
                            context.setAuthCache( authCache );
                        }
                    }
                }

                // session cookies
                Cookie[] cookies = builder.getCookies( fileSystemOptions );
                if ( cookies != null )
                {
                    BasicCookieStore cookieStore = new BasicCookieStore();
                    cookieStore.addCookies( cookies );
                    context.setCookieStore( cookieStore );
                }

                SSLContextBuilder sslbuilder = new SSLContextBuilder();

                // trust strategy           
                TrustStrategy[] ts = builder.getTrustStrategies( fileSystemOptions );
                if ( ts != null )
                {
                    for ( TrustStrategy trust : ts )
                    {
                        sslbuilder.loadTrustMaterial( null, trust );
                    }
                }

                // key Stores
                KeyStore[] ks = builder.getKeyStores( fileSystemOptions );
                if ( ks != null )
                {
                    for ( KeyStore keys : ks )
                    {
                        sslbuilder.loadTrustMaterial( keys, null );
                    }
                }

                String[] ciphers = builder.getEnabledSSLCipherSuites( fileSystemOptions );
                String[] protocols = builder.getEnabledSSLProtocols( fileSystemOptions );
                HostnameVerifier hostVerifier = builder.getSSLHostnameVerifier( fileSystemOptions );
                SSLContext sslcontext = sslbuilder.build();

                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory( sslcontext, protocols, ciphers,
                                                                                   hostVerifier );

                // over-ride socket registry
                socketRegistryBuilder.register( "https", sslsf );

            }

            // host credentials
            if ( username != null )
            {
                final UsernamePasswordCredentials creds = new UsernamePasswordCredentials( username, password );
                AuthScope scope = new AuthScope( hostname, AuthScope.ANY_PORT );

                credentials.setCredentials( scope, creds );
            }

            // timeouts and redirects
            int connectionTimeout = builder.getConnectionTimeout( fileSystemOptions );
            int socketTimeout = builder.getSoTimeout( fileSystemOptions );
            boolean followRedirects = builder.getFollowRedirect( fileSystemOptions );
            boolean allowCircularRedirects = builder.getCircularRedirectsAllowed( fileSystemOptions );

            RequestConfig.Builder requestConfig = RequestConfig.custom();
            requestConfig.setExpectContinueEnabled( false );
            requestConfig.setConnectTimeout( connectionTimeout );
            requestConfig.setSocketTimeout( socketTimeout );
            requestConfig.setRedirectsEnabled( followRedirects );
            requestConfig.setCircularRedirectsAllowed( allowCircularRedirects );

            clientBuilder.setDefaultRequestConfig( requestConfig.build() );

            // user agent
            String userAgent = builder.getUserAgent( fileSystemOptions );
            clientBuilder.setUserAgent( userAgent );

            // Use pooled connection manager
            PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager( socketRegistryBuilder
                .build() );
            manager.setMaxTotal( builder.getMaxTotalConnections( fileSystemOptions ) );
            manager.setDefaultMaxPerRoute( builder.getMaxConnectionsPerHost( fileSystemOptions ) );

            HttpHost host = new HttpHost( hostname, port, scheme );
            clientManager = new HttpConnectionClientManager( manager );
            clientManager.setClientBuilder( clientBuilder );
            clientManager.setClientContext( context );
            clientManager.setHost( host );
        }
        catch ( final Exception exc )
        {
            throw new FileSystemException( "vfs.provider.http/connect.error", exc, hostname );
        }

        return clientManager;
    }

}
