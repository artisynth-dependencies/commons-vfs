package org.apache.commons.vfs2.provider.webdav;

import java.io.IOException;

import com.github.sardine.Sardine;

public interface VersionableSardine extends Sardine
{
    
    /**
     * Creates a version-controlled resource at the requested url
     * @param url
     * @return
     */
    public boolean createVersion(String url);

    void checkout( String url ) throws IOException;

    void uncheckout( String url ) throws IOException;

    void checkin( String url ) throws IOException;
    
    
    
}
