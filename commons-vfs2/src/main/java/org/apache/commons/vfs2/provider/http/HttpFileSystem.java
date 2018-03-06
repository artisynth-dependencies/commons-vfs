/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.provider.http;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.GenericFileName;

/**
 * An HTTP file system.
 */
public class HttpFileSystem extends AbstractFileSystem {
    private final HttpConnectionClientManager clientManager;
    private final HttpConnectionClient client;
    
    protected HttpFileSystem(final GenericFileName rootName, final HttpConnectionClientManager clientManager,
            final FileSystemOptions fileSystemOptions) {
        super(rootName, null, fileSystemOptions);
        this.clientManager = clientManager;
        this.client = clientManager.createClient();
    }

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps) {
        caps.addAll(HttpFileProvider.capabilities);
    }
    
    protected HttpConnectionClientManager getClientManager() {
    	return clientManager;
    }

    protected HttpConnectionClient getClient() {
        return client;
    }

    /** @since 2.0 */
    @Override
    public void closeCommunicationLink() {
        final HttpConnectionClientManager manager = getClientManager();
        if ( manager != null ) {
            manager.shutdown();
        }
    }

    /**
     * Creates a file object. This method is called only if the requested file is not cached.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name) throws Exception {
        return new HttpFileObject<>(name, this);
    }
}
