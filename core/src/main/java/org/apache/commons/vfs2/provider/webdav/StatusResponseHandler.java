package org.apache.commons.vfs2.provider.webdav;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import com.github.sardine.impl.handler.ValidatingResponseHandler;

public class StatusResponseHandler extends ValidatingResponseHandler<StatusLine>
{

    @Override
    public StatusLine handleResponse( HttpResponse response ) throws ClientProtocolException, IOException
    {
        super.validateResponse( response );
        return response.getStatusLine();
    }

}
