package org.apache.commons.vfs2.provider.webdav;

import java.net.URI;

import org.apache.http.client.methods.HttpRequestBase;

/**
 * Simple class for making WebDAV <code>VERSION-CONTROL</code> requests.
 *
 */
public class HttpCheckin extends HttpRequestBase
{
    public static final String METHOD_NAME = "CHECKIN";

    public HttpCheckin(URI sourceUrl)
    {
        this.setURI(sourceUrl);
    }

    public HttpCheckin(String sourceUrl) {
        this(URI.create(sourceUrl));
    }

    @Override
    public String getMethod()
    {
        return METHOD_NAME;
    }
}