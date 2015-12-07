package org.apache.commons.vfs2.provider.webdav.sardine;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import com.github.sardine.impl.handler.ValidatingResponseHandler;

public class InputStreamResponseHandler extends ValidatingResponseHandler<HttpResponseInputStream>
{
    
    @Override
    public HttpResponseInputStream handleResponse( HttpResponse response )
        throws ClientProtocolException, IOException
    {
        super.validateResponse( response );
        
        return new HttpResponseInputStream( response );
    }

}
