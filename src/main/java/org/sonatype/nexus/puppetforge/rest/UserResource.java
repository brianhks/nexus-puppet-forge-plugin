package org.sonatype.nexus.puppetforge.rest;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.puppetforge.PuppetForgePlugin;
import org.sonatype.nexus.puppetforge.RepositoryUtils;
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
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 Created by bhawkins on 8/10/15.
 */
@Named
@Singleton
@Path(PuppetForgePlugin.URI_PREFIX)
public class UserResource extends ApplicationSupport
		implements Resource
{

	private RepositoryRegistry m_repositoryRegistry;

	@Inject
	public void setDefaultRepositoryRegistry(final @Named("default") RepositoryRegistry repositoryRegistry) {
		this.m_repositoryRegistry = checkNotNull(repositoryRegistry);
	}


	@GET
	@Path("/users")
	@Produces("application/json")
	public Response getUsers(@PathParam("repo") String repo) throws NoSuchRepositoryException, MalformedURLException, URISyntaxException
	{
		Repository repository = m_repositoryRegistry.getRepository(repo);
		File baseDir = RepositoryUtils.getBaseDir(repository);

		File[] groups = baseDir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				if (pathname.isDirectory() && !pathname.getName().startsWith("."))
					return true;
				else
					return false;
			}
		});

		StringBuilder users = new StringBuilder();
		users.append("{\n" +
						"  \"pagination\": {\n" +
						"    \"limit\": 100,\n" +
						"    \"offset\": 0,\n" +
						"    \"first\": \"/v3/users?limit=100&offset=0\",\n" +
						"    \"previous\": null,\n" +
						"    \"current\": \"/v3/users?limit=100&offset=0\",\n" +
						"    \"next\": null,\n" +
						"    \"total\": " + groups.length + "\n" +
						"  },\n" +
						"  \"results\": [\n");

		boolean first = true;
		for (File group : groups)
		{
			String groupId = group.getName();
			if (!first)
				users.append(",");

			first = false;
			users.append("    {\n" +
					"      \"uri\": \"/v3/users/" + groupId + "\",\n" +
					"      \"slug\": \"" + groupId + "\",\n" +
					"      \"gravatar_id\": \"4dbab6720a744e44beb0999266bfe6a2\",\n" +
					"      \"username\": \"" + groupId + "\",\n" +
					"      \"display_name\": \"" + groupId + "\",\n" +
					"      \"release_count\": 1,\n" +
					"      \"module_count\": 1,\n" +
					"      \"created_at\": \"2014-11-06 06:33:31 -0800\",\n" +
					"      \"updated_at\": \"2014-11-06 06:33:53 -0800\"\n" +
					"    }");
		}

		users.append("]}");

		Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(
				users.toString());

		return responseBuilder.build();
	}

}
