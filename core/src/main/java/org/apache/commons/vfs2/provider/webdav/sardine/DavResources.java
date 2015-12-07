package org.apache.commons.vfs2.provider.webdav.sardine;

import javax.xml.namespace.QName;

/**
 * WebDAV properties
 * @author Antonio
 *
 */
public class DavResources
{
    public static final String DAV_NAMESPACE = "DAV";
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
