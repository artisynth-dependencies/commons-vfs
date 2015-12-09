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
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.github.sardine.Sardine;

/**
 * Extends Sardine to allow versioning commands and getting of properties
 *
 * @author Antonio
 *
 */
public interface SardineExtended extends Sardine
{   
      
    /**
     * Creates a version-controlled resource at the requested url by sending
     * a <code>VERSION-CONTROL</code> request
     * @param url Path to the resource including protocol and hostname
     * @return true if versioning supported and command executed successfully
     */
    public boolean createVersion(String url);

    /**
     * Checkout a version of a resource by sending
     * a <code>CHECKOUT</code> request
     * @param url Path to the resource including protocol and hostname
     * @throws IOException I/O error or HTTP response validation failure
     */
    void checkout( String url ) throws IOException;

    /**
     * Uncheckout a version of a resource by sending
     * a <code>UNCHECKOUT</code> request
     * @param url Path to the resource including protocol and hostname
     * @throws IOException I/O error or HTTP response validation failure
     */
    void uncheckout( String url ) throws IOException;

    /**
     * Checkin a version of a resource by sending
     * a <code>CHECKIN</code> request
     * @param url Path to the resource including protocol and hostname
     * @throws IOException I/O error or HTTP response validation failure
     */
    void checkin( String url ) throws IOException;    
    
    /**
     * Executes the <code>PROPFIND</code> request to find only particular properties
     * @param url Path to the resource including protocol and hostname
     * @param props set of properties to request
     * @return map of property to value
     * @throws IOException I/O error or HTTP response validation failure
     */
    public Map<QName, String> getProperties(String url, Set<QName> props ) throws IOException;
    
    /**
     * Executes the <code>PROPFIND</code> request to find only particular properties
     * @param url Path to the resource including protocol and hostname
     * @param allprop adds the &lt;allprop/&gt; element to <code>PROPFIND</code>
     * @return map of property to value
     * @throws IOException I/O error or HTTP response validation failure
     */
    public Map<QName, String> getProperties(String url, boolean allprop ) throws IOException;
    
    /**
     * Executes the <code>PROPFIND</code> request to find only particular properties
     * @param url Path to the resource including protocol and hostname
     * @param props set of properties to request
     * @param allprop adds the &lt;allprop/&gt; element to <code>PROPFIND</code>
     * @return map of property to value
     * @throws IOException I/O error or HTTP response validation failure
     */
    public Map<QName, String> getProperties(String url, Set<QName> props, boolean allprop ) throws IOException;
}
