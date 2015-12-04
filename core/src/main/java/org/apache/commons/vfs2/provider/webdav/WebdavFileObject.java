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
package org.apache.commons.vfs2.provider.webdav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.DefaultFileContent;
import org.apache.commons.vfs2.provider.URLFileName;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.MonitorOutputStream;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;

import com.github.sardine.DavResource;
import com.github.sardine.impl.SardineException;
import com.github.sardine.util.SardineUtil;

public class WebdavFileObject<FS extends WebdavFileSystem>
extends AbstractFileObject<FS>
{

    public class WebdavOutputStream extends MonitorOutputStream
    {
        WebdavFileObject<FS> oldfile;

        public WebdavOutputStream(final WebdavFileObject<FS> file)
        {
            super(new ByteArrayOutputStream());
            oldfile = file;
        }

        private boolean createVersion( final String urlString ) {
            return sardine.createVersion( urlString );
        }

        private void setUserName(final URLFileName fileName, final String url)
            throws IOException
        { 
            String name = builder.getCreatorName(fileSystem.getFileSystemOptions());
            final String userName = fileName.getUserName();
            HashMap<QName,String> propMap = new HashMap<QName,String>();
            if (name == null)
            {
                name = userName;
            }
            else
            {
                if (userName != null && !userName.equals( name ))
                {
                    final String comment = "Modified by user " + userName;
                    propMap.put( DavResources.COMMENT, comment );
                }
            }
            propMap.put( DavResources.CREATOR_DISPLAYNAME, name );
            
            sardine.patch( url, propMap );
        }

        /**
         * On close, the file is written
         * @throws IOException
         * @see org.apache.commons.vfs2.util.MonitorOutputStream#onClose()
         */
        @Override
        protected void onClose() throws IOException
        {
            final URLFileName fileName = (URLFileName) getName();
            String url = getFullUrl(fileName, true);

            // check if versioning
            if (builder.isVersioning(fileSystem.getFileSystemOptions()))
            {
                boolean fileExists = sardine.exists( url );
                boolean isCheckedIn = true;

                // look for CHECKED_OUT property
                if (fileExists) {
                    // look for checked-in property
                    List<DavResource> res = sardine.list( url, 0, true );
                    Map<QName,String> customProps = null;
                    if (res.size() > 0) {
                        customProps = res.get( 0 ).getCustomPropsNS();
                    }

                    if (customProps != null) {

                        // look for CHECKED_OUT
                        if (customProps.containsKey(DavResources.CHECKED_OUT))
                        {
                            isCheckedIn = false;
                        }
                        else if (!customProps.containsKey(DavResources.CHECKED_IN))
                        {
                            String prop = customProps.get(DavResources.AUTO_VERSION);
                            if (prop != null)
                            {
                                if (DavResources.XML_CHECKOUT_CHECKIN.equals(prop))
                                {
                                    createVersion( url );
                                }
                            }
                        } // not checked in
                    } // has custom properties
                } // file exists

                if (fileExists && isCheckedIn)
                {
                    try
                    {
                        sardine.checkout( url );
                        isCheckedIn = false;
                    }
                    catch (final FileSystemException ex)
                    {
                        // Ignore the exception checking out.
                    }
                }

                // put data
                try {
                    sardine.put( url, ( (ByteArrayOutputStream) out ).toByteArray() );
                    setUserName(fileName, url);
                }
                catch (final IOException ex)
                {
                    if (!isCheckedIn)
                    {
                        try
                        {
                            sardine.uncheckout( url );
                            isCheckedIn = true;
                        }
                        catch (final Exception e)
                        {
                            // Ignore the exception. Going to throw original.
                        }
                        throw ex;
                    }
                }

                // now the file should exist
                if (!fileExists)
                {
                    createVersion( url );
                    try
                    {
                        List<DavResource> res = sardine.list( url, 0, true );
                        Map<QName,String> customProps = null;
                        if (res.size() > 0) {
                            customProps = res.get( 0 ).getCustomPropsNS();
                        }
                        if (customProps != null) {
                            isCheckedIn = !customProps.containsKey(DavResources.CHECKED_OUT);
                        } else {
                            isCheckedIn = false;
                        }
                    }
                    catch (final FileNotFoundException fnfe)
                    {
                        // Ignore the error
                    }
                }
                if (!isCheckedIn)
                {
                    sardine.checkin( url );
                }                


            } else
            {
                sardine.put( url, ( (ByteArrayOutputStream) out ).toByteArray() );
                try
                {
                    setUserName(fileName, url );
                }
                catch (final IOException e)
                {
                    // Ignore the exception if unable to set the user name.
                }
            }
            if (oldfile != null) {
                ((DefaultFileContent) oldfile.getContent()).resetAttributes();
            }

        }
    }

    private static final String MIME_DIRECTORY = "httpd/unix-directory";
    private final WebdavFileSystemConfigBuilder builder;
    private final VersionableSardine sardine;

    FS fileSystem;

    public WebdavFileObject( final AbstractFileName name, final FS fileSystem, final VersionableSardine sardine )
    {
        super( name, fileSystem );
        this.sardine = sardine;
        this.fileSystem = fileSystem;
        builder = WebdavFileSystemConfigBuilder.getInstance();
    }

    @Override
    public URLFileName getName()
    {
        return (URLFileName) super.getName();
    }

    @Override
    protected void doCreateFolder()
        throws Exception
    {
        sardine.createDirectory( getFullUrl() );
    }

    @Override
    protected void doDelete()
        throws Exception
    {
        sardine.delete( getFullUrl() );
    }

    /**
     * Get Dav resource associated with this file
     * @return
     * @throws IOException 
     */
    private DavResource getResource()
        throws IOException
    {
        URI uri = getFullURI();
        List<DavResource> resources = sardine.list( uri.toString() );
        for ( DavResource res : resources )
        {
            if ( pathsEqual( res.getHref(), uri ) )
            {
                return res;
            }
        }
        return null;
    }

    private boolean maybeAdd( Map<String, Object> map, String label, Object o )
    {

        boolean added = false;
        if ( o != null )
        {
            map.put( label, o );
            added = true;
        }

        return added;
    }

    public String getContentType()
        throws IOException
    {
        final DavResource resource = getResource();
        if ( resource != null )
        {
            return resource.getContentType();
        }
        return null;
    }

    @Override
    protected Map<String, Object> doGetAttributes()
        throws Exception
    {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final DavResource resource = getResource();

        if ( resource != null )
        {
            // build map
            maybeAdd( attributes, "content-language", resource.getContentLanguage() );
            maybeAdd( attributes, "content-length", resource.getContentLength() );
            maybeAdd( attributes, "content-type", resource.getContentType() );
            maybeAdd( attributes, "creation-date", resource.getCreation() );
            maybeAdd( attributes, "display-name", resource.getDisplayName() );
            maybeAdd( attributes, "etag", resource.getEtag() );
            maybeAdd( attributes, "href", resource.getHref() );
            maybeAdd( attributes, "modification-date", resource.getModified() );
            maybeAdd( attributes, "name", resource.getName() );
            maybeAdd( attributes, "path", resource.getPath() );
            for ( Map.Entry<String, String> entry : resource.getCustomProps().entrySet() )
            {
                maybeAdd( attributes, entry.getKey(), entry.getValue() );
            }
        }

        return attributes;
    }

    @Override
    protected long doGetContentSize()
        throws Exception
    {
        final DavResource resource = getResource();
        if ( resource != null )
        {
            return resource.getContentLength();
        }
        return 0;
    }

    @Override
    protected long doGetLastModifiedTime()
        throws Exception
    {
        final DavResource resource = getResource();
        if ( resource != null )
        {
            final Date modified = resource.getModified();
            if ( modified != null )
            {
                return modified.getTime();
            }
        }
        return 0;
    }

    @Override
    protected OutputStream doGetOutputStream( boolean bAppend )
        throws Exception
    {
        return new WebdavOutputStream(this);
    }

    @Override
    protected FileType doGetType()
        throws Exception
    {

        if ( sardine.exists( getFullUrl() ) )
        {
            final DavResource resource = getResource();
            if ( resource != null )
            {
                final String contentType = resource.getContentType();
                if ( contentType != null )
                {
                    if ( MIME_DIRECTORY.equals( contentType ) )
                    {
                        return FileType.FOLDER;
                    }
                    else
                    {
                        return FileType.FILE;
                    }
                }
            }
        }

        return FileType.IMAGINARY;
    }

    @Override
    protected boolean doIsWriteable()
        throws Exception
    {
        return true;
    }

    private boolean isDirectory()
    {
        try
        {
            if ( getType() == FileType.FOLDER )
            {
                return true;
            }
        }
        catch ( FileSystemException e )
        {
        }
        return false;
    }

    @Override
    protected String[] doListChildren()
        throws Exception
    {

        // Shouldn't be called for simple files
        if ( isDirectory() )
        {

            List<DavResource> resources = sardine.list( getFullUrl() );
            List<String> children = new LinkedList<String>();

            for ( DavResource res : resources )
            {
                if ( !res.getHref().equals( getFullUrl() ) )
                {
                    children.add( res.getHref().toString() );
                }
            }
            return children.toArray( new String[children.size()] );
        }
        else
        {
            return null;
        }
    }

    /**
     * Check if the two URIs have same path/query, indicating identical files
     * We have to account for a potential discrepancy with a trailing slash
     * @param a
     * @param b
     * @return
     */
    protected boolean pathsEqual( URI a, URI b )
    {

        if ( a.equals( b ) )
        {
            return true;
        }

        String pa = a.getPath();
        String pb = b.getPath();

        // maybe add slash
        if ( !pa.endsWith( "/" ) && pb.endsWith( "/" ) )
        {
            pa = pa + "/";
        }
        else if ( !pb.endsWith( "/" ) && pa.endsWith( "/" ) )
        {
            pb = pb + "/";
        }

        if ( !pa.equals( pb ) )
        {
            return false;
        }
        String qa = a.getQuery();
        String qb = b.getQuery();

        if ( qa != null && !qa.equals( qb ) )
        {
            return false;
        }
        else if ( qb != null )
        {
            return false;
        }

        return true;
    }

    protected FileObject[] doListChildrenResolved()
        throws Exception
    {
        try
        {
            if ( isDirectory() )
            {
                List<DavResource> resources = sardine.list( getFullUrl() );

                final List<FileObject> vfs = new ArrayList<FileObject>();
                for ( DavResource res : resources )
                {
                    if ( !pathsEqual( stripHost( res.getHref() ), getFullURI() ) )
                    {
                        String resourceName = res.getName();
                        if ( resourceName != null && resourceName.length() > 0 )
                        {
                            FileName fname = fileSystem.getFileSystemManager()
                                .resolveName( getName(), UriParser.encode( resourceName ), NameScope.CHILD );

                            // do not cast, as could be OnCall object
                            final FileObject fo = fileSystem.resolveFile( fname );
                            vfs.add( fo );
                        }
                    }
                }
                return vfs.toArray( new FileObject[vfs.size()] );
            }
            throw new FileNotFolderException( getName() );
        }
        catch ( final FileNotFolderException fnfe )
        {
            throw fnfe;
        }
        catch ( final IOException e )
        {
            throw new FileSystemException( e.getMessage(), e );
        }
    }

    @Override
    protected void doRename( FileObject newFile )
        throws Exception
    {
        final String from = getFullUrl();
        final String to = getFullUrl( (URLFileName) newFile.getName(), false );
        sardine.move( from, to );

    }

    @Override
    protected void doSetAttribute( String attrName, Object value )
        throws Exception
    {
        Map<String, String> properties = new HashMap<String, String>( 1 );
        properties.put( attrName, value.toString() );
        sardine.patch( getFullUrl(), SardineUtil.toQName( properties ), Collections.<QName> emptyList() );
    }

    @Override
    protected FileContentInfoFactory getFileContentInfoFactory()
    {
        return new WebdavFileContentInfoFactory();
    }

    @Override
    protected InputStream doGetInputStream()
        throws Exception
    {
        InputStream stream = null;

        try
        {
            stream = sardine.get( getFullUrl() );
        }
        catch ( SardineException e )
        {
            int code = e.getStatusCode();
            if ( code == HttpStatus.SC_NOT_FOUND )
            {
                throw new FileNotFoundException( "file not found", e );
            }

            // otherwise re-throw exceptions
            throw e;
        }

        return stream;
    }

    @Override
    protected boolean doIsSameFile( FileObject destFile )
        throws FileSystemException
    {
        return destFile.getURL().equals( getURL() );
    }

    @Override
    protected void doRemoveAttribute( String attrName )
        throws Exception
    {
        List<String> properties = new LinkedList<String>();
        properties.add( attrName );
        sardine.patch( getFullUrl(), Collections.<QName, String> emptyMap(), SardineUtil.toQName( properties ) );
    }

    private String getFullUrl()
        throws FileSystemException
    {
        URI uri = getFullURI();
        return uri.toString();
    }

    private String getFullUrl( URLFileName file, boolean addIdentity )
        throws FileSystemException
    {
        URI uri = getFullURI( file, addIdentity );
        return uri.toString();
    }

    private URI getFullURI()
        throws FileSystemException
    {
        return getFullURI( (URLFileName) getName(), true );
    }

    private URI getFullURI( URLFileName file, boolean addIdentity )
        throws FileSystemException
    {
        URI uri = null;

        // replace scheme (webdav(s) instead of http(s))
        try
        {
            uri = new URI( file.getURI() );
            HttpHost host = fileSystem.getHost();
            // replace scheme with that from host
            String id = null;
            if (addIdentity) {
                id = uri.getUserInfo();
            }
            uri = new URI( host.getSchemeName(), id, uri.getHost(), uri.getPort(), uri.getPath(),
                           uri.getQuery(), uri.getFragment() );
        }
        catch ( URISyntaxException se )
        {
            throw new FileSystemException( "Invalid URI syntax", se );
        }

        return uri;
    }

    // XXX ideally, I would use host-relative urls, since the client has a host specified
    //     however, the specs for Sardine say it takes a full url

    @SuppressWarnings("unused")
    private String getHostRelativeUrl( URLFileName file )
        throws FileSystemException
    {
        URI uri = getHostRelativeURI( file );
        return uri.toString();
    }

    @SuppressWarnings("unused")
    private String getHostRelativeUrl()
        throws FileSystemException
    {
        URI uri = getHostRelativeURI();
        return uri.toString();
    }

    private URI getHostRelativeURI()
        throws FileSystemException
    {
        URLFileName file = ( (URLFileName) getName() );
        return getHostRelativeURI( file );
    }

    private URI getHostRelativeURI( URLFileName file )
        throws FileSystemException
    {
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

        return uri;
    }

    private static URI stripHost( URI uri )
    {
        URI out = null;
        try
        {
            out = new URI( null, null, null, -1, uri.getPath(), uri.getQuery(), uri.getFragment() );
        }
        catch ( URISyntaxException e )
        {
            // never here
        }

        return out;
    }

}
