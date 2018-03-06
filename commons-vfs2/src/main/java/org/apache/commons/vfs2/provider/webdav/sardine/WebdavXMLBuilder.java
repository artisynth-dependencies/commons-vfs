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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Builds XML content for WebDav commands.  Takes over from Sardine for building
 * PROPFIND commands, since Sardine doesn't support the "D:include" element. 
 * @author Antonio
 *
 */
public class WebdavXMLBuilder
{
    
    private static String fill(int idx, int length, StringBuilder prefixChars, Set<String> prefixes) {
        
        if (idx == length) {
            String prefix = prefixChars.toString();
            if (!prefixes.contains( prefix )) {
                return prefix;
            }
            return null;
        }
        
        for (char c='A'; c<='Z'; ++c) {
            prefixChars.setCharAt( idx, c );
            String prefix = fill(idx+1, length, prefixChars, prefixes);
            if (prefix != null) {
                return prefix;
            }
        }
        
        return null;
    }
    
    /**
     * Generates a new prefix not contained in the given set.  Loops up from 'A' to 'Z',
     * adding a new character until a unique prefix is found
     * @param prefixes
     * @return
     */
    private static String generatePrefix(Set<String> prefixes) {
    
        int i = 1;
        while(true) {
            StringBuilder sb = new StringBuilder();
            String prefix = fill(0, i, sb, prefixes );
            if (prefix != null) {
                return prefix;
            }
            ++i;
        }
        
        
    }
    
    /**
     * Builds PROPFIND content 
     * @param addAllprop if true adds the &lt;D:allprop/&gt; element
     * @param props list of properties to include
     * @param pretty if true, add indents and newlines
     * @return xml content as a string 
     */
    public static String buildPropfind(boolean addAllprop, Set<QName> props, boolean pretty)  {
    
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware( true );

        DocumentBuilder docBuilder = null;
        try
        {
            docBuilder = docFactory.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e )
        {
            throw new RuntimeException(e);
        }

        // root elements
        Document doc = docBuilder.newDocument();
        doc.setXmlStandalone(true);
        Element propfind = doc.createElementNS("DAV:", "D:propfind" );
        propfind.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns:D", "DAV:" );
        doc.appendChild( propfind );
        
        HashMap<String,String> nsToPrefix =  new HashMap<String,String>();
        nsToPrefix.put( "DAV:", "D" );
        nsToPrefix.put( "xml:", "xml" );
        nsToPrefix.put( "http://www.w3.org/2000/xmlns/", "xmlns" );
        
        HashSet<String> prefixes = new HashSet<String>();
        prefixes.addAll( nsToPrefix.values() );
        
        if (addAllprop) {
            Element allprop = doc.createElementNS("DAV:",  nsToPrefix.get( "DAV:" ) + ":allprop" );
            propfind.appendChild( allprop );
            
            if (props != null && props.size() > 0) {
                Element include = doc.createElementNS( "DAV:", nsToPrefix.get( "DAV:" ) + ":include" );
                propfind.appendChild( include );
                
                for (QName prop : props) {
                    String prefix = nsToPrefix.get(prop.getNamespaceURI());
                    if (prefix ==  null) {
                        prefix = prop.getPrefix();
                        if (prefixes.contains( prefix )) {
                            prefix = generatePrefix(prefixes);
                        }
                        prefixes.add( prefix );
                        nsToPrefix.put( prefix, prop.getNamespaceURI() );
                    }
                    Element pe = doc.createElementNS( prop.getNamespaceURI(), prefix + ":" + prop.getLocalPart() );
                    include.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns:" + prefix, prop.getNamespaceURI() );
                    include.appendChild( pe );
                }
            }
        } else {
            if (props != null && props.size() > 0) {
                Element eprop = doc.createElementNS( "DAV:", nsToPrefix.get( "DAV:" ) + ":prop" );
                propfind.appendChild( eprop );
                
                for (QName prop : props) {
                    String prefix = nsToPrefix.get(prop.getNamespaceURI());
                    if (prefix ==  null) {
                        prefix = prop.getPrefix();
                        if (prefixes.contains( prefix )) {
                            prefix = generatePrefix(prefixes);
                        }
                        prefixes.add( prefix );
                        nsToPrefix.put( prefix, prop.getNamespaceURI() );
                    }
                    Element pe = doc.createElementNS( prop.getNamespaceURI(), prefix + ":" + prop.getLocalPart() );
                    eprop.setAttributeNS( "http://www.w3.org/2000/xmlns/", "xmlns:" + prefix, prop.getNamespaceURI() );
                    eprop.appendChild( pe );
                }
            }
        }
        
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("indent-number", 2);
        Transformer transformer;
        try
        {
            transformer = tf.newTransformer();
        }
        catch ( TransformerConfigurationException e )
        {
            throw new RuntimeException(e);
        }
        if (pretty) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        StringWriter writer = new StringWriter();
        try
        {
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        }
        catch ( TransformerException e )
        {
            throw new RuntimeException(e);
        }
        
        return writer.toString();
        
    }
    
    public static void main( String[] args )
    {
        Set<QName> set = new HashSet<QName>();
        set.add( DavResources.AUTO_VERSION );
        set.add( DavResources.COMMENT );
        set.add( new QName("DAV:", "testprefix", "R") );
        set.add( new QName("TEST:", "testns", "S") );
        
        String xml = buildPropfind( false, set, false);
        System.out.println( xml );
    }
    
}
