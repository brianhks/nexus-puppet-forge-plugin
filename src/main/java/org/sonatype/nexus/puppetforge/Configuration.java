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
