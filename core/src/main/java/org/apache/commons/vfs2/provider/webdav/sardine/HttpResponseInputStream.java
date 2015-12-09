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
package org.apache.commons.vfs2.provider.webdav.sardine;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import com.github.sardine.impl.io.ContentLengthInputStream;

/**
 * Input stream from an HttpResponse, keeps track of the response status
 * @author Antonio
 *
 */
public class HttpResponseInputStream extends ContentLengthInputStream
{
    protected HttpResponse response;

    /**
     * @param response The HTTP response to read from
     * @throws IOException        If there is a problem reading from the response
     * @throws NullPointerException If the response has no message entity
     */
    public HttpResponseInputStream(final HttpResponse response) throws IOException
    {
        super(response.getEntity().getContent(), response.getEntity().getContentLength());
        this.response = response;
    }
    
    /**
     * @return the status line of the response
     */
    public StatusLine getStatus() {
        return response.getStatusLine();
    }
    
    /**
     * @return the corresponding HTTP response
     */
    public HttpResponse getResponse() {
        return response;
    }

    @Override
    /**
     * Consumes entirety of HTTP response on close
     */
    public void close() throws IOException
    {
        EntityUtils.consume(response.getEntity());
    }
    
    
}
