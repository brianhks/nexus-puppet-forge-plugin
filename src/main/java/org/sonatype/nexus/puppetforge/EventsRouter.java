package org.sonatype.nexus.puppetforge;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/9/15.
 */
@Named
@EagerSingleton
public class EventsRouter
{
	private Provider<RepositoryRegistry> m_repositoryRegistryProvider;

	@Inject
	public EventsRouter(final Provider<RepositoryRegistry> repositoryRegistryProvider,
			final EventBus eventBus)
	{
		//This is interesting
		//if (request.isExternal() && getRepositoryKind().isFacetAvailable(MavenHostedRepository.class)) {

		//AbstractMavenRepository.recreateMavenMetadata() copy this stuff

		m_repositoryRegistryProvider = checkNotNull(repositoryRegistryProvider);
		checkNotNull(eventBus).register(this);
	}

	@AllowConcurrentEvents
	@Subscribe
	public void on(final RepositoryItemEventStore eventStore)
	{
		//Verify that it is a puppet module
		//Extract to temp location
		//Parse metadata.json
	}
}
