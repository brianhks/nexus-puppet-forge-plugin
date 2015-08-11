package org.sonatype.nexus.puppetforge.rest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.sonatype.nexus.puppetforge.PuppetForgePlugin;
import org.sonatype.sisu.siesta.common.Resource;
import org.sonatype.sisu.siesta.server.ApplicationSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 Created by bhawkins on 8/8/15.

 Example url: http://localhost:8081/nexus/service/siesta/puppetforge/{repo}/v3/modules/puppetlabs-mysql
 */
@Named
@Singleton
@Path(PuppetForgePlugin.URI_PREFIX+"/modules")
public class ModuleResource extends ApplicationSupport
	implements Resource
{
	private Multimap<String, String> m_artifactMap;

	public ModuleResource()
	{
		//log.debug("++++++++++HERE+++++++++++++++++");
		m_artifactMap = TreeMultimap.create();

		m_artifactMap.put("puppetlabs-mysql", "3.4.0");
		m_artifactMap.put("nanliu-staging", "1.0.3");
		m_artifactMap.put("puppetlabs-stdlib", "4.6.0");
	}
	/**
	 Needs to return something like
	 {
	   "releases": [
	 {
	   "version": "2.3.4"
	 },
	 {
	   "version": 2.3.4"
	 }
	 ]
	 }
	 //@param groupId
	 //@param artifactId
	 @return
	 */
	@GET
	@Path("/{moduleName}")
	@Produces("application/json")
	public Response getModuleInfo(@PathParam("moduleName") String moduleName,
			@PathParam("repo") String repo)
	{
		try
		{
			Collection<String> versions = m_artifactMap.get(moduleName);

			Release release = new Release();

			for (String version : versions)
			{
				release.addVersion(new Version(version));
			}

			//Hack
			StringBuilder sb = new StringBuilder();

			sb.append("{\"uri\": \"/nexus/service/siesta/puppetforge/").append(repo)
					.append("/v3/modules/")
					.append(moduleName)
					.append("\",\"name\":\"")
					.append("module_name")
					.append("\",\"version\":\"")
					.append("10.0.0") //todo: fix this
					.append("\",\"slug\":\"")
					.append(moduleName)
					.append("\",\"owner\":{\"username\":\"puppetlabs\"}")
					.append(",\"releases\":[");

			boolean first = true;
			for (String version : versions)
			{
				if (!first)
					sb.append(",");

				sb.append("{\"uri\":\"/nexus/service/siesta/puppetforge/").append(repo)
						.append("/v3/releases/")
						.append(moduleName).append("-").append(version)
						.append("\",\"version\": \"").append(version).append("\"}");
				first = false;
			}

			sb.append("]}");

			Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
					.entity(sb.toString());

			return responseBuilder.build();
		}
		catch (Exception e)
		{
			log.debug("Error getting modules", e);
			return Response.status(501).build();
		}

	}

}
