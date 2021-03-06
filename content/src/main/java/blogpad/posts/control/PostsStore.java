
package blogpad.posts.control;

import blogpad.posts.entity.Post;
import blogpad.storage.control.FileNames;
import blogpad.storage.control.FileOperations;
import static blogpad.storage.control.FileOperations.initializeStorageFolder;
import static blogpad.storage.control.FileOperations.read;
import static blogpad.storage.control.FileOperations.write;
import blogpad.storage.control.StorageException;
import blogpad.tracing.boundary.Tracer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author airhacks.com
 */
public class PostsStore {

    @Inject
    @ConfigProperty(name = "storage.dir")
    String rootStorageDir;

    @Inject
    @ConfigProperty(name = "posts.subdir", defaultValue = "posts")
    String postsSubFolder;

    private Jsonb jsonb;

    Path postsDirectory;

    @PostConstruct
    public void init() {
        Path storageFolder = Path.of(this.rootStorageDir);
        this.postsDirectory = storageFolder.resolve(this.postsSubFolder);

        initializeStorageFolder(this.postsDirectory);

        JsonbConfig config = new JsonbConfig().
                withFormatting(true);
        this.jsonb = JsonbBuilder.newBuilder().
                withConfig(config).
                build();
    }

    public String saveOrUpdate(Post newOrUpdated) {
        String title = newOrUpdated.title;
        Post updatedPost = this.getPost(title).
                map(existing -> Post.copy(newOrUpdated, existing)).
                orElseGet(() -> newOrUpdated.setCreationDate());

        String content = serialize(updatedPost);
        String fileName = FileNames.encode(title);
        write(this.postsDirectory, fileName, content);
        return fileName;
    }



    String serialize(Post post) {
        return this.jsonb.toJson(post);
    }

    Post deserialize(String content) {
        return this.jsonb.fromJson(content, Post.class);
    }

    String readFromStorageFolder(Path fileName) {
        Path path = this.postsDirectory.resolve(fileName);
        return read(path);
    }

    boolean postExists(Path title) {
        Path fileInStore = this.postsDirectory.resolve(title);
        return Files.exists(fileInStore);
    }

    public List<Post> allPosts() {
        return this.recentPosts(Integer.MAX_VALUE);
    }

    public List<Post> recentPosts(int max) {
        try {
            return Files.list(this.postsDirectory).
                    map(FileOperations::read).
                    map(this::deserialize).
                    sorted(this::newestFirst).
                    limit(max).
                    collect(Collectors.toList());
        } catch (IOException ex) {
            throw new StorageException("Cannot list contents of: " + this.postsDirectory + " reason: " + ex.getMessage());
        }
    }

    int newestFirst(Post first, Post next) {
        return next.createdAt.compareTo(first.createdAt);
    }

    public Optional<Post> getPost(String title) {
        Path fileName = Path.of(title);
        return this.getPost(fileName);
    }

    public Optional<Post> getPost(Path title) {
        Tracer.info("Fetching: " + title);
        if (!postExists(title)) {
            Tracer.info("Post does not exist " + title);
            return Optional.empty();
        }
        String content = this.readFromStorageFolder(title);
        Tracer.info("Post found: " + content);
        return Optional.of(deserialize(content));
    }

}
