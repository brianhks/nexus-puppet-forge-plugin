package org.sonatype.nexus.puppetforge.rest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MetadataLocator;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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
	private RepositoryRegistry m_repositoryRegistry;
	private MetadataLocator m_metadataLocator;
	private SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

	@Inject
	public ModuleResource(RepositoryRegistry repositoryRegistry,
			MetadataLocator metadataLocator)
	{
		//log.info("++++++++++HERE+++++++++++++++++");
		m_repositoryRegistry = repositoryRegistry;
		m_metadataLocator = metadataLocator;
	}
	/**
	 
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
			Repository repository = m_repositoryRegistry.getRepository(repo);
			if (!repository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class))
			{
				throw new RestException(Response.Status.BAD_REQUEST, "Repository '"+repo+"' is not a maven repository.");
			}

			MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

			String split[] = moduleName.split("-");
			if (split.length != 2)
				throw new RestException(Response.Status.BAD_REQUEST, "'"+moduleName+"' is a bad slug.");

			String groupId = split[0];
			String artifactId = split[1];

			Gav gav = new Gav(groupId, artifactId, "1.0", null, "tar.gz", 	null,
					null, null, false, null, false, null);

			ArtifactStoreRequest request = new ArtifactStoreRequest(
					mavenRepository, gav, true);
			Metadata metadata = m_metadataLocator.retrieveGAMetadata(request);

			if ((metadata == null)||(metadata.getVersioning() == null))
				throw new RestException(Response.Status.NOT_FOUND, "Unable to find module '"+moduleName+"'");

			JSONObject response = new JSONObject();
			JSONObject user = new JSONObject();
			String baseUri = "/nexus/service/siesta/puppetforge/"+repo;

			//StringBuilder sb = new StringBuilder();


			user.put("username", groupId);
			user.put("slug", groupId);
			user.put("uri", baseUri+"/v3/users/"+groupId);

			response.put("uri", baseUri+"/v3/modules/"+moduleName);
			response.put("name", moduleName);
			response.put("version", metadata.getVersioning().getLatest());
			response.put("slug", moduleName);
			response.put("owner", user);

			/*sb.append("{\"uri\": \"/nexus/service/siesta/puppetforge/").append(repo)
					.append("/v3/modules/")
					.append(moduleName)

					.append("\",\"name\":\"")
					.append(moduleName)
					.append("\",\"version\":\"")
					.append(metadata.getVersioning().getLatest())
					.append("\",\"slug\":\"")
					.append(moduleName)
					.append("\",\"owner\":{\"username\":\"")
					.append(groupId)
					.append("\",\"slug\":\"")
					.append(groupId)
					.append("\",\"uri\": \"/nexus/service/siesta/puppetforge/").append(repo)
					.append("/v3/users/")
					.append(groupId)
					.append("\"}")
					.append(",\"releases\":[");*/

			JSONArray releases = new JSONArray();
			boolean first = true;
			for (String version : metadata.getVersioning().getVersions())
			{
				Gav releaseGav = new Gav(groupId, artifactId, version, null, "tar.gz", null, null, null, false, null, false, null);
				ArtifactStoreRequest storeRequest = new ArtifactStoreRequest(mavenRepository,
						releaseGav, true, false);

				StorageFileItem storageFileItem = mavenRepository.getArtifactStoreHelper().retrieveArtifact(storeRequest);
				Date createdDate = new Date(storageFileItem.getRepositoryItemAttributes().getCreated());


				JSONObject release = new JSONObject();
				release.put("uri", baseUri+"/v3/releases/"+moduleName+"-"+version);
				release.put("file_uri", baseUri+"/v3/files"+moduleName+"-"+version+".tar.gz");
				release.put("file_size", storageFileItem.getLength());
				release.put("created_at", m_dateFormat.format(createdDate));
				release.put("slug", moduleName+"-"+version);
				release.put("deleted_at", JSONObject.NULL);
				release.put("version", version);
				release.put("supported", false);

				releases.put(release);
			}

			response.put("releases", releases);



			Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
					.entity(response.toString());

			return responseBuilder.build();
		}
		catch (RestException re)
		{
			StringBuilder sb = new StringBuilder();

			sb.append("{\"errors\":[\"");
			sb.append(re.getMessage());
			sb.append("\"]}");

			return Response.status(re.getStatus()).entity(sb.toString()).build();
		}
		catch (Exception e)
		{
			log.error("Error getting modules", e);
			StringBuilder sb = new StringBuilder();

			sb.append("{\"errors\":[\"");
			sb.append(e.getMessage());
			sb.append("\"]}");

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(sb.toString()).build();
		}

	}

}
