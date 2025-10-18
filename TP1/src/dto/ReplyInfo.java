package dto;

import entityClasses.Reply; // Import the base Reply entity
import java.sql.Timestamp; // Import Timestamp for delegated getter

/**
 * <p> Title: ReplyInfo Class (Data Transfer Object) </p>
 * <p> Description: A DTO used to transfer reply data along with calculated information
 * (specifically, the read status for the current user) from the Database layer to the
 * Controller/View layers. It encapsulates a base Reply object. Used for features related
 * to User Stories #6 and #9. </p>
 * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * @author Amaan Sayed
 * @version 1.00 HW2 Expanded Scope Implementation
 */
public class ReplyInfo {

    /* Attributes ********************************************************************************/
    private Reply reply; // The underlying Reply entity object containing core reply data.
    private boolean isRead; // Has *this user* read this specific reply? (User Story #6/#9 context)

    /* Constructors ******************************************************************************/
    /**
     * <p> Method: ReplyInfo() - Constructor </p>
     * <p> Description: Initializes the DTO with the base Reply object. The read status
     * must be set separately using the setter, typically after being retrieved by the Database layer. </p>
     * @param reply The core Reply entity object. Cannot be null.
     * @throws IllegalArgumentException if the provided Reply object is null.
     */
    public ReplyInfo(Reply reply) {
         if (reply == null) {
            // Why this check? Prevents NullPointerException if invalid data is passed.
            throw new IllegalArgumentException("Reply object cannot be null for ReplyInfo DTO.");
        }
        this.reply = reply;
        // Default read status to false; Database layer will update it based on query results.
        this.isRead = false; 
    }

    /* Accessors and Mutators (Getters and Setters) ********************************************/

    /**
     * <p> Method: getReply() </p>
     * <p> Description: Returns the underlying Reply entity object. </p>
     * @return The encapsulated Reply object.
     */
    public Reply getReply() {
        return reply;
    }

    // --- Delegated Getters ---
    // Why delegate? Simplifies accessing core reply data from the DTO in the Controller/View.
    
    /** Gets the reply ID from the underlying Reply object. */
    public int getReplyID() { return reply.getReplyID(); }
    /** Gets the parent post ID from the underlying Reply object. */
    public int getPostID() { return reply.getPostID(); }
    /** Gets the author's username from the underlying Reply object. */
    public String getAuthorUsername() { return reply.getAuthorUsername(); }
    /** Gets the content from the underlying Reply object. */
    public String getContent() { return reply.getContent(); }
    /** Gets the timestamp from the underlying Reply object. */
    public Timestamp getTimestamp() { return reply.getTimestamp(); }


    /**
     * <p> Method: isRead() </p>
     * <p> Description: Checks if the current viewing user has marked this specific reply as read. </p>
     * @return true if the reply is marked as read by the viewing user, false otherwise.
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * <p> Method: setRead() </p>
     * <p> Description: Sets the read status of this reply for the current viewing user. Called by the Database layer. </p>
     * @param isRead true if the reply is read by the viewing user, false otherwise.
     */
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}