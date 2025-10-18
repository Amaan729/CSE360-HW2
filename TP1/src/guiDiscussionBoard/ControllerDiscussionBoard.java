package guiDiscussionBoard;

import database.Database;
import applicationMain.FoundationsMain; 
// DTOs are essential for transferring combined data
import dto.PostInfo;
import dto.ReplyInfo;
// Base entities are still needed
import entityClasses.Post;
import entityClasses.Reply;
import entityClasses.User;

// --- Necessary JavaFX Imports ---
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;         // For dialogs
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;       // For expandable dialog content
import javafx.scene.layout.Priority;     // For expandable dialog content

// --- Necessary Java Util Imports ---
import java.util.Collections; // For empty list return
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p> Title: ControllerDiscussionBoard Class </p>
 * <p> Description: This Controller manages the logic for the Discussion Board view (ViewDiscussionBoard).
 * It handles user actions like creating/editing/deleting posts and replies, searching, filtering,
 * and marking items as read. It interacts with the Database (Model) to persist and retrieve data,
 * including calculated counts and read status, fulfilling the expanded HW2 requirements for the
 * Student role. </p>
 * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * @author Amaan Sayed
 * @version 3.00 HW2 Expanded Scope Implementation
 */
public class ControllerDiscussionBoard {

    /* Attributes ********************************************************************************/
    // Singleton database instance, consistent with baseline architecture.
    private static Database theDatabase = applicationMain.FoundationsMain.database;

    /* Methods for Populating UI Elements ********************************************************/

    /**
     * <p> Method: populatePostsTable() </p>
     * <p> Description: Fetches post data (using PostInfo DTOs) from the database
     * based on the 'My Posts' filter state and updates the posts TableView. Called on initial view,
     * after create/delete, clearing search, or toggling the 'My Posts' filter. </p>
     * @param showOnlyMyPosts If true, fetches only posts by the current user; otherwise, fetches all posts.
     */
    protected static void populatePostsTable(boolean showOnlyMyPosts) {
        // Essential: Get the current user's *database ID* for personalized queries.
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) { // Error handling if user ID couldn't be found
            showError("Could not identify current user. Please log out and log back in.");
            ViewDiscussionBoard.postsTable.setItems(FXCollections.emptyObservableList()); // Clear table
            return;
        }

        List<PostInfo> postsInfo; // List to hold DTO results

        // Choose the correct database query based on the filter.
        if (showOnlyMyPosts) {
            // User Story #5: "As a student, I can see a list of my posts..."
            postsInfo = theDatabase.getPostsByAuthorWithInfo(ViewDiscussionBoard.theUser.getUserName(), currentUserID);
        } else {
            // User Story #2: "As a student, I can see a list of posts others have made..."
            postsInfo = theDatabase.getAllPostsWithInfo(currentUserID);
        }

        // Convert List to ObservableList and update the UI TableView.
        ObservableList<PostInfo> observablePosts = FXCollections.observableArrayList(postsInfo);
        ViewDiscussionBoard.postsTable.setItems(observablePosts);

        // Reset selections/dependent UI elements when the main table reloads.
        ViewDiscussionBoard.postsTable.getSelectionModel().clearSelection();
        ViewDiscussionBoard.repliesListView.getItems().clear();
        // Also clear search field when explicitly showing all or my posts
        if (!ViewDiscussionBoard.searchTextField.getText().isEmpty()){
            ViewDiscussionBoard.searchTextField.clear();
            ViewDiscussionBoard.searchThreadComboBox.getSelectionModel().select("All Threads");
        }
    }

    /**
     * <p> Method: populateRepliesList() </p>
     * <p> Description: Fetches replies (as ReplyInfo DTOs) for a specific postID, considering the
     * 'Unread Only' filter. Updates the replies ListView. Also marks the displayed replies
     * as read for the current user *after* retrieval. Handles display for replies to deleted posts. </p>
     * @param postID The ID of the post whose replies are needed.
     * @param unreadOnly If true (toggle selected), fetch only replies not yet read by the current user (User Story #6).
     */
    protected static void populateRepliesList(int postID, boolean unreadOnly) {
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) {
            showError("Could not identify current user for loading replies.");
            ViewDiscussionBoard.repliesListView.setItems(FXCollections.emptyObservableList());
            return;
        }

        // Fetch replies using the method that includes read status and filtering.
        List<ReplyInfo> repliesInfo = theDatabase.getRepliesForPostWithInfo(postID, currentUserID, unreadOnly);

        // Update the ListView in the View.
        ObservableList<ReplyInfo> observableReplies = FXCollections.observableArrayList(repliesInfo);
        ViewDiscussionBoard.repliesListView.setItems(observableReplies);

        // --- Mark displayed replies as read ---
        // Fulfills implication of User Story #6 (viewing makes them not unread).
        // Only mark *after* fetching, especially if filtering by unread.
        boolean markedAnyRead = false;
        for (ReplyInfo replyInfo : repliesInfo) {
            if (!replyInfo.isRead()) { // Only mark if currently unread
                theDatabase.markReplyAsRead(replyInfo.getReplyID(), currentUserID);
                // No need to update DTO's isRead flag here, CellFactory handles initial display.
                // Next full refresh will reflect the change.
                markedAnyRead = true; // Flag that counts might need update
            }
        }

        // If we marked any replies as read, refresh the posts table to update the 'Unread' count column.
        if (markedAnyRead) {
            refreshPostTableKeepingSelection();
        }
    }

    /* Event Handlers for UI Actions ************************************************************/

    /**
     * <p> Method: handleMyPostsToggle() </p>
     * <p> Description: Handles the action for the "Show Only My Posts" toggle button (User Story #5).
     * Clears search criteria and refreshes the posts table based on the toggle state. </p>
     */
    protected static void handleMyPostsToggle() {
        // Clear search fields when activating/deactivating this filter for clarity.
        ViewDiscussionBoard.searchTextField.clear();
        ViewDiscussionBoard.searchThreadComboBox.getSelectionModel().select("All Threads");
        // Reload the posts table, passing the toggle's selected state.
        populatePostsTable(ViewDiscussionBoard.toggleMyPosts.isSelected());
    }

    /**
     * <p> Method: handleUnreadRepliesToggle() </p>
     * <p> Description: Handles the action for the "Show Unread Only" toggle button for replies (User Story #6).
     * Reloads the replies list for the currently selected post using the new filter state. </p>
     */
    protected static void handleUnreadRepliesToggle() {
        // Find out which post is currently selected.
        PostInfo selectedPostInfo = ViewDiscussionBoard.postsTable.getSelectionModel().getSelectedItem();
        // If a post is selected, reload its replies using the toggle's current state.
        if (selectedPostInfo != null) {
            populateRepliesList(selectedPostInfo.getPostID(), ViewDiscussionBoard.toggleUnreadReplies.isSelected());
        } else {
            // If no post is selected, just ensure the replies list is empty.
             ViewDiscussionBoard.repliesListView.getItems().clear();
        }
    }

    /**
     * <p> Method: performCreatePost() </p>
     * <p> Description: Handles the "Create Post" button action (User Story #1). Prompts for thread, title,
     * content; validates input (non-empty title/content - User Stories #7/#8); saves via Database; refreshes post table. </p>
     */
    protected static void performCreatePost() {
         int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
         if (currentUserID == -1) { showError("User session error. Cannot create post."); return; }
         String currentUsername = ViewDiscussionBoard.theUser.getUserName();

        // Get selected thread from the creation ComboBox (User Story #10).
        String selectedThread = ViewDiscussionBoard.threadComboBox.getValue();
        // Apply default if nothing selected (User Story #11).
        if (selectedThread == null || selectedThread.equals("<Select a Thread>")) { selectedThread = "General"; }

        // Prompt for Title.
        TextInputDialog titleDialog = new TextInputDialog();
        titleDialog.setTitle("Create New Post");
        titleDialog.setHeaderText("Enter Title (Thread: " + selectedThread + ")");
        titleDialog.setContentText("Title:");
        Optional<String> titleResult = titleDialog.showAndWait();

        // Validate Title (User Story #7).
        if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {
            String title = titleResult.get().trim();

            // Prompt for Content using expandable dialog.
            TextArea contentArea = new TextArea(); contentArea.setWrapText(true);
            Alert contentDialog = createContentDialog("Create New Post", "Enter Content:", contentArea);
            Optional<ButtonType> contentResult = contentDialog.showAndWait();

            // Validate Content (User Story #8).
            if (contentResult.isPresent() && contentResult.get() == ButtonType.OK) {
                String content = contentArea.getText().trim();
                if (content.isEmpty()) { showError("Post content cannot be empty."); return; }

                // Create Post object and save to database.
                Post newPost = new Post();
                newPost.setTitle(title);
                newPost.setContent(content);
                newPost.setAuthorUsername(currentUsername);
                newPost.setThreadName(selectedThread);
                theDatabase.createPost(newPost); // Use simple DB method for creation

                // Refresh UI.
                populatePostsTable(ViewDiscussionBoard.toggleMyPosts.isSelected()); // Respect current filter
            } // else: User cancelled content dialog
        } else if (titleResult.isPresent()) { // User entered blank title
            showError("Post title cannot be empty.");
        } // else: User cancelled title dialog
    }

    /**
     * <p> Method: performEditPost() </p>
     * <p> Description: Handles editing the selected post. Validates selection, checks authorization
     * (author only - User Story #10 Validation), prompts for changes, validates input, updates DB, refreshes UI. </p>
     */
    protected static void performEditPost() {
        PostInfo selectedPostInfo = ViewDiscussionBoard.postsTable.getSelectionModel().getSelectedItem();
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) { showError("User session error. Cannot edit post."); return; }

        if (selectedPostInfo == null) { showError("Please select a post to edit."); return; }
        Post postToEdit = selectedPostInfo.getPost();

        // Authorization Check (User Story #10 Validation).
        if (!postToEdit.getAuthorUsername().equals(ViewDiscussionBoard.theUser.getUserName())) {
            showError("You can only edit your own posts."); return;
        }

        // Prompt for Title (pre-filled).
        TextInputDialog titleDialog = new TextInputDialog(postToEdit.getTitle());
        titleDialog.setTitle("Edit Post"); titleDialog.setHeaderText("Enter New Title:"); titleDialog.setContentText("Title:");
        Optional<String> titleResult = titleDialog.showAndWait();

        // Validate Title & Prompt for Content.
        if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {
            String newTitle = titleResult.get().trim();

            // Prompt for Content (pre-filled).
            TextArea contentArea = new TextArea(postToEdit.getContent()); contentArea.setWrapText(true);
            Alert contentDialog = createContentDialog("Edit Post", "Enter New Content:", contentArea);
            Optional<ButtonType> contentResult = contentDialog.showAndWait();

            // Validate Content & Update DB.
            if (contentResult.isPresent() && contentResult.get() == ButtonType.OK) {
                String newContent = contentArea.getText().trim();
                if (newContent.isEmpty()) { showError("Post content cannot be empty."); return; }

                postToEdit.setTitle(newTitle);
                postToEdit.setContent(newContent);
                theDatabase.updatePost(postToEdit); // Use simple update

                // Refresh UI.
                populatePostsTable(ViewDiscussionBoard.toggleMyPosts.isSelected());
            } // else: Cancelled content
        } else if (titleResult.isPresent()) { showError("Post title cannot be empty."); } // else: Cancelled title
    }

    /**
     * <p> Method: performDeletePost() </p>
     * <p> Description: Handles deleting the selected post. Validates selection, checks authorization
     * (Author or Admin - User Story #11 Validation implicitly allows Admin), confirms (User Story #13),
     * deletes from DB (replies remain - User Story #14), refreshes UI. </p>
     */
    protected static void performDeletePost() {
        PostInfo selectedPostInfo = ViewDiscussionBoard.postsTable.getSelectionModel().getSelectedItem();
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) { showError("User session error. Cannot delete post."); return; }
        User currentUser = ViewDiscussionBoard.theUser;

        if (selectedPostInfo == null) { showError("Please select a post to delete."); return; }
        Post postToDelete = selectedPostInfo.getPost();

        // Authorization Check (User Story #11 Validation allows author or admin).
        boolean isAuthor = postToDelete.getAuthorUsername().equals(currentUser.getUserName());
        boolean isAdmin = currentUser.getAdminRole();
        if (!isAuthor && !isAdmin) {
            // Error message aligns with student perspective.
            showError("You can only delete your own posts."); return;
        }

        // Confirmation Dialog (User Story #13).
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Are you sure you want to delete this post?");
        // Explicitly state reply behavior (User Story #14).
        confirmDialog.setContentText("Post Title: \"" + postToDelete.getTitle() + "\"\nReplies will NOT be deleted.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            theDatabase.deletePost(postToDelete.getPostID()); // Use simple delete
            // Refresh UI.
            populatePostsTable(ViewDiscussionBoard.toggleMyPosts.isSelected());
        } // else: User cancelled
    }

    /**
     * <p> Method: performAddReply() </p>
     * <p> Description: Handles adding a reply to the selected post (User Story #1). Validates selection,
     * prompts for content, validates (non-empty - User Story #9 Validation), saves, refreshes reply list & post table counts. </p>
     */
    protected static void performAddReply() {
        PostInfo selectedPostInfo = ViewDiscussionBoard.postsTable.getSelectionModel().getSelectedItem();
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) { showError("User session error. Cannot add reply."); return; }
        String currentUsername = ViewDiscussionBoard.theUser.getUserName();

        if (selectedPostInfo == null) { showError("Please select a post to reply to."); return; }
        int parentPostID = selectedPostInfo.getPostID();

        // Prompt for Reply Content.
        TextInputDialog replyDialog = new TextInputDialog();
        replyDialog.setTitle("Add Reply");
        replyDialog.setHeaderText("Enter reply for: \"" + selectedPostInfo.getTitle() + "\"");
        replyDialog.setContentText("Reply:");
        Optional<String> result = replyDialog.showAndWait();

        // Validate Content & Save (User Story #9 Validation).
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String content = result.get().trim();
            Reply newReply = new Reply();
            newReply.setPostID(parentPostID);
            newReply.setAuthorUsername(currentUsername);
            newReply.setContent(content);
            theDatabase.createReply(newReply); // Use simple create

            // Refresh Reply List (respecting filter).
            populateRepliesList(parentPostID, ViewDiscussionBoard.toggleUnreadReplies.isSelected());
            // Refresh Post Table to update reply counts.
            refreshPostTableKeepingSelection();

        } else if (result.isPresent()) { showError("Reply content cannot be empty."); } // else: Cancelled
    }

    /**
     * <p> Method: performSearch() </p>
     * <p> Description: Handles the search action (User Stories #7, #15, #16). Gets keyword and thread filter,
     * validates keyword, calls DB search (using *WithInfo method), updates post table. </p>
     */
    protected static void performSearch() {
        int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
        if (currentUserID == -1) { showError("User session error. Cannot perform search."); return; }

        // Get Keyword and Thread Filter from View.
        String keyword = ViewDiscussionBoard.searchTextField.getText();
        String selectedThread = ViewDiscussionBoard.searchThreadComboBox.getValue();
        // Treat "All Threads" as null for the database method (User Story #16).
        if ("All Threads".equals(selectedThread)) { selectedThread = null; }

        // Validate keyword is not empty.
        if (keyword == null || keyword.trim().isEmpty()) {
            showError("Please enter a keyword to search for.");
            return;
        }

        // Execute Search using the database method that returns DTOs.
        List<PostInfo> searchResults = theDatabase.searchPostsWithInfo(keyword.trim(), selectedThread, currentUserID);

        // Update UI Table with results.
        ObservableList<PostInfo> observableResults = FXCollections.observableArrayList(searchResults);
        ViewDiscussionBoard.postsTable.setItems(observableResults);

        // Clear dependent UI elements after search.
        ViewDiscussionBoard.postsTable.getSelectionModel().clearSelection();
        ViewDiscussionBoard.repliesListView.getItems().clear();
        // Deactivate 'My Posts' filter when performing a general search.
        ViewDiscussionBoard.toggleMyPosts.setSelected(false);
    }

    /**
     * <p> Method: markSelectedPostAsRead() </p>
     * <p> Description: Called by the TableView selection listener. Marks the newly selected post
     * as read in the database for the current user (User Story #8). Updates the DTO and refreshes
     * the table row for immediate visual feedback (e.g., unbolding). </p>
     * @param selectedPostInfo The PostInfo object corresponding to the selected row.
     */
    protected static void markSelectedPostAsRead(PostInfo selectedPostInfo) {
         // Avoid action if nothing selected or DTO already shows it as read.
         if (selectedPostInfo == null || selectedPostInfo.isRead()) {
             return;
         }
         int currentUserID = theDatabase.getUserID(ViewDiscussionBoard.theUser.getUserName());
         if (currentUserID == -1) return; // Need valid user

         // Call database method to record the read action.
         theDatabase.markPostAsRead(selectedPostInfo.getPostID(), currentUserID);

         // Update the DTO's status locally *immediately*.
         // Why? So the RowFactory styling updates correctly during the refresh.
         selectedPostInfo.setRead(true);
         // Explicitly refresh the table.
         // Why? Forces the TableView to re-evaluate the RowFactory for all rows, applying the style change.
         ViewDiscussionBoard.postsTable.refresh();
    }

     /**
     * <p> Method: refreshPostTableKeepingSelection() </p>
     * <p> Description: Helper method to reload the posts table data (e.g., after reply counts change)
     * while attempting to re-select the post that was selected before the refresh. Improves user experience. </p>
     */
     private static void refreshPostTableKeepingSelection() {
         // 1. Remember the ID of the currently selected post (if any).
         PostInfo selected = ViewDiscussionBoard.postsTable.getSelectionModel().getSelectedItem();
         int selectedId = (selected != null) ? selected.getPostID() : -1;

         // 2. Repopulate the table based on the *current filter state* ("My Posts" toggle).
         populatePostsTable(ViewDiscussionBoard.toggleMyPosts.isSelected());

         // 3. Try to re-select the post if an ID was remembered.
         if (selectedId != -1) {
             // Iterate through the newly loaded items.
             for (PostInfo item : ViewDiscussionBoard.postsTable.getItems()) {
                 if (item.getPostID() == selectedId) {
                     // Found the matching item, select it.
                     ViewDiscussionBoard.postsTable.getSelectionModel().select(item);
                     // Optional: Scroll the table view to make the re-selected item visible.
                     // ViewDiscussionBoard.postsTable.scrollTo(item);
                     break; // Stop searching once found.
                 }
             }
         }
     }


    /**
     * <p> Method: performReturn() </p>
     * <p> Description: Handles the "Return to Home" button action. Navigates back to the Student Home screen. </p>
     */
    protected static void performReturn() {
        // Navigate back using the standard display method for the Student Home view.
        guiStudent.ViewStudentHome.displayStudentHome(ViewDiscussionBoard.theStage, ViewDiscussionBoard.theUser);
    }

    /* Helper Methods ****************************************************************************/

    /**
     * <p> Method: showError() - Private Helper </p>
     * <p> Description: Displays a standardized error Alert dialog box with the specified message. </p>
     * @param message The error message text to display.
     */
    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null); // No header needed
        alert.setContentText(message);
        alert.showAndWait(); // Wait for user acknowledgment
    }

    /**
     * <p> Method: createContentDialog() - Private Helper </p>
     * <p> Description: Creates and configures an Alert dialog (CONFIRMATION type) containing an
     * expandable TextArea suitable for multi-line content input for posts/replies. </p>
     * @param title The title for the dialog window.
     * @param header The header text displayed above the content area.
     * @param contentArea The TextArea UI element to embed in the dialog.
     * @return The configured Alert object, ready to be shown via showAndWait().
     */
     private static Alert createContentDialog(String title, String header, TextArea contentArea) {
         Alert dialog = new Alert(Alert.AlertType.CONFIRMATION); // Use CONFIRMATION for OK/Cancel buttons
         dialog.setTitle(title);
         dialog.setHeaderText(header);

         // Configure TextArea for better usability in dialog.
         contentArea.setWrapText(true);
         contentArea.setMaxWidth(Double.MAX_VALUE);
         contentArea.setMaxHeight(Double.MAX_VALUE);

         // Use a GridPane to allow the TextArea to grow vertically and horizontally.
         GridPane expContent = new GridPane();
         GridPane.setVgrow(contentArea, Priority.ALWAYS); // Allow vertical expansion
         GridPane.setHgrow(contentArea, Priority.ALWAYS); // Allow horizontal expansion
         expContent.setMaxWidth(Double.MAX_VALUE);
         expContent.add(contentArea, 0, 0); // Add TextArea to grid

         // Set this grid as the expandable content area of the dialog.
         dialog.getDialogPane().setExpandableContent(expContent);
         dialog.getDialogPane().setExpanded(true); // Show it expanded by default
         // Set a simple label in the main content area (above the expandable part).
         dialog.getDialogPane().setContent(new Label("Enter Content Below:"));

         return dialog; // Return the configured dialog
     }

} 