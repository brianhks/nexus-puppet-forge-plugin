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
