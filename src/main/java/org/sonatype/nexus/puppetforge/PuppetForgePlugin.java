package org.sonatype.nexus.puppetforge;


import org.jetbrains.annotations.NonNls;
import org.sonatype.nexus.plugin.PluginIdentity;

import javax.inject.Inject;

/**
 Created by bhawkins on 8/8/15.
 */
public class PuppetForgePlugin
		extends PluginIdentity
{
	/**
	 * Prefix for ID-like things.
	 */
	@NonNls
	public static final String ID_PREFIX = "puppetforge";

	/**
	 * Expected groupId for plugin artifact.
	 */
	@NonNls
	public static final String GROUP_ID = "org.sonatype.nexus.plugins";

	/**
	 * Expected artifactId for plugin artifact.
	 */
	@NonNls
	public static final String ARTIFACT_ID = "nexus-" + ID_PREFIX + "-plugin";

	public static final String URI_PREFIX = "/"+ID_PREFIX+"/{repo}/v3";

	@Inject
	public PuppetForgePlugin() throws Exception
	{
		super(GROUP_ID, ARTIFACT_ID);
	}
}
