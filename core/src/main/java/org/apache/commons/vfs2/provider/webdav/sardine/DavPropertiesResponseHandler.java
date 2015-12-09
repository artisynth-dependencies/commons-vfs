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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Element;

import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.handler.ValidatingResponseHandler;
import com.github.sardine.model.Creationdate;
import com.github.sardine.model.Displayname;
import com.github.sardine.model.Getcontentlanguage;
import com.github.sardine.model.Getcontentlength;
import com.github.sardine.model.Getcontenttype;
import com.github.sardine.model.Getetag;
import com.github.sardine.model.Getlastmodified;
import com.github.sardine.model.Multistatus;
import com.github.sardine.model.Propstat;
import com.github.sardine.model.Resourcetype;
import com.github.sardine.model.Response;
import com.github.sardine.util.SardineUtil;

/**
 * Parses a map of properties from a Multiresponse (in response to a PROPFIND command)
 * @author Antonio
 *
 */
public class DavPropertiesResponseHandler extends ValidatingResponseHandler<Map<URI, Map<QName,String>>>
{

    /**
     * The default content-type if {@link Getcontenttype} is not set in
     * the {@link com.github.sardine.model.Multistatus} response.
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * The default content-lenght if {@link Getcontentlength} is not set in
     * the {@link com.github.sardine.model.Multistatus} response.
     */
    public static final long DEFAULT_CONTENT_LENGTH = -1;

    /**
     * content-type for {@link com.github.sardine.model.Collection}.
     */
    public static final String HTTPD_UNIX_DIRECTORY_CONTENT_TYPE = "httpd/unix-directory";
    
    @Override
    public Map<URI,Map<QName, String>> handleResponse( HttpResponse response )
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
        if (responses.size() == 0) {
            return null;
        }
        
        Map<URI,Map<QName,String>> out = new HashMap<URI, Map<QName,String>>();
        for (Response mresponse : responses)
        {
            try
            {
                String href = mresponse.getHref().get( 0 );
                URI uri = new URI(href);
                Map<QName,String> props =  out.get( uri );
                if (props == null) {
                    props = new HashMap<QName,String>();
                    out.put( uri, props );
                }
                
                String str = getCreationDate( mresponse );
                if (str != null) {
                    props.put( DavResources.CREATIONDATE, str );
                }
                
                str = getModifiedDate( mresponse );
                if (str != null) {
                    props.put( DavResources.GETLASTMODIFIED, str );
                }
                
                str = getContentType( mresponse );
                if (str != null) {
                    props.put( DavResources.GETCONTENTTYPE, str );
                }
                
                str = getContentLength( mresponse );
                if (str != null) {
                    props.put( DavResources.GETCONTENTLENGTH, str );
                }
                
                str = getEtag( mresponse );
                if (str != null) {
                    props.put( DavResources.GETETAG, str );
                }
                
                str = getDisplayName( mresponse );
                if (str != null) {
                    props.put( DavResources.DISPLAYNAME, str );
                }
                
                str = getContentLanguage( mresponse );
                if (str != null) {
                    props.put( DavResources.GETCONTENTLANGUAGE, str );
                }
                
                addCustomProps( mresponse, props );
                
            }
            catch (URISyntaxException e)
            {
            }
        }
        return out;
    }
    
    /**
     * Retrieves modified date from props. If it is not available return null.
     *
     * @param response The response complex type of the multistatus
     * @return Null if not found in props
     */
    private static String getModifiedDate(Response response)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Getlastmodified glm = propstat.getProp().getGetlastmodified();
                if ((glm != null) && (glm.getContent().size() == 1))
                {
                    return glm.getContent().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves creation date from props. If it is not available return null.
     *
     * @param response The response complex type of the multistatus
     * @return Null if not found in props
     */
    private static String getCreationDate(Response response)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Creationdate gcd = propstat.getProp().getCreationdate();
                if ((gcd != null) && (gcd.getContent().size() == 1))
                {
                    return gcd.getContent().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the content-type from prop or set it to {@link #DEFAULT_CONTENT_TYPE}. If isDirectory, always 
     * set the content-type to {@link #HTTPD_UNIX_DIRECTORY_CONTENT_TYPE}.
     *
     * @param response The response complex type of the multistatus
     * @return the content type.
     */
    private static String getContentType(Response response)
    {
        // Make sure that directories have the correct content type.
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Resourcetype resourcetype = propstat.getProp().getResourcetype();
                if ((resourcetype != null) && (resourcetype.getCollection() != null))
                {
                    // Need to correct the contentType to identify as a directory.
                    return HTTPD_UNIX_DIRECTORY_CONTENT_TYPE;
                }
                else
                {
                    Getcontenttype gtt = propstat.getProp().getGetcontenttype();
                    if ((gtt != null) && (gtt.getContent().size() == 1))
                    {
                        return gtt.getContent().get(0);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves content-length from props. If it is not available return null
     *
     * @param response The response complex type of the multistatus
     * @return contentlength
     */
    private static String getContentLength(Response response)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Getcontentlength gcl = propstat.getProp().getGetcontentlength();
                if ((gcl != null) && (gcl.getContent().size() == 1))
                {
                    try
                    {
                        return gcl.getContent().get(0);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves content-length from props. If it is not available return {@link #DEFAULT_CONTENT_LENGTH}.
     *
     * @param response The response complex type of the multistatus
     * @return contentlength
     */
    private static String getEtag(Response response)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Getetag e = propstat.getProp().getGetetag();
                if ((e != null) && (e.getContent().size() == 1))
                {
                    return e.getContent().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the content-language from prop.
     *
     * @param response The response complex type of the multistatus
     * @return the content language; {@code null} if it is not avaialble
     */
    private static String getContentLanguage(Response response)
    {
        // Make sure that directories have the correct content type.
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Resourcetype resourcetype = propstat.getProp().getResourcetype();
                if ((resourcetype != null) && (resourcetype.getCollection() != null))
                {
                    // Need to correct the contentType to identify as a directory.
                    return HTTPD_UNIX_DIRECTORY_CONTENT_TYPE;
                }
                else
                {
                    Getcontentlanguage gtl = propstat.getProp().getGetcontentlanguage();
                    if ((gtl != null) && (gtl.getContent().size() == 1))
                    {
                        return gtl.getContent().get(0);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves displayName from props.
     *
     * @param response The response complex type of the multistatus
     * @return the display name; {@code null} if it is not available
     */
    private static String getDisplayName(Response response)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return null;
        }
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                Displayname dn = propstat.getProp().getDisplayname();
                if ((dn != null) && (dn.getContent().size() == 1))
                {
                    return dn.getContent().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Creates a simple complex Map from the given custom properties of a response.
     * This implementation does take namespaces into account.
     *
     * @param response The response complex type of the multistatus
     * @return Custom properties
     */
    private static void addCustomProps(Response response, Map<QName, String> propMap)
    {
        List<Propstat> list = response.getPropstat();
        if (list.isEmpty())
        {
            return;
        }
        
        for (Propstat propstat : list)
        {
            if (propstat.getProp() != null) {
                List<Element> props = propstat.getProp().getAny();
                for (Element element : props)
                {
                    String namespace = element.getNamespaceURI();
                    if (namespace == null)
                    {
                        propMap.put(new QName(SardineUtil.DEFAULT_NAMESPACE_URI,
                                element.getLocalName(),
                                SardineUtil.DEFAULT_NAMESPACE_PREFIX),
                                element.getTextContent());
                    }
                    else
                    {
                        if (element.getPrefix() == null)
                        {
                            propMap.put(new QName(element.getNamespaceURI(),
                                    element.getLocalName()),
                                    element.getTextContent());
                        }
                        else
                        {
                            propMap.put(new QName(element.getNamespaceURI(),
                                    element.getLocalName(),
                                    element.getPrefix()),
                                    element.getTextContent());
                        }
                    }

                }
            }
        }
    }

}
