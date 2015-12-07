package org.apache.commons.vfs2.provider.webdav.sardine;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import com.github.sardine.impl.io.ContentLengthInputStream;

public class HttpResponseInputStream extends ContentLengthInputStream
{
    protected HttpResponse response;

    /**
     * @param response The HTTP response to read from
     * @throws IOException        If there is a problem reading from the response
     * @throws NullPointerException If the response has no message entity
     */
    public HttpResponseInputStream(final HttpResponse response) throws IOException
    {
        super(response.getEntity().getContent(), response.getEntity().getContentLength());
        this.response = response;
    }
    
    /**
     * @return the status line of the response
     */
    public StatusLine getStatus() {
        return response.getStatusLine();
    }

    @Override
    /**
     * Consumes entirety of HTTP response on close
     */
    public void close() throws IOException
    {
        EntityUtils.consume(response.getEntity());
    }
    
    
}
