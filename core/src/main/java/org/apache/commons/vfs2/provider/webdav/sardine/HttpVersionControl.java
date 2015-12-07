package org.apache.commons.vfs2.provider.webdav.sardine;

import java.net.URI;

import org.apache.http.client.methods.HttpRequestBase;

/**
 * Simple class for making WebDAV <code>VERSION-CONTROL</code> requests.
 *
 */
public class HttpVersionControl extends HttpRequestBase
{
    public static final String METHOD_NAME = "VERSION-CONTROL";

    public HttpVersionControl(URI sourceUrl)
    {
        this.setURI(sourceUrl);
    }

    public HttpVersionControl(String sourceUrl) {
        this(URI.create(sourceUrl));
    }

    @Override
    public String getMethod()
    {
        return METHOD_NAME;
    }
}