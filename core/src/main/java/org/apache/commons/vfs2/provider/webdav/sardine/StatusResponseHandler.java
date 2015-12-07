package org.apache.commons.vfs2.provider.webdav.sardine;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;

public class StatusResponseHandler implements ResponseHandler<StatusLine>
{

    @Override
    public StatusLine handleResponse( HttpResponse response )
    {
        return response.getStatusLine();
    }

}
