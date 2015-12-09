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

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Class that stores the client, manager, and context
 * @author Antonio
 *
 */
public class HttpConnectionClient
{
    private HttpClientContext context;

    private HttpHost host;

    private CloseableHttpClient client;

    public HttpConnectionClient( CloseableHttpClient client, HttpHost host, HttpClientContext context )
    {
        this.client = client;
        this.host = host;
        this.context = context;
    }

    /**
     * Executes an Http request
     * @param request
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public HttpResponse execute( HttpUriRequest request )
        throws ClientProtocolException, IOException
    {
        HttpResponse response;
        if ( context != null )
        {
            response = client.execute( host, request, context );
        }
        else
        {
            response = client.execute( host, request );
        }

        return response;
    }

    /**
     * Executes an Http request
     * @param request
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public HttpResponse execute( HttpHost host, HttpUriRequest request )
        throws ClientProtocolException, IOException
    {
        HttpResponse response;
        if ( context != null )
        {
            response = client.execute( host, request, context );
        }
        else
        {
            response = client.execute( host, request );
        }

        return response;
    }

    /**
     * Executes an Http request
     * @param request
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public HttpResponse execute( HttpHost host, HttpUriRequest request, HttpClientContext context )
        throws ClientProtocolException, IOException
    {
        HttpResponse response;
        if ( context != null )
        {
            response = client.execute( host, request, context );
        }
        else
        {
            response = client.execute( host, request );
        }

        return response;
    }

}
