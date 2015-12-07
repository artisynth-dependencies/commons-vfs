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
     * Creates a version-controlled resource at the requested url
     * @param url
     * @return
     */
    public boolean createVersion(String url);

    /**
     * Checkout a version of a resource
     * @param url
     * @throws IOException
     */
    void checkout( String url ) throws IOException;

    /**
     * Uncheckout a version of a resource
     * @param url
     * @throws IOException
     */
    void uncheckout( String url ) throws IOException;

    /**
     * Checkin a version of a resource
     * @param url
     * @throws IOException
     */
    void checkin( String url ) throws IOException;    
    
    /**
     * Executes the propfind command to find only particular properties
     * @param url
     * @param props
     * @return map of property to value
     */
    public Map<QName, String> getProperties(String url, Set<QName> props ) throws IOException;
    
    /**
     * Executes the propfind command to find only particular properties
     * @param url
     * @param props custom properties
     * @param allprop adds the allprop element to PROPFIND
     * @return map of property to value
     */
    public Map<QName, String> getProperties(String url, boolean allprop ) throws IOException;
}
