/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.nexus.puppetforge.rest;

import org.sonatype.nexus.proxy.item.StorageFileItem;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 Created by bhawkins on 10/24/16.
 */
public class StorageFileStreamingOutput implements StreamingOutput
{
	private final StorageFileItem m_fileItem;

	public StorageFileStreamingOutput(StorageFileItem storageFileItem)
	{
		m_fileItem = storageFileItem;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException
	{
		InputStream reader = m_fileItem.getInputStream();

		try
		{
			byte[] buffer = new byte[1024];
			int size;

			while ((size = reader.read(buffer)) != -1)
			{
				output.write(buffer, 0, size);
			}

			output.flush();
		}
		finally
		{
			reader.close();
		}
	}
}
