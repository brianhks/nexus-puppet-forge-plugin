package org.sonatype.nexus.puppetforge.rest;

import org.apache.commons.io.IOUtils;
import org.sonatype.nexus.proxy.*;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
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
import java.io.File;
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

	private RepositoryRegistry defaultRepositoryRegistry;

	@Inject
	public void setDefaultRepositoryRegistry(final @Named("default") RepositoryRegistry repositoryRegistry) {
		this.defaultRepositoryRegistry = checkNotNull(repositoryRegistry);
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
			@PathParam("repo") String repo) throws ItemNotFoundException, IllegalOperationException, NoSuchRepositoryException, IOException, AccessDeniedException
	{

		StorageFileItem metadata = getMetadata(groupId, artifactId, version, repo);

		StringWriter sw = new StringWriter();

		IOUtils.copy(metadata.getInputStream(), sw, "UTF-8");

		StringBuilder sb = new StringBuilder();

		sb.append("{\"file_uri\":\"/nexus/service/siesta/puppetforge/").append(repo).append("/v3/files/")
				.append(groupId).append("-")
				.append(artifactId).append("-").append(version).append(".tar.gz\"");

		sb.append(",\"metadata\": ")
				.append(sw.toString());
		sb.append(",\"deleted_at\": null");
		sb.append("}");

		Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(
				sb.toString());

		return responseBuilder.build();
	}

	private StorageFileItem getMetadata(String groupId, String artifactId, String version, String repo) throws NoSuchRepositoryException, ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException
	{
		Repository repository = defaultRepositoryRegistry.getRepository(repo);

		MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

		Gav gav = new Gav(groupId, artifactId, version, null, "json", null, null, null, false, null, false, null);

		ArtifactStoreRequest storeRequest = new ArtifactStoreRequest(mavenRepository,
				gav, true, false);

		StorageFileItem storageFileItem = mavenRepository.getArtifactStoreHelper().retrieveArtifact(storeRequest);

		return (storageFileItem);

	}
}
