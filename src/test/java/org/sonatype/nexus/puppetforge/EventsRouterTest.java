package org.sonatype.nexus.puppetforge;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.when;

/**
 Created by bhawkins on 10/13/16.
 */
public class EventsRouterTest
{

	@Test
	public void test_extractModel_dashInDependency() throws IOException
	{
		EventBus eventBus = Mockito.mock(EventBus.class);
		NexusConfiguration nexusConfiguration = Mockito.mock(NexusConfiguration.class);

		when(nexusConfiguration.getConfigurationDirectory()).thenReturn(new File("src/test/resources"));

		EventsRouter eventsRouter = new EventsRouter(nexusConfiguration, eventBus);

		File metadata = new File("src/test/resources/dependency_with_dash.json");

		eventsRouter.extractModel(metadata);
	}

	@Test
	public void test_extractModel_noGroupSeparator() throws IOException
	{
		EventBus eventBus = Mockito.mock(EventBus.class);
		NexusConfiguration nexusConfiguration = Mockito.mock(NexusConfiguration.class);

		when(nexusConfiguration.getConfigurationDirectory()).thenReturn(new File("src/test/resources"));

		EventsRouter eventsRouter = new EventsRouter(nexusConfiguration, eventBus);

		File metadata = new File("src/test/resources/dependency_no_separator.json");

		eventsRouter.extractModel(metadata);
	}
}
