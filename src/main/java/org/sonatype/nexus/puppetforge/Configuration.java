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

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.maven.metadata.operations.AddVersionOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 Created by bhawkins on 8/8/16.
 */
public class Configuration
{
	public static final String CONFIG_FILE = "puppet-forge.properties";
	private static Configuration s_configuration;
	private static Object s_lock = new Object();

	private Properties m_properties;

	public static Configuration getConfigueration(NexusConfiguration nexusConfiguration) throws IOException
	{
		if (s_configuration == null)
		{
			synchronized (s_lock)
			{
				if (s_configuration == null)
				{
					s_configuration = new Configuration(nexusConfiguration);
				}
			}
		}

		return s_configuration;
	}

	private Configuration(NexusConfiguration nexusConfiguration) throws IOException
	{
		File configurationDirectory = nexusConfiguration.getConfigurationDirectory();
		File configurations = new File(configurationDirectory, CONFIG_FILE);
		if (configurations.exists())
		{
			m_properties = new Properties();
			FileInputStream fileInputStream = new FileInputStream(configurations);
			m_properties.load(fileInputStream);
			fileInputStream.close();
		}
	}

	public List<String> getRepositoryList()
	{
		List<String> ret;
		String property = m_properties.getProperty("puppet-forge.repository.list");
		if (property != null)
			ret = Arrays.asList(property.split(","));
		else
			ret = Collections.EMPTY_LIST;

		return ret;
	}

	public boolean useReleaseFullUri()
	{
		String property = m_properties.getProperty("puppet-forge.release.full_uri");
		if (property == null)
			property = "true";

		return Boolean.parseBoolean(property);
	}
}
