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
package org.apache.commons.vfs2.provider.webdav.sardine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.w3c.dom.Element;

import com.github.sardine.Sardine;

/**
 * An extension of Sardine commands allowing for custom response handlers
 * @author Antonio
 *
 */
public interface HttpAwareSardine extends Sardine
{
    /**
     * Gets a directory listing using WebDAV <code>PROPFIND</code>.
     *
     * @param url Path to the resource including protocol and hostname
     * @param handler for the returned PROPFIND response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T list(String url, ResponseHandler<T> handler) throws IOException;

    /**
     * Gets a directory listing using WebDAV <code>PROPFIND</code>.
     *
     * @param url   Path to the resource including protocol and hostname
     * @param depth The depth to look at (use 0 for single ressource, 1 for directory listing)
     * @param handler for the returned PROPFIND response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T list(String url, int depth, ResponseHandler<T> handler) throws IOException;

    /**
     * Gets a directory listing using WebDAV <code>PROPFIND</code>.
     *
     * @param url   Path to the resource including protocol and hostname
     * @param depth The depth to look at (use 0 for single ressource, 1 for directory listing)
     * @param props Additional properties which should be requested (excluding default props).
     * @param handler for the returned PROPFIND response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T list(String url, int depth, Set<QName> props, ResponseHandler<T> handler) throws IOException;

    /**
     * Perform a search of the Webdav repository using WebDAV <code>SEARCH</code>.
     * @param url The base resource to search from.
     * @param language The language the query is formed in.
     * @param query The query string to be processed by the webdav server.
     * @param handler for the returned SEARCH response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure.
     */
    public<T> T search(String url, String language, String query, ResponseHandler<T> handler) throws IOException;


    /**
     * Add custom properties for a url WebDAV <code>PROPPATCH</code>.
     *
     * @param url     Path to the resource including protocol and hostname
     * @param addProps Properties to add to resource. If a property already exists then its value is replaced.
     * @param handler for the returned PROPPATCH response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T patch(String url, Map<QName, String> addProps, ResponseHandler<T> handler) throws IOException;

    /**
     * Add or remove custom properties for a url using WebDAV <code>PROPPATCH</code>.
     *
     * @param url        Path to the resource including protocol and hostname
     * @param addProps  Properties to add to resource. If a property already exists then its value is replaced.
     * @param removeProps Properties to remove from resource. Specifying the removal of a property that does not exist is not an error.
     * @param handler for the returned PROPPATCH response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T patch(String url, Map<QName, String> addProps, List<QName> removeProps, ResponseHandler<T> handler) throws IOException;

    /**
     * Add or remove custom properties for a url using WebDAV <code>PROPPATCH</code>.
     *
     * @param url        Path to the resource including protocol and hostname
     * @param addProps  Properties to add to resource. If a property already exists then its value is replaced.
     * @param removeProps Properties to remove from resource. Specifying the removal of a property that does not exist is not an error.
     * @param handler for the returned PROPPATCH response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T patch(String url, List<Element> addProps, List<QName> removeProps, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses HTTP <code>GET</code> to download data from a server. The stream must be closed after reading.
     *
     * @param url Path to the resource including protocol and hostname
     * @param handler for the returned GET response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T get(String url, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses HTTP <code>GET</code> to download data from a server. The stream must be closed after reading.
     *
     * @param url    Path to the resource including protocol and hostname
     * @param headers Additional HTTP headers to add to the request
     * @param handler for the returned GET response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T get(String url, Map<String, String> headers, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses HTTP <code>PUT</code> to send data to a server. Repeatable on authentication failure.
     *
     * @param url  Path to the resource including protocol and hostname
     * @param data Input source
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T put(String url, byte[] data, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server. Not repeatable on authentication failure.
     *
     * @param url       Path to the resource including protocol and hostname
     * @param dataStream Input source
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T put(String url, InputStream dataStream, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server with a specific content type
     * header. Repeatable on authentication failure.
     *
     * @param url        Path to the resource including protocol and hostname
     * @param data      Input source
     * @param contentType MIME type to add to the HTTP request header
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T put(String url, byte[] data, String contentType, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server with a specific content
     * type header. Not repeatable on authentication failure.
     *
     * @param url        Path to the resource including protocol and hostname
     * @param dataStream  Input source
     * @param contentType MIME type to add to the HTTP request header
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T put(String url, InputStream dataStream, String contentType, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server with a specific content
     * type header. Not repeatable on authentication failure.
     *
     * @param url           Path to the resource including protocol and hostname
     * @param dataStream     Input source
     * @param contentType   MIME type to add to the HTTP request header
     * @param expectContinue Enable <code>Expect: continue</code> header for <code>PUT</code> requests.
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T put(String url, InputStream dataStream, String contentType, boolean expectContinue,
                     ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server with a specific content
     * type header. Not repeatable on authentication failure.
     *
     * @param url           Path to the resource including protocol and hostname
     * @param dataStream     Input source
     * @param contentType   MIME type to add to the HTTP request header
     * @param expectContinue Enable <code>Expect: continue</code> header for <code>PUT</code> requests.
     * @param contentLength data size in bytes to set to Content-Length header
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T put(String url, InputStream dataStream, String contentType, boolean expectContinue, 
             long contentLength, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to send data to a server with specific headers. Not repeatable
     * on authentication failure.
     *
     * @param url       Path to the resource including protocol and hostname
     * @param dataStream Input source
     * @param headers   Additional HTTP headers to add to the request
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T put(String url, InputStream dataStream, Map<String, String> headers,
                     ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to upload file to a server with specific contentType. 
     * Repeatable on authentication failure.
     *
     * @param url       Path to the resource including protocol and hostname
     * @param localFile local file to send
     * @param contentType   MIME type to add to the HTTP request header
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T put(String url, File localFile, String contentType, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses <code>PUT</code> to upload file to a server with specific contentType. 
     * Repeatable on authentication failure.
     *
     * @param url       Path to the resource including protocol and hostname
     * @param localFile local file to send
     * @param contentType   MIME type to add to the HTTP request header
     * @param expectContinue Enable <code>Expect: continue</code> header for <code>PUT</code> requests.
     * @param handler for the returned PUT response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T put(String url, File localFile, String contentType, boolean expectContinue,
             ResponseHandler<T> handler) throws IOException;

    /**
     * Delete a resource using HTTP <code>DELETE</code> at the specified url
     *
     * @param url Path to the resource including protocol and hostname
     * @param handler for the returned <code>DELETE</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T delete(String url, ResponseHandler<T> handler) throws IOException;

    /**
     * Uses WebDAV <code>MKCOL</code> to create a directory at the specified url
     *
     * @param url Path to the resource including protocol and hostname
     * @param handler for the returned <code>MKCOL</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T createDirectory(String url, ResponseHandler<T> handler) throws IOException;

    /**
     * Move a url to from source to destination using WebDAV <code>MOVE</code>. Assumes overwrite.
     *
     * @param sourceUrl   Path to the resource including protocol and hostname
     * @param destinationUrl Path to the resource including protocol and hostname
     * @param handler for the returned <code>MOVE</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T move(String sourceUrl, String destinationUrl, ResponseHandler<T> handler) throws IOException;

    /**
     * Move a url to from source to destination using WebDAV <code>MOVE</code>.
     *
     * @param sourceUrl   Path to the resource including protocol and hostname
     * @param destinationUrl Path to the resource including protocol and hostname
     * @param overwrite {@code true} to overwrite if the destination exists, {@code false} otherwise.
     * @param handler for the returned <code>MOVE</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public <T> T move(String sourceUrl, String destinationUrl, boolean overwrite, ResponseHandler<T> handler) throws IOException;

    /**
     * Copy a url from source to destination using WebDAV <code>COPY</code>. Assumes overwrite.
     *
     * @param sourceUrl   Path to the resource including protocol and hostname
     * @param destinationUrl Path to the resource including protocol and hostname
     * @param handler for the returned <code>COPY</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T copy(String sourceUrl, String destinationUrl, ResponseHandler<T> handler) throws IOException;

    /**
     * Copy a url from source to destination using WebDAV <code>COPY</code>.
     *
     * @param sourceUrl   Path to the resource including protocol and hostname
     * @param destinationUrl Path to the resource including protocol and hostname
     * @param overwrite {@code true} to overwrite if the destination exists, {@code false} otherwise.
     * @param handler for the returned <code>COPY</code> response
     * @return output of the response handler
     * @throws IOException I/O error or HTTP response validation failure
     */
    public<T> T copy(String sourceUrl, String destinationUrl, boolean overwrite, ResponseHandler<T> handler) throws IOException;

    /**
     * Executes a custom HTTP request
     * @param url   Path to the resource including protocol and hostname
     * @param request the request to execute
     * @param handler for the response to the provided request
     * @return
     * @throws IOException 
     */
    public<T> T execute(HttpRequest request, ResponseHandler<T> handler) throws IOException;

    /**
     * Executes a custom HTTP request
     * @param url   Path to the resource including protocol and hostname
     * @param request the request to execute
     * @return
     * @throws IOException 
     */
    HttpResponse execute( HttpRequest request ) throws IOException;
    
}
