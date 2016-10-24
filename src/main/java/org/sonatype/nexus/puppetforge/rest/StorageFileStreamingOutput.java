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
