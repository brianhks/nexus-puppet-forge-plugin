package org.sonatype.nexus.puppetforge;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.model.Dependency;
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
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.siesta.server.ApplicationSupport;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/9/15.
 */
@Named
@EagerSingleton
public class EventsRouter extends ApplicationSupport
{
	private NexusConfiguration m_nexusConfiguration;
	private Gson m_gson;
	private Set<String> m_puppetRepositories = new HashSet<>();

	@Inject
	public EventsRouter(
			final NexusConfiguration nexusConfiguration,
			final EventBus eventBus) throws IOException
	{
		//log.info("+++++++++++++++EventRouter++++++++++++++++++++++");

		checkNotNull(eventBus).register(this);
		m_nexusConfiguration = checkNotNull(nexusConfiguration);

		Configuration configuration = Configuration.getConfigueration(nexusConfiguration);
		m_puppetRepositories.addAll(configuration.getRepositoryList());

		GsonBuilder builder = new GsonBuilder();
		m_gson = builder.create();
	}

	private void deltree(File directory)
	{
		if (!directory.exists())
			return;
		File[] list = directory.listFiles();

		if ((list != null ? list.length : 0) > 0)
		{
			for (File aList : list)
			{
				if (aList.isDirectory())
					deltree(aList);

				aList.delete();
			}
		}

		directory.delete();
	}

	private File extractMetadata(MavenRepository repository, String groupId,
			String artifactId, String version, File moduleArchive) throws IOException, ItemNotFoundException, IllegalOperationException, AccessDeniedException, UnsupportedStorageOperationException
	{
		String tempFolderName = artifactId+"-"+version;
		File extractFolder = new File(m_nexusConfiguration.getTemporaryDirectory(),
				tempFolderName);

		deltree(extractFolder);

		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
		log.info("Extracting to " + extractFolder.getAbsolutePath());
		archiver.extract(moduleArchive, extractFolder);

		File metadataFile = new File(extractFolder, groupId+"-"+
				artifactId+"-"+
				version+"/metadata.json");

		if (!metadataFile.exists())
		{
			log.warn("Metadata file "+metadataFile.getAbsolutePath() + " does not exist!");
			return null;
		}

		//Store metadata.json as separate file
		Map<String, String> attributes = new HashMap<>();
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

		FileInputStream metaInputStream = new FileInputStream(metadataFile);

		repository.getArtifactStoreHelper().storeArtifact(artifactStoreRequest,
				metaInputStream, attributes);

		metaInputStream.close();


		return metadataFile;
	}


	Model extractModel(File metadataFile) throws IOException
	{
		FileReader metaReader = new FileReader(metadataFile);

		Metadata metadata = m_gson.fromJson(metaReader, Metadata.class);

		metaReader.close();

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

		model.setLicenses(Collections.singletonList(license));

		for (Metadata.Dependency dependency : metadata.getDependencies())
		{
			//Figure out what character is used to split group id
			String splitChar = "/";
			if (!dependency.getName().contains("/") && dependency.getName().contains("-"))
				splitChar = "-";

			String[] depNameSplit = dependency.getName().split(splitChar);
			if (depNameSplit.length != 2)
			{
				log.warn("Unable to parse dependency name {} for module {}", dependency.getName(), metadata.getName());
				continue;
			}

			Dependency pomDep = new Dependency();
			pomDep.setGroupId(depNameSplit[0]);
			pomDep.setArtifactId(depNameSplit[1]);
			pomDep.setVersion(VersionTranslator.translateVersion(dependency.getVersion_requirement(), log));

			model.addDependency(pomDep);
		}

		return model;
	}


	private void createPom(File metadataFile, MavenRepository repository,
			String groupId, String artifactId, String version) throws IOException, ItemNotFoundException, IllegalOperationException, AccessDeniedException, UnsupportedStorageOperationException
	{
		/*
				Create object to read json with gson
				Create and write pom file.
				 */
		Model model = extractModel(metadataFile);

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

		Map<String, String> attributes = new HashMap<>();
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
		String repositoryId = repository.getId();

		/*
		 Repository name must be configured in puppet-forge.properties and it must be a Maven
		 repository.
		 */
		if (m_puppetRepositories.contains(repositoryId) &&
				repository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class) &&
				eventStoreItem.getName().endsWith(".tar.gz"))
		{
			try
			{
				MavenRepository mrepository = repository.adaptToFacet(MavenRepository.class);
				Gav gav = mrepository.getGavCalculator().pathToGav(eventStoreItem.getPath());

				File baseDir = RepositoryUtils.getBaseDir(repository);
				File moduleArchive = new File(baseDir, eventStoreItem.getPath());

				File metadataFile = extractMetadata(mrepository, gav.getGroupId(),
						gav.getArtifactId(), gav.getVersion(), moduleArchive);

				if (metadataFile != null)
				{
					createPom(metadataFile, mrepository, gav.getGroupId(), gav.getArtifactId(),
							gav.getVersion());
				}

			}
			catch (Exception e)
			{
				log.error("Error in puppet plugin event router", e);
			}

		}
	}
}
