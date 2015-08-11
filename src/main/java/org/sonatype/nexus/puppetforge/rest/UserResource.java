package org.sonatype.nexus.puppetforge.rest;

import org.sonatype.nexus.puppetforge.PuppetForgePlugin;
import org.sonatype.sisu.siesta.common.Resource;
import org.sonatype.sisu.siesta.server.ApplicationSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 Created by bhawkins on 8/10/15.
 */
@Named
@Singleton
@Path(PuppetForgePlugin.URI_PREFIX)
public class UserResource extends ApplicationSupport
		implements Resource
{


	@GET
	@Path("/users")
	@Produces("application/json")
	public Response getUsers()
	{
		String users = "{\n" +
				"  \"pagination\": {\n" +
				"    \"limit\": 100,\n" +
				"    \"offset\": 0,\n" +
				"    \"first\": \"/v3/users?limit=100&offset=0\",\n" +
				"    \"previous\": null,\n" +
				"    \"current\": \"/v3/users?limit=100&offset=0\",\n" +
				"    \"next\": null,\n" +
				"    \"total\": 1\n" +
				"  },\n" +
				"  \"results\": [\n" +
				"    {\n" +
				"      \"uri\": \"/v3/users/001john\",\n" +
				"      \"slug\": \"001john\",\n" +
				"      \"gravatar_id\": \"4dbab6720a744e44beb0999266bfe6a2\",\n" +
				"      \"username\": \"001john\",\n" +
				"      \"display_name\": \"001john\",\n" +
				"      \"release_count\": 0,\n" +
				"      \"module_count\": 0,\n" +
				"      \"created_at\": \"2014-11-06 06:33:31 -0800\",\n" +
				"      \"updated_at\": \"2014-11-06 06:33:53 -0800\"\n" +
				"    }" +
				"]" +
				"}";

		Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK).entity(
				users);

		return responseBuilder.build();
	}

}
