package dto;

import entityClasses.Post; // Import the base Post entity
import java.sql.Timestamp; // Import Timestamp for delegated getter

/**
 * <p> Title: PostInfo Class (Data Transfer Object) </p>
 * <p> Description: A DTO used to transfer post data along with calculated information
 * (reply counts, read status) from the Database layer to the Controller/View layers.
 * It encapsulates a base Post object and adds fields necessary for the UI display based
 * on the expanded HW2 requirements (User Stories #5, #8, #9). </p>
 * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * @author Amaan Sayed
 * @version 1.00 HW2 Expanded Scope Implementation
 */
public class PostInfo {

    /* Attributes ********************************************************************************/
    private Post post; // The underlying Post entity object containing core post data.
    private int replyCount; // Total number of replies associated with this post. (User Story #9)
    private int unreadReplyCount; // Number of replies *this user* hasn't read yet. (User Stories #5, #9)
    private boolean isRead; // Has *this user* read this specific post? (User Story #8)

    /* Constructors ******************************************************************************/
    /**
     * <p> Method: PostInfo() - Constructor </p>
     * <p> Description: Initializes the DTO with the base Post object. Calculated fields
     * (replyCount, unreadReplyCount, isRead) must be set separately using setters,
     * typically after being calculated by the Database layer. </p>
     * @param post The core Post entity object. Cannot be null.
     * @throws IllegalArgumentException if the provided Post object is null.
     */
    public PostInfo(Post post) {
        if (post == null) {
            // Why this check? Prevents NullPointerException if invalid data is passed from DB layer.
            throw new IllegalArgumentException("Post object cannot be null when creating PostInfo DTO.");
        }
        this.post = post;
        // Initialize calculated fields to default values (0 or false).
        this.replyCount = 0;
        this.unreadReplyCount = 0;
        this.isRead = false;
    }

    /* Accessors and Mutators (Getters and Setters) ********************************************/

    /**
     * <p> Method: getPost() </p>
     * <p> Description: Returns the underlying Post entity object, allowing access to its original attributes. </p>
     * @return The encapsulated Post object.
     */
    public Post getPost() {
        return post;
    }

    // --- Delegated Getters ---
    // Why delegate? Provides convenient access to core Post attributes directly from the PostInfo object.
    // This simplifies usage in the View/Controller and allows PropertyValueFactory in TableView
    // to bind columns directly to these methods (e.g., binding column "Title" to "title" property via getTitle).
    
    /** Gets the post ID from the underlying Post object. */
    public int getPostID() { return post.getPostID(); }
    /** Gets the title from the underlying Post object. */
    public String getTitle() { return post.getTitle(); }
    /** Gets the author's username from the underlying Post object. */
    public String getAuthorUsername() { return post.getAuthorUsername(); }
    /** Gets the content from the underlying Post object. */
    public String getContent() { return post.getContent(); }
    /** Gets the timestamp from the underlying Post object. */
    public Timestamp getTimestamp() { return post.getTimestamp(); }
    /** Gets the thread name from the underlying Post object. */
    public String getThreadName() { return post.getThreadName(); }

    // --- Getters and Setters for Calculated Fields ---

    /**
     * <p> Method: getReplyCount() </p>
     * <p> Description: Gets the total number of replies associated with this post (User Story #9). </p>
     * @return The total reply count.
     */
    public int getReplyCount() {
        return replyCount;
    }

    /**
     * <p> Method: setReplyCount() </p>
     * <p> Description: Sets the total number of replies. Called by the Database layer after calculation. </p>
     * @param replyCount The total reply count.
     */
    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    /**
     * <p> Method: getUnreadReplyCount() </p>
     * <p> Description: Gets the number of replies to this post that the current viewing user has not read (User Stories #5, #9). </p>
     * @return The unread reply count for the specific viewing user.
     */
    public int getUnreadReplyCount() {
        return unreadReplyCount;
    }

    /**
     * <p> Method: setUnreadReplyCount() </p>
     * <p> Description: Sets the count of unread replies for the viewing user. Called by the Database layer. </p>
     * @param unreadReplyCount The unread reply count calculated for the specific viewing user.
     */
    public void setUnreadReplyCount(int unreadReplyCount) {
        this.unreadReplyCount = unreadReplyCount;
    }

    /**
     * <p> Method: isRead() </p>
     * <p> Description: Checks if the current viewing user has marked this specific post as read (User Story #8). </p>
     * @return true if the post is marked as read by the viewing user, false otherwise.
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * <p> Method: setRead() </p>
     * <p> Description: Sets the read status of this post for the current viewing user. Called by the Database layer. </p>
     * @param isRead true if the post is read by the viewing user, false otherwise.
     */
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}