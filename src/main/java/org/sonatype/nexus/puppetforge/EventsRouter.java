package org.sonatype.nexus.puppetforge;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.sisu.EagerSingleton;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.siesta.server.ApplicationSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/9/15.
 */
@Named
@EagerSingleton
public class EventsRouter extends ApplicationSupport
{
	private Provider<RepositoryRegistry> m_repositoryRegistryProvider;
	private NexusConfiguration m_nexusConfiguration;
	private Gson m_gson;

	@Inject
	public EventsRouter(final Provider<RepositoryRegistry> repositoryRegistryProvider,
			final NexusConfiguration nexusConfiguration,
			final EventBus eventBus)
	{
		//log.info("+++++++++++++++EventRouter++++++++++++++++++++++");

		m_repositoryRegistryProvider = checkNotNull(repositoryRegistryProvider);
		checkNotNull(eventBus).register(this);
		m_nexusConfiguration = checkNotNull(nexusConfiguration);

		GsonBuilder builder = new GsonBuilder();
		m_gson = builder.create();
	}

	private File extractMetadata(MavenRepository repository, String groupId,
			String artifactId, String version, File moduleArchive) throws IOException, ItemNotFoundException, IllegalOperationException, AccessDeniedException, UnsupportedStorageOperationException
	{
		String tempFolderName = artifactId+"-"+version;
		File extractFolder = new File(m_nexusConfiguration.getTemporaryDirectory(),
				tempFolderName);

		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
		log.info("Extracting to " + extractFolder.getAbsolutePath());
		archiver.extract(moduleArchive, extractFolder);

		File metadataFile = new File(extractFolder, groupId+"-"+
				artifactId+"-"+
				version+"/metadata.json");

		log.info("Metadata file exists: "+metadataFile.exists());
		//todo if metadata is missing then bail

		//Store metadata.json as separate file
		Map<String, String> attributes = new HashMap<String, String>();
		Gav jsonGav = new Gav(
				groupId,
				artifactId,
				version,
				null,
				"json",
				null,
				null,
				null,
				false,
				null,
				false,
				null);
		ArtifactStoreRequest artifactStoreRequest = new ArtifactStoreRequest(
				repository, jsonGav, true);
		repository.getArtifactStoreHelper().storeArtifact(artifactStoreRequest,
				new FileInputStream(metadataFile), attributes);

		return metadataFile;
	}

	private void createPom(File metadataFile, MavenRepository repository,
			String groupId, String artifactId, String version) throws IOException, ItemNotFoundException, IllegalOperationException, AccessDeniedException, UnsupportedStorageOperationException
	{
		/*
				Create object to read json with gson
				Create and write pom file.
				 */
		Metadata metadata = m_gson.fromJson(new FileReader(metadataFile), Metadata.class);

		Model model = new Model();
		String[] nameSplit = metadata.getName().split("-");
		model.setArtifactId(nameSplit[1]);
		model.setGroupId(nameSplit[0]);
		model.setVersion(metadata.getVersion());
		model.setDescription(metadata.getDescription());
		model.setPackaging("tar.gz");

		Scm scm = new Scm();
		scm.setUrl(metadata.getSource());
		model.setScm(scm);

		License license = new License();
		license.setName(metadata.getLicense());

		model.setLicenses(Arrays.asList(license));

		MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();

		StringWriter sw = new StringWriter();
		mavenXpp3Writer.write(sw, model);

		Gav pomGav = new Gav(
				groupId,
				artifactId,
				version,
				null,
				"pom",
				null,
				null,
				null,
				false,
				null,
				false,
				null);

		ArtifactStoreRequest pomRequest = new ArtifactStoreRequest(
				repository, pomGav, true);

		Map<String, String> attributes = new HashMap<String, String>();
		repository.getArtifactStoreHelper().storeArtifactPom(
				pomRequest, new ByteArrayInputStream(sw.toString().getBytes()),
				attributes);
	}

	@AllowConcurrentEvents
	@Subscribe
	public void on(final RepositoryItemEventStore eventStore)
	{
		//log.info("|||||||||| I GOT AN EVENT |||||||||||||");
		StorageItem eventStoreItem = eventStore.getItem();
		Repository repository = eventStore.getRepository();
		String repositoryName = repository.getName();

		/*
		 Repository name must contain the word 'puppet' and it must be a Maven
		 repository.
		 */
		if (repositoryName.toLowerCase().contains("puppet") &&
				repository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class) &&
				eventStoreItem.getName().endsWith(".tar.gz")) //todo add other conditions
		{
			try
			{
				MavenRepository mrepository = repository.adaptToFacet(MavenRepository.class);
				Gav gav = mrepository.getGavCalculator().pathToGav(eventStoreItem.getPath());

				File baseDir = RepositoryUtils.getBaseDir(repository);
				File moduleArchive = new File(baseDir, eventStoreItem.getPath());

				File metadataFile = extractMetadata(mrepository, gav.getGroupId(),
						gav.getArtifactId(), gav.getVersion(), moduleArchive);

				createPom(metadataFile, mrepository, gav.getGroupId(), gav.getArtifactId(),
						gav.getVersion());

			}
			catch (Exception e)
			{
				log.error("Error in puppet plugin event router", e);
			}

		}
	}
}
