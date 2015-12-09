/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.commons.vfs2.provider.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.URLFileName;
import org.apache.commons.vfs2.util.MonitorInputStream;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;

/**
 * A file object backed by Apache HttpComponents.
 * <p>
 * TODO - status codes.
 *
 * @param <FS> An {@link HttpFileSystem} subclass
 */
public class HttpFileObject<FS extends HttpFileSystem>
    extends AbstractFileObject<FS>
{
    /**
     * An InputStream that cleans up the HTTP connection on close.
     */
    static class HttpInputStream
        extends MonitorInputStream
    {
        private final HttpResponse response;

        public HttpInputStream( final HttpResponse response )
            throws IOException
        {
            super( response.getEntity().getContent() );
            this.response = response;
        }

        /**
         * Called after the stream has been closed.
         */
        @Override
        protected void onClose()
            throws IOException
        {
            if ( response instanceof CloseableHttpResponse )
            {
                ( (CloseableHttpResponse) response ).close();
            }
        }
    }

    private HttpResponse headResponse;

    protected HttpFileObject( final AbstractFileName name, final FS fileSystem )
    {
        this( name, fileSystem, HttpFileSystemConfigBuilder.getInstance() );
    }

    protected HttpFileObject( final AbstractFileName name, final FS fileSystem,
                              final HttpFileSystemConfigBuilder builder )
    {
        super( name, fileSystem );
        headResponse = null;
    }

    /**
     * Detaches this file object from its file resource.
     */
    @Override
    protected void doDetach()
        throws Exception
    {
        headResponse = null;
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize()
        throws Exception
    {
        if ( headResponse == null )
        {
            return 0;
        }

        final Header[] headers = headResponse.getHeaders( "content-length" );

        if ( headers == null || headers.length == 0 )
        {
            // Assume 0 content-length
            return 0;
        }

        // return first response
        return Long.parseLong( headers[0].getValue() );
    }

    /**
     * Creates an input stream to read the file content from.  Is only called
     * if {@link #doGetType} returns {@link FileType#FILE}.
     * <p>
     * It is guaranteed that there are no open output streams for this file
     * when this method is called.
     * <p>
     * The returned stream does not have to be buffered.
     */
    @Override
    protected InputStream doGetInputStream()
        throws Exception
    {
        final HttpGet getMethod = new HttpGet();
        setupMethod( getMethod );

        HttpResponse getResponse = getAbstractFileSystem().getClient().execute( getMethod );

        final int status = getResponse.getStatusLine().getStatusCode();

        if ( status == HttpURLConnection.HTTP_NOT_FOUND )
        {
            throw new FileNotFoundException( getName() );
        }
        if ( status != HttpURLConnection.HTTP_OK )
        {
            throw new FileSystemException( "vfs.provider.http/get.error", getName(), Integer.valueOf( status ) );
        }

        return new HttpInputStream( getResponse );
    }

    /**
     * Returns the last modified time of this file.
     * <p>
     * This implementation throws an exception.
     */
    @Override
    protected long doGetLastModifiedTime()
        throws Exception
    {
        if ( headResponse == null )
        {
            return 0;
        }

        final Header[] headers = headResponse.getHeaders( "last-modified" );
        if ( headers == null || headers.length == 0 )
        {
            throw new FileSystemException( "vfs.provider.http/last-modified.error", getName() );
        }
        return DateUtils.parseDate( headers[0].getValue() ).getTime();
    }

    @Override
    protected RandomAccessContent doGetRandomAccessContent( final RandomAccessMode mode )
        throws Exception
    {
        return new HttpRandomAccessContent( this, mode );
    }

    /**
     * Determines the type of this file.  Must not return null.  The return
     * value of this method is cached, so the implementation can be expensive.
     */
    @Override
    protected FileType doGetType()
        throws Exception
    {
        // Use the HEAD method to probe the file.
        HttpResponse headResponse = getHeadResponse();
        final int status = headResponse.getStatusLine().getStatusCode();

        if ( status == HttpURLConnection.HTTP_OK
            || status == HttpURLConnection.HTTP_BAD_METHOD /* method is bad, but resource exist */ )
        {
            // return FileType.FILE_OR_FOLDER;  // we can't know which from head only
            return FileType.FILE; // assume FILE, since tests rely on it
        }
        else if ( status == HttpURLConnection.HTTP_NOT_FOUND || status == HttpURLConnection.HTTP_GONE )
        {
            return FileType.IMAGINARY;
        }
        else
        {
            throw new FileSystemException( "vfs.provider.http/head.error", getName(), Integer.valueOf( status ) );
        }
    }

    @Override
    protected boolean doIsWriteable()
        throws Exception
    {
        return false;
    }

    /**
     * Lists the children of this file.
     */
    @Override
    protected String[] doListChildren()
        throws Exception
    {
        throw new Exception( "Not implemented." );
    }

    @Override
    protected FileContentInfoFactory getFileContentInfoFactory()
    {
        return new HttpFileContentInfoFactory();
    }

    HttpResponse getHeadResponse()
        throws IOException
    {
        if ( headResponse != null )
        {
            return headResponse;
        }

        // Use the HEAD method to probe the file.
        HttpHead method = new HttpHead();
        setupMethod( method );
        final HttpConnectionClient client = getAbstractFileSystem().getClient();

        // final int status = client.execute(method);
        headResponse = client.execute( method );
        return headResponse;
    }

    /**
     * Prepares a Request object.
     * @throws FileSystemException
     * @since 2.0 (was package)
     */
    protected void setupMethod( final HttpRequestBase method )
        throws FileSystemException
    {
        URLFileName file = ( (URLFileName) getName() );
        URI uri = null;

        try
        {
            // technically I only need the relative path/query
            uri = new URI( null, null, file.getPathDecoded(), file.getQueryString(), null );
        }
        catch ( URISyntaxException se )
        {
            throw new FileSystemException( "Invalid URI syntax", se );
        }
        method.setURI( uri );

    }

    @Override
    protected Map<String, Object> doGetAttributes()
        throws Exception
    {
        return super.doGetAttributes();
    }

    /*
    protected Map doGetAttributes() throws Exception
    {
        TreeMap map = new TreeMap();
    
        Header contentType = method.getResponseHeader("content-type");
        if (contentType != null)
        {
            HeaderElement[] element = contentType.getValues();
            if (element != null && element.length > 0)
            {
                map.put("content-type", element[0].getName());
            }
        }
    
        map.put("content-encoding", method.getResponseCharSet());
        return map;
    }
    */
}
