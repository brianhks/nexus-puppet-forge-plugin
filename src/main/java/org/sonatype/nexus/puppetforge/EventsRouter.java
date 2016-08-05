package org.sonatype.nexus.puppetforge;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
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
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.siesta.server.ApplicationSupport;
import com.github.jknack.semver.RelationalOp;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/9/15.
 */
@Named
@EagerSingleton
public class EventsRouter extends ApplicationSupport
{
	public static final String CONFIG_FILE = "puppet-forge.properties";


	private Provider<RepositoryRegistry> m_repositoryRegistryProvider;
	private NexusConfiguration m_nexusConfiguration;
	private Gson m_gson;
	private Set<String> m_puppetRepositories = new HashSet<String>();

	@Inject
	public EventsRouter(final Provider<RepositoryRegistry> repositoryRegistryProvider,
			final NexusConfiguration nexusConfiguration,
			final EventBus eventBus) throws IOException
	{
		//log.info("+++++++++++++++EventRouter++++++++++++++++++++++");

		m_repositoryRegistryProvider = checkNotNull(repositoryRegistryProvider);
		checkNotNull(eventBus).register(this);
		m_nexusConfiguration = checkNotNull(nexusConfiguration);

		File configurationDirectory = m_nexusConfiguration.getConfigurationDirectory();
		File configurations = new File(configurationDirectory, CONFIG_FILE);
		if (configurations.exists())
		{
			Properties props = new Properties();
			props.load(new FileInputStream(configurations));
			String repoList = props.getProperty("puppet-forge.repository.list");
			m_puppetRepositories.addAll(Arrays.asList(repoList.split(",")));
		}

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

	private class RangeVersion
	{
		public String leftSymbol = "(";
		public String leftVersion = "";
		public String rightVersion = "";
		public String rightSymbol = ")";

		public String toString()
		{
			return (leftSymbol+leftVersion+","+rightVersion+rightSymbol);
		}
	}


	private void translateRelationalOp(RelationalOp op, RangeVersion rangeVersion)
	{
		if (op == null)
			return;
		if (op instanceof RelationalOp.GreaterThan)
		{
			rangeVersion.leftSymbol = "]";
			rangeVersion.leftVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.GreatherThanEqualsTo)
		{
			rangeVersion.leftSymbol = "[";
			rangeVersion.leftVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.LessThan)
		{
			rangeVersion.rightSymbol = "[";
			rangeVersion.rightVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.LessThanEqualsTo)
		{
			rangeVersion.rightSymbol = "]";
			rangeVersion.rightVersion = op.getSemver().toString();
		}
	}


	private String translateVersion(String version)
	{
		Semver semver = Semver.create(version)

		if (semver instanceof com.github.jknack.semver.Version)
		{
			return "${semver}"
		}
		else if (semver instanceof RelationalOp)
		{
			def rangeVersion = new RangeVersion()
			translateRelationalOp(semver, rangeVersion)
			return rangeVersion.toString()
		}
		else if (semver instanceof AndExpression)
		{
			def rangeVersion = new RangeVersion()
			translateRelationalOp(semver.getLeft(), rangeVersion)
			translateRelationalOp(semver.getRight(), rangeVersion)
			return rangeVersion.toString()
		}

		""
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

		for (Metadata.Dependency dependency : metadata.getDependencies())
		{
			String[] depNameSplit = dependency.getName().split("/");
			Dependency pomDep = new Dependency();
			pomDep.setGroupId(depNameSplit[0]);
			pomDep.setArtifactId(depNameSplit[1]);
			pomDep.setVersion(dependency.getVersion_requirement());

			model.addDependency(pomDep);
		}

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
		 Repository name must be configured in puppet-forge.properties and it must be a Maven
		 repository.
		 */
		if (m_puppetRepositories.contains(repositoryName) &&
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
