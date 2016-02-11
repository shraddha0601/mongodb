

package course;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

public class BlogPostDataService {
    private final MongoCollection<Document> postsCollection;

    public BlogPostDataService(final MongoDatabase blogDatabase) {
        postsCollection = blogDatabase.getCollection("posts");
    }

    public Document findByPermalink(String permalink) {
        Document post = postsCollection.find(eq("permalink", permalink)).first();

        // fix up if a post has no likes
        if (post != null) {
            List<Document> comments = (List<Document>) post.get("comments");
            for (Document comment : comments) {
                if (!comment.containsKey("num_likes")) {
                    comment.put("num_likes", 0);
                }
            }
        }
        return post;
    }

    public List<Document> findByDateDescending(int limit) {
        return postsCollection.find().sort(descending("date"))
                              .limit(limit)
                              .into(new ArrayList<Document>());
    }

    public List<Document> findByTagDateDescending(final String tag) {
        return postsCollection.find(eq("tags", tag))
                              .sort(descending("date"))
                              .limit(10)
                              .into(new ArrayList<Document>());
    }

    public String addPost(String title, String body, List tags, String username) {
        String permalink = title.replaceAll("\\s", "_"); // whitespace becomes _
        permalink = permalink.replaceAll("\\W", ""); // get rid of non alphanumeric
        permalink = permalink.toLowerCase();

        Document post = new Document("title", title)
                        .append("author", username)
                        .append("body", body)
                        .append("permalink", permalink)
                        .append("tags", tags)
                        .append("comments", new ArrayList())
                        .append("date", new Date());

        postsCollection.insertOne(post);

        return permalink;
    }

    public void addPostComment(final String name, final String email, final String body, final String permalink) {
        Document comment = new Document("author", name)
                           .append("body", body);

        if (email != null && !email.isEmpty()) {
            comment.append("email", email);
        }

        postsCollection.updateOne(eq("permalink", permalink),
                                  new Document("$push", new Document("comments", comment)));
    }

    public void likePost(final String permalink, final int ordinal) {
        Document post = postsCollection.find(eq("permalink", permalink)).first();

        if (post != null) {
            List<Document> comments = (List<Document>) post.get("comments");
            Document comment = comments.get(ordinal);
            Integer likes = 1;
            if (comment.containsKey("num_likes")) {
                likes = (Integer) comment.get("num_likes") + 1;
            }
            comment.put("num_likes", likes);
            String commentIndex = "comments." + ordinal;
            postsCollection.updateOne(eq("permalink", permalink),
                    new Document("$set", new Document(commentIndex, comment)));

        }
    }
}
