package org.sonatype.nexus.puppetforge.rest;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.*;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.puppetforge.Configuration;
import org.sonatype.nexus.puppetforge.PuppetForgePlugin;
import org.sonatype.sisu.siesta.common.Resource;
import org.sonatype.sisu.siesta.server.ApplicationSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/8/15.

 Example url: http://localhost:8081/nexus/service/siesta/puppetforge/{repo}/v3/releases/puppetlabs-mysql-3.4.0

 Download Path
 http://vm-server:8081/nexus/service/local/repositories/puppet/content/puppetlabs/mysql/3.4.0/mysql-3.4.0.tar.gz
 */
@Named
@Singleton
@Path(PuppetForgePlugin.URI_PREFIX+"/releases")
public class ReleaseResource extends ApplicationSupport
	implements Resource
{

	private RepositoryRegistry m_repositoryRegistry;
	private final Configuration m_configuration;

	@Inject
	public ReleaseResource(NexusConfiguration nexusConfiguration) throws IOException
	{
		super();

		m_configuration = Configuration.getConfigueration(nexusConfiguration);
	}

	@Inject
	public void setDefaultRepositoryRegistry(final @Named("default") RepositoryRegistry repositoryRegistry) {
		this.m_repositoryRegistry = checkNotNull(repositoryRegistry);
	}

	/**
	 Needs to return something like
	 {
	 "file_uri": "/path/to/file.tar.gz"
	 }

	 Looks like path normally starts with /v3 so we may have to go up (..) to
	 get to the maven paths
	 @param groupId
	 @param artifactId
	 @param version
	 @return
	 */
	@GET
	@Path("{groupId}-{artifactId}-{version}")
	@Produces("application/json")
	public Response getReleaseInfo(@PathParam("groupId") String groupId,
			@PathParam("artifactId") String artifactId,
			@PathParam("version") String version,
			@PathParam("repo") String repo)
	{
		try
		{
			StorageFileItem metadata = getMetadata(groupId, artifactId, version, repo);

			StringWriter sw = new StringWriter();

			IOUtils.copy(metadata.getInputStream(), sw, "UTF-8");

			String baseUri = "";
			if (m_configuration.useReleaseFullUri())
				baseUri = "/nexus/service/siesta/puppetforge/"+repo;

			JSONObject response = new JSONObject();

			response.put("file_uri", baseUri+"/v3/files/" + groupId + "-" + artifactId + "-" + version + ".tar.gz");
			response.put("slug", groupId+"-"+artifactId+"-"+version);

			response.put("module", new JSONObject()
					.put("uri", baseUri+"/v3/modules/"+groupId+"-"+artifactId)
					.put("slug", groupId+"-"+artifactId)
					.put("name", artifactId)
					.put("owner", new JSONObject()
									.put("uri", baseUri+"/v3/users/" + groupId)
									.put("slug", groupId)
									.put("username", groupId)
					));

			response.put("metadata", new JSONObject(sw.toString()));
			response.put("deleted_at", JSONObject.NULL);

			Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(
					response.toString());

			return responseBuilder.build();
		}
		catch (Exception e)
		{
			log.error("Error getting releases", e);
			StringBuilder sb = new StringBuilder();

			sb.append("{\"errors\":[\"");
			sb.append(e.getMessage().replace('\"', '\''));
			sb.append("\"]}");

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(sb.toString()).build();
		}
	}

	private StorageFileItem getMetadata(String groupId, String artifactId, String version, String repo) throws NoSuchRepositoryException, ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException
	{
		Repository repository = m_repositoryRegistry.getRepository(repo);

		MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

		Gav gav = new Gav(groupId, artifactId, version, null, "json", null, null, null, false, null, false, null);

		ArtifactStoreRequest storeRequest = new ArtifactStoreRequest(mavenRepository,
				gav, true, false);

		StorageFileItem storageFileItem = mavenRepository.getArtifactStoreHelper().retrieveArtifact(storeRequest);

		return (storageFileItem);

	}
}
