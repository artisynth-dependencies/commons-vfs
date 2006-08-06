/*
 * Copyright 2002-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs.operations.vcs;

/**
 * todo: add class description here
 * 
 * @author Siarhei Baidun
 * @since 0.1
 */
public interface VcsUpdateListener
{
	/**
	 * 
	 * @param path
	 * @param revision
	 * @param contentStatus
	 *            takes one of the values as defined in the
	 * @see VcsStatus constants.
	 */
	void updated(final String path, final long revision, final int contentStatus);
}