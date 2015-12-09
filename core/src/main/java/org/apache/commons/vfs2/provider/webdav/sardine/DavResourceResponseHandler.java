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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import com.github.sardine.DavResource;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.handler.ValidatingResponseHandler;
import com.github.sardine.model.Multistatus;
import com.github.sardine.model.Response;
import com.github.sardine.util.SardineUtil;

/**
 * Parses a set of resources from a Multiresponse (from a PROPFIND command)
 * @author Antonio
 *
 */
public class DavResourceResponseHandler extends ValidatingResponseHandler<List<DavResource>>
{

    @Override
    public List<DavResource> handleResponse( HttpResponse response )
        throws ClientProtocolException, IOException
    {
        super.validateResponse( response );
        
        // Process the multistatus response from the server.
        Multistatus multistatus = null;
        HttpEntity entity = response.getEntity();
        StatusLine statusLine = response.getStatusLine();
        if (entity == null)
        {
            throw new SardineException("No entity found in response", statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }
        try
        {
            multistatus = SardineUtil.unmarshal(entity.getContent());
        }
        catch(IOException e) {
            // JAXB error unmarshalling response stream
            throw new SardineException(e.getMessage(), statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        
        // collected responses
        List<Response> responses = multistatus.getResponse();
        List<DavResource> resources = new ArrayList<DavResource>(responses.size());
        for (Response mresponse : responses)
        {
            try
            {
                DavResource resource = new DavResource(mresponse);
                resources.add(resource);
            }
            catch (URISyntaxException e)
            {
            }
        }
        return resources;
    }

}
