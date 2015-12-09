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

import javax.xml.namespace.QName;

/**
 * WebDAV property elements
 * @author Antonio
 *
 */
public class DavResources
{
    public static final String DAV_NAMESPACE = "DAV:";
    public static final String DAV_PREFIX = "D";
    
    public static final QName createQName(String key) {
        return new QName(DAV_NAMESPACE, key, DAV_PREFIX);
    }
    
    // basic DAV stuff
    public static final QName CREATIONDATE = createQName( "creationdata" );
    public static final QName DISPLAYNAME = createQName( "displayname" );
    public static final QName GETCONTENTLANGUAGE = createQName( "getcontentlanguage" );
    public static final QName GETCONTENTLENGTH = createQName( "getcontentlength" );
    public static final QName GETCONTENTTYPE = createQName( "getcontenttype" );
    public static final QName GETETAG = createQName( "getetag" );
    public static final QName GETLASTMODIFIED = createQName( "getlastmodified" );
    public static final QName LOCKDISCOVERY = createQName( "lockdiscovery" );
    public static final QName RESOURCETYPE = createQName( "resourcetype" );
    public static final QName SUPPORTEDLOCK = createQName( "supportedlock" );
    
    
    // versioning specific stuff
    public static final QName COMMENT = createQName("comment");
    public static final QName CREATOR_DISPLAYNAME = createQName("creator-displayname");
    public static final QName CHECKED_OUT = createQName( "checked-out");
    public static final QName CHECKED_IN = createQName( "checked-in" );
    public static final QName AUTO_VERSION = createQName( "auto-version" );
    
    public static final String CHECKOUT_CHECKIN = "checkin-checkout";
    
    
}
