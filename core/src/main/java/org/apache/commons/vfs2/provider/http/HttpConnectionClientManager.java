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

import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpConnectionClientManager
{

    private HttpClientConnectionManager manager;
    private HttpClientBuilder builder;
    private HttpClientContext context;
    private HttpHost host;

    public HttpConnectionClientManager()
    {
        this.manager = null;
        this.builder = null;
        this.context = null;
        this.host = null;
    }

    public HttpConnectionClientManager( HttpClientConnectionManager manager )
    {
        if (manager != null) {
            this.manager = manager;
        } else {
            this.manager = new PoolingHttpClientConnectionManager();
        }
    }
    
    public void setClientContext( HttpClientContext context )
    {
        this.context = context;
    }
    
    public void setHost(HttpHost host) {
        this.host = host;
    }

    public void setClientBuilder( HttpClientBuilder builder )
    {
        this.builder = builder;
        this.builder.setConnectionManager( manager );
        this.builder.setConnectionManagerShared( true );
    }
    
    public HttpConnectionClient createClient() {
        return createClient(host);
    }
    
    public HttpConnectionClient createClient(HttpHost host) {
        CloseableHttpClient client = builder.build();
        return new HttpConnectionClient( client, host, context );
    }
    
    public HttpClientConnectionManager getClientManager() {
        return manager;
    }
    
    public HttpClientBuilder getClientBuilder() {
        return builder;
    }
    
    public HttpClientContext getClientContext() {
        return context;
    }
    
    public HttpHost getHost() {
    	return host;
    }
    
    public void shutdown() {
        manager.shutdown();
    }
    
}
