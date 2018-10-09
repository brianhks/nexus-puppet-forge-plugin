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
