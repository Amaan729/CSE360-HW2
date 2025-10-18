package tests; // Or your preferred package for tests

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.SQLException;
import java.util.List;
import database.Database;
import entityClasses.Post;
import entityClasses.Reply;
// No User needed for these specific tests, but keep import if other tests might use it

/**
 * <p> Title: DiscussionBoardTests Class </p>
 * * <p> Description: A JUnit test class for verifying the CRUD operations, search functionality,
 * thread handling, and delete behavior for Posts and Replies in the Database.java file,
 * based on HW2 requirements. </p>
 * * @author Amaan Sayed
 * * @version 2.10 HW2 Implementation with updated tests
 */
public class DiscussionBoardTests {

    /* Attributes ********************************************************************************/
    private Database database; // Instance of the Database class to test against.

    /* Test Setup and Teardown *******************************************************************/
    
    /**
     * <p> Method: setUp() </p>
     * <p> Description: This method runs before each @Test method. It establishes a fresh
     * connection to the database. It also clears the posts and replies tables to ensure
     * that tests do not interfere with each other, providing a clean state for each test. </p>
     */
    @Before
    public void setUp() {
        database = new Database();
        try {
            database.connectToDatabase();
            // Why clear tables? Ensures test independence. One test's data won't affect another's outcome.
            clearDiscussionTables(); 
        } catch (SQLException e) {
            fail("Database setup failed: Failed to connect or clear tables. " + e.getMessage());
        }
    }

    /**
     * <p> Method: tearDown() </p>
     * <p> Description: This method runs after each @Test method. It ensures the database
     * connection is closed, releasing resources. </p>
     */
    @After
    public void tearDown() {
        if (database != null) {
            database.closeConnection();
        }
    }
    
    /**
     * <p> Method: clearDiscussionTables() - Private Helper </p>
     * <p> Description: Helper method to delete all records from the replies and posts tables.
     * Called by setUp() to ensure a clean slate for each test. Needs to delete replies first
     * due to potential (though removed) foreign key constraints. </p>
     */
     private void clearDiscussionTables() throws SQLException {
         // Why delete replies first? Good practice even without cascade, avoids potential future FK issues.
         String clearReplies = "DELETE FROM replies";
         String clearPosts = "DELETE FROM posts";
         // Use the existing statement object from the connected database instance.
         try (Statement stmt = database.connection.createStatement()) { // Assuming connection is accessible or provide getter
             stmt.executeUpdate(clearReplies);
             stmt.executeUpdate(clearPosts);
             System.out.println("Cleared posts and replies tables for test."); // Log action
         } catch (NullPointerException npe) {
             fail("Database connection not established in setUp before clearing tables.");
         }
     }


    /* Test Methods ******************************************************************************/

    /**
     * <p> Test Case: Create and Read Post with Thread </p>
     * <p> Description: Verifies post creation including the 'threadName' attribute,
     * checks the default thread, and confirms retrieval via getAllPosts. Corresponds to
     * User Stories for creating and reading posts. </p>
     */
    @Test
    public void testCreateAndReadPostWithThread() {
        // 1. Create a Post with a specific thread
        Post post1 = new Post();
        post1.setTitle("Post in HW1 Thread");
        post1.setAuthorUsername("studentA");
        post1.setContent("Content for HW1 thread.");
        post1.setThreadName("Homework 1"); // Explicitly set thread
        database.createPost(post1);

        // 2. Create a Post using the default thread
        Post post2 = new Post();
        post2.setTitle("Post in General Thread");
        post2.setAuthorUsername("studentB");
        post2.setContent("Content for default General thread.");
        // No setThreadName call, should default to "General"
        database.createPost(post2);
        
        // 3. Retrieve all posts (should be ordered newest first)
        List<Post> allPosts = database.getAllPosts();
        
        // 4. Verify
        assertNotNull("Post list should not be null.", allPosts);
        assertEquals("Should retrieve 2 posts.", 2, allPosts.size());
        
        // Check post2 (most recent, should be first in list)
        Post retrievedPost2 = allPosts.get(0);
        assertEquals("Post 2 Title mismatch.", "Post in General Thread", retrievedPost2.getTitle());
        assertEquals("Post 2 Author mismatch.", "studentB", retrievedPost2.getAuthorUsername());
        assertEquals("Post 2 Thread mismatch (should be default).", "General", retrievedPost2.getThreadName());

        // Check post1 (older, should be second in list)
        Post retrievedPost1 = allPosts.get(1);
        assertEquals("Post 1 Title mismatch.", "Post in HW1 Thread", retrievedPost1.getTitle());
        assertEquals("Post 1 Author mismatch.", "studentA", retrievedPost1.getAuthorUsername());
        assertEquals("Post 1 Thread mismatch.", "Homework 1", retrievedPost1.getThreadName());
    }

    /**
     * <p> Test Case: Update Post </p>
     * <p> Description: Verifies that a post's title and content can be updated.
     * Note: Thread name is generally not updated post-creation per typical forum logic.
     * Corresponds to the Update Post user story. </p>
     */
    @Test
    public void testUpdatePost() {
        // 1. Setup: Create a post to update
        Post initialPost = new Post();
        initialPost.setTitle("Initial Title");
        initialPost.setAuthorUsername("updater");
        initialPost.setContent("Initial content.");
        initialPost.setThreadName("General");
        database.createPost(initialPost);
        
        // 2. Retrieve the created post to get its ID
        List<Post> posts = database.getAllPosts();
        assertFalse("Setup failed: No post found to update.", posts.isEmpty());
        Post postToUpdate = posts.get(0); // Get the post we just created
        int postId = postToUpdate.getPostID(); // Store its ID

        // 3. Modify the object and call update
        postToUpdate.setTitle("Updated Title");
        postToUpdate.setContent("Updated content.");
        database.updatePost(postToUpdate);

        // 4. Retrieve the post again using its ID and verify changes
        Post updatedPost = database.getPost(postId); // Use getPost(id) for specific retrieval
        assertNotNull("Updated post should exist.", updatedPost);
        assertEquals("Title should be updated.", "Updated Title", updatedPost.getTitle());
        assertEquals("Content should be updated.", "Updated content.", updatedPost.getContent());
        assertEquals("Author should remain unchanged.", "updater", updatedPost.getAuthorUsername());
        assertEquals("Thread should remain unchanged.", "General", updatedPost.getThreadName());
    }

    /**
     * <p> Test Case: Create and Read Reply </p>
     * <p> Description: Verifies that a reply can be added to a post and then retrieved
     * using getRepliesForPost. Corresponds to Create and Read Reply user stories. </p>
     */
    @Test
    public void testCreateAndReadReply() {
        // 1. Setup: Create a parent post
        Post parentPost = new Post();
        parentPost.setTitle("Parent Post");
        parentPost.setAuthorUsername("poster");
        parentPost.setContent("Parent content.");
        database.createPost(parentPost);
        int parentPostId = database.getAllPosts().get(0).getPostID(); // Get the ID

        // 2. Create a Reply linked to the parent post
        Reply newReply = new Reply();
        newReply.setPostID(parentPostId);
        newReply.setAuthorUsername("replier");
        newReply.setContent("This is the reply content.");
        database.createReply(newReply);

        // 3. Retrieve replies for the parent post
        List<Reply> replies = database.getRepliesForPost(parentPostId);

        // 4. Verify
        assertNotNull("Replies list should not be null.", replies);
        assertEquals("Should retrieve 1 reply.", 1, replies.size());
        Reply retrievedReply = replies.get(0);
        assertEquals("Reply content mismatch.", "This is the reply content.", retrievedReply.getContent());
        assertEquals("Reply author mismatch.", "replier", retrievedReply.getAuthorUsername());
        assertEquals("Reply postID mismatch.", parentPostId, retrievedReply.getPostID());
    }

    /**
     * <p> Test Case: Delete Post - Verify Replies Remain </p>
     * <p> Description: Verifies that deleting a post does NOT delete its associated replies,
     * as required by the updated user story. Corresponds to the Delete Post user story's
     * specific behavior regarding replies. </p>
     */
    @Test
    public void testDeletePostRepliesRemain() {
        // 1. Setup: Create a post and add a reply to it
        Post parentPost = new Post();
        parentPost.setTitle("Post To Be Deleted");
        parentPost.setAuthorUsername("doomedAuthor");
        parentPost.setContent("Some content.");
        database.createPost(parentPost);
        int parentPostId = database.getAllPosts().get(0).getPostID();

        Reply reply = new Reply();
        reply.setPostID(parentPostId);
        reply.setAuthorUsername("replyAuthor");
        reply.setContent("Reply that should survive.");
        database.createReply(reply);
        
        // Sanity check: verify reply exists before deleting post
        List<Reply> repliesBeforeDelete = database.getRepliesForPost(parentPostId);
        assertEquals("Setup failed: Reply should exist before post deletion.", 1, repliesBeforeDelete.size());
        int replyId = repliesBeforeDelete.get(0).getReplyID(); // Get reply ID for later check

        // 2. Delete the parent post
        database.deletePost(parentPostId);

        // 3. Verify the post is gone
        Post deletedPost = database.getPost(parentPostId);
        assertNull("Post should be null after deletion.", deletedPost);

        // 4. Verify the reply STILL exists (by attempting to retrieve it directly,
        // or checking if getRepliesForPost still finds it - checking count is easier here)
        List<Reply> repliesAfterDelete = database.getRepliesForPost(parentPostId);
        // THIS IS THE KEY CHECK FOR THE NEW REQUIREMENT:
        assertNotNull("Replies list should not be null even after post deletion.", repliesAfterDelete);
        assertEquals("Reply should still exist after parent post deletion.", 1, repliesAfterDelete.size());
        assertEquals("The remaining reply should have the correct ID.", replyId, repliesAfterDelete.get(0).getReplyID());
        assertEquals("The remaining reply content should be correct.", "Reply that should survive.", repliesAfterDelete.get(0).getContent());
    }
    
     /**
     * <p> Test Case: Delete Reply </p>
     * <p> Description: Verifies that a specific reply can be deleted without affecting
     * its parent post or other replies. Corresponds to the Delete Reply user story. </p>
     */
    @Test
    public void testDeleteReply() {
        // 1. Setup: Create post and two replies
        Post parentPost = new Post();
        parentPost.setTitle("Post with Replies");
        database.createPost(parentPost);
        int parentPostId = database.getAllPosts().get(0).getPostID();

        Reply reply1 = new Reply(0, parentPostId, "user1", "Reply 1", null); // ID will be generated
        Reply reply2 = new Reply(0, parentPostId, "user2", "Reply 2", null);
        database.createReply(reply1);
        database.createReply(reply2);

        // 2. Get IDs and verify setup
        List<Reply> initialReplies = database.getRepliesForPost(parentPostId);
        assertEquals("Setup failed: Should have 2 replies.", 2, initialReplies.size());
        int replyIdToDelete = initialReplies.get(0).getReplyID(); // Get ID of the first reply
        int replyIdToKeep = initialReplies.get(1).getReplyID();   // Get ID of the second reply

        // 3. Delete the first reply
        database.deleteReply(replyIdToDelete);

        // 4. Verify the first reply is gone, but the post and the second reply remain
        List<Reply> remainingReplies = database.getRepliesForPost(parentPostId);
        assertNotNull("Remaining replies list should not be null.", remainingReplies);
        assertEquals("Should only have 1 reply remaining.", 1, remainingReplies.size());
        assertEquals("The remaining reply should be reply 2.", replyIdToKeep, remainingReplies.get(0).getReplyID());

        Post postCheck = database.getPost(parentPostId);
        assertNotNull("Parent post should still exist after deleting a reply.", postCheck);
    }
    
    /**
     * <p> Test Case: Search Posts - Keyword Match </p>
     * <p> Description: Verifies the searchPosts method finds posts containing a specific keyword
     * in either the title or content, case-insensitively, across all threads. Corresponds to the
     * Search Post user story. </p>
     */
    @Test
    public void testSearchPostsKeywordMatch() {
        // 1. Setup: Create posts with and without the keyword
        database.createPost(new Post(0, "About Java", "userA", "Java is fun.", null, "General"));
        database.createPost(new Post(0, "Python Intro", "userB", "Let's learn Python.", null, "General"));
        database.createPost(new Post(0, "Databases", "userC", "Learning about JAVA databases.", null, "Homework 1")); // Keyword in content, diff thread

        // 2. Search for "java" (lowercase) across all threads (null threadName)
        List<Post> results = database.searchPosts("java", null);

        // 3. Verify
        assertNotNull("Search results should not be null.", results);
        assertEquals("Should find 2 posts containing 'java' (case-insensitive).", 2, results.size());
        // Check titles (order might depend on timestamp, check both)
        boolean foundJavaTitle = results.stream().anyMatch(p -> p.getTitle().equals("About Java"));
        boolean foundDbTitle = results.stream().anyMatch(p -> p.getTitle().equals("Databases"));
        assertTrue("Should find the post titled 'About Java'.", foundJavaTitle);
        assertTrue("Should find the post titled 'Databases'.", foundDbTitle);
    }

    /**
     * <p> Test Case: Search Posts - Keyword Match within Specific Thread </p>
     * <p> Description: Verifies searchPosts filters results correctly when a specific thread name
     * is provided along with the keyword. </p>
     */
    @Test
    public void testSearchPostsKeywordInThread() {
        // 1. Setup: Create posts with the keyword in different threads
        database.createPost(new Post(0, "JavaFX Issue", "userA", "Help with JavaFX.", null, "Homework 1"));
        database.createPost(new Post(0, "General Java Question", "userB", "How does Java GC work?", null, "General"));
        database.createPost(new Post(0, "Python Topic", "userC", "Python lists.", null, "General"));

        // 2. Search for "java" specifically within the "General" thread
        List<Post> results = database.searchPosts("java", "General");

        // 3. Verify
        assertNotNull("Search results should not be null.", results);
        assertEquals("Should find only 1 post containing 'java' in the 'General' thread.", 1, results.size());
        assertEquals("The found post should be 'General Java Question'.", "General Java Question", results.get(0).getTitle());
    }

    /**
     * <p> Test Case: Search Posts - No Match </p>
     * <p> Description: Verifies searchPosts returns an empty list when the keyword does not match
     * any post titles or content. </p>
     */
    @Test
    public void testSearchPostsNoMatch() {
        // 1. Setup: Create some posts
        database.createPost(new Post(0, "Post One", "userA", "Content A", null, "General"));
        database.createPost(new Post(0, "Post Two", "userB", "Content B", null, "Homework 1"));

        // 2. Search for a keyword that doesn't exist
        List<Post> results = database.searchPosts("nonexistent", null);

        // 3. Verify
        assertNotNull("Search results list should not be null, even if empty.", results);
        assertTrue("Search results list should be empty for non-matching keyword.", results.isEmpty());
    }

}