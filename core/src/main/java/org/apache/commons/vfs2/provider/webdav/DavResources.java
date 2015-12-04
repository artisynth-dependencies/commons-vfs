package org.apache.commons.vfs2.provider.webdav;

import javax.xml.namespace.QName;

public class DavResources
{
    public static final String DAV_NAMESPACE = "DAV";
    public static final String DAV_PREFIX = "D";
    
    public static final QName createQName(String key) {
        return new QName(DAV_NAMESPACE, key, DAV_PREFIX);
    }
    
    public static final QName COMMENT = createQName("comment");
    
    // versioning specific stuff
    public static final QName CREATOR_DISPLAYNAME = createQName("creator-displayname");
    public static final QName CHECKED_OUT = new QName( "DAV",  "checked-out", "D");
    public static final QName CHECKED_IN = new QName( "DAV", "checked-in", "D" );
    public static final QName AUTO_VERSION = new QName( "DAV", "auto-version", "D");
    
    public static final String XML_CHECKOUT_CHECKIN = "checkin-checkout";
    
    
}
