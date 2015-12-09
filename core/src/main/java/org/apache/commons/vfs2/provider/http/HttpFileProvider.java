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

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;

/**
 * An HTTP provider that uses Apache HttpComponents
 *
 */
public class HttpFileProvider
    extends AbstractOriginatingFileProvider
{
    /** Authenticator information. */
    public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
        UserAuthenticationData.USERNAME,
        UserAuthenticationData.PASSWORD };

    static final Collection<Capability> capabilities = Collections
        .unmodifiableCollection( Arrays.asList( new Capability[] {
            Capability.GET_TYPE,
            Capability.READ_CONTENT,
            Capability.URI,
            Capability.GET_LAST_MODIFIED,
            Capability.ATTRIBUTES,
            Capability.RANDOM_ACCESS_READ,
            Capability.DIRECTORY_READ_CONTENT, } ) );

    /**
     * Constructs a new provider.
     */
    public HttpFileProvider()
    {
        super();
        setFileNameParser( HttpFileNameParser.getInstance() );
    }

    protected void testConnection( HttpConnectionClient client )
        throws FileSystemException
    {

        // build and execute client
        HttpResponse response;
        try
        {
            response = client.execute( new HttpHead() );
        }
        catch ( Exception e )
        {
            throw new FileSystemException( "vfs.provider.http/connect.error", e );
        }

        // check head response
        int status = response.getStatusLine().getStatusCode();
        if ( status != HttpURLConnection.HTTP_OK )
        {
            throw new FileSystemException( "vfs.provider.http/head.error", new Object[] { response } );
        }

    }

    /**
     * Creates a {@link FileSystem}.
     */
    @Override
    protected FileSystem doCreateFileSystem( final FileName name, final FileSystemOptions fileSystemOptions )
        throws FileSystemException
    {
        // Create the file system
        final GenericFileName rootName = (GenericFileName) name;

        UserAuthenticationData authData = null;
        HttpConnectionClientManager httpClientManager;
        try
        {
            authData = UserAuthenticatorUtils.authenticate( fileSystemOptions, AUTHENTICATOR_TYPES );

            httpClientManager = HttpClientManagerFactory
                .createConnectionManager( rootName.getScheme(), rootName.getHostName(), rootName.getPort(),
                                          UserAuthenticatorUtils.toString( UserAuthenticatorUtils
                                              .getData( authData, UserAuthenticationData.USERNAME,
                                                        UserAuthenticatorUtils.toChar( rootName.getUserName() ) ) ),
                                          UserAuthenticatorUtils.toString( UserAuthenticatorUtils
                                              .getData( authData, UserAuthenticationData.PASSWORD,
                                                        UserAuthenticatorUtils.toChar( rootName.getPassword() ) ) ),
                                                                                  fileSystemOptions );

            // test connection
            HttpHost host = new HttpHost( rootName.getHostName(), rootName.getPort(), rootName.getScheme() ); // target host
            HttpConnectionClient client = httpClientManager.createClient( host );
            testConnection( client );

        }
        finally
        {
            UserAuthenticatorUtils.cleanup( authData );
        }

        return new HttpFileSystem( rootName, httpClientManager, fileSystemOptions );
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return HttpFileSystemConfigBuilder.getInstance();
    }

    @Override
    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }
}
