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
package org.sonatype.nexus.puppetforge.rest;

import org.apache.commons.io.IOUtils;
import org.sonatype.nexus.proxy.*;
import org.sonatype.nexus.proxy.item.StorageFileItem;
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
import java.io.IOException;
import java.io.StringWriter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/10/15.
 */
@Named
@Singleton
@Path(PuppetForgePlugin.URI_PREFIX+"/files")
public class FileResource extends ApplicationSupport
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
	@Path("{groupId}-{artifactId}-{version}.tar.gz")
	@Produces("application/binary")
	public Response getReleaseInfo(@PathParam("groupId") String groupId,
			@PathParam("artifactId") String artifactId,
			@PathParam("version") String version,
			@PathParam("repo") String repo) throws ItemNotFoundException, IllegalOperationException, NoSuchRepositoryException, IOException, AccessDeniedException
	{

		StorageFileItem tarFile = getMetadata(groupId, artifactId, version, repo);

		Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(
				new StorageFileStreamingOutput(tarFile));

		return responseBuilder.build();
	}

	private StorageFileItem getMetadata(String groupId, String artifactId, String version, String repo) throws NoSuchRepositoryException, ItemNotFoundException, IllegalOperationException, StorageException, AccessDeniedException
	{
		Repository repository = defaultRepositoryRegistry.getRepository(repo);

		MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

		Gav gav = new Gav(groupId, artifactId, version, null, "tar.gz", null, null, null, false, null, false, null);

		ArtifactStoreRequest storeRequest = new ArtifactStoreRequest(mavenRepository,
				gav, true, false);

		StorageFileItem storageFileItem = mavenRepository.getArtifactStoreHelper().retrieveArtifact(storeRequest);

		return (storageFileItem);

	}
}
