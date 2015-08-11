package org.sonatype.nexus.puppetforge.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by bhawkins on 8/10/15.
 */
public class Release
	implements Serializable
{
	private List<Version> releases = new ArrayList<>();

	public void addVersion(Version version)
	{
		releases.add(version);
	}
}
