
package blogpad.installer.control;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 *
 * @author airhacks.com
 */
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "content")
public interface PostsStoreClient {

    @PUT
    @Path("posts")
    Response savePost(String post);

    @PUT
    @Path("templates/{fileName}")
    @Consumes(MediaType.TEXT_PLAIN)
    Response saveTemplate(@PathParam("fileName") String fileName, String content);
}
