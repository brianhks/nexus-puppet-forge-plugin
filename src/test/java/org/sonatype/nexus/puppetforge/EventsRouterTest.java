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
