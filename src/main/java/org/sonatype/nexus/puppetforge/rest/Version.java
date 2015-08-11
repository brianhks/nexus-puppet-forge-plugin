package org.sonatype.nexus.puppetforge.rest;

import java.io.Serializable;

/**
 Created by bhawkins on 8/10/15.
 */
public class Version
	implements Serializable
{
	private String version;

	public Version(String version)
	{
		this.version = version;
	}
}
