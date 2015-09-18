package org.sonatype.nexus.puppetforge.rest;

import javax.ws.rs.core.Response;

/**
 Created by bhawkins on 9/17/15.
 */
public class RestException extends Exception
{
	private Response.Status m_status;

	public RestException(Response.Status status, String message)
	{
		super(message);
		m_status = status;
	}

	public Response.Status getStatus()
	{
		return m_status;
	}
}
