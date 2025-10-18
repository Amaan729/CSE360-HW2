package guiDiscussionBoard;

// --- Core Imports ---
import database.Database;
import applicationMain.FoundationsMain; // <-- **Crucial Import**
import dto.PostInfo;
import dto.ReplyInfo;
import entityClasses.Post; // Still needed for context
import entityClasses.User;

// --- JavaFX Imports ---
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
// import javafx.scene.paint.Color; // Using setStyle instead
// import javafx.scene.shape.Line; // Not used in VBox layout
import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight; // Using setStyle instead
import javafx.stage.Stage;

// --- Java Util Imports ---
import java.sql.Timestamp;
// import java.util.Arrays; // Not used

/**
 * <p> Title: ViewDiscussionBoard Class </p>
 * <p> Description: The Java/FX-based View for the main discussion board page.
 * Sets up the UI for viewing posts (with counts and read status), replies (with filtering),
 * searching, selecting threads, and initiating CRUD operations, fulfilling all Student user stories. </p>
 * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * @author Amaan Sayed
 * @version 3.01 HW2 Expanded Scope Implementation - Corrected DB Reference
 */
public class ViewDiscussionBoard {

    /* Constants *********************************************************************************/
    // Use constants from FoundationsMain for consistency
    private static final double WINDOW_WIDTH = FoundationsMain.WINDOW_WIDTH;
    private static final double WINDOW_HEIGHT = FoundationsMain.WINDOW_HEIGHT;
    private static final double HORIZONTAL_MARGIN = 20.0;
    private static final double VERTICAL_SPACING = 8.0;

    /* Attributes (UI Elements) ******************************************************************/
    // Using protected for elements the Controller might need to access directly (like ComboBoxes, TextFields, ToggleButtons)
    // Using private for layout containers managed entirely within this View.
    private static VBox mainLayout;
    protected static Label label_PageTitle = new Label("Discussion Board");
    private static HBox filterSearchBox;
    protected static ToggleButton toggleMyPosts = new ToggleButton("Show Only My Posts");
    protected static TextField searchTextField = new TextField();
    protected static ComboBox<String> searchThreadComboBox; // Initialized in static block
    protected static Button searchButton = new Button("Search");
    protected static Button clearSearchButton = new Button("Show All Posts");
    protected static Label label_PostsTable = new Label("Posts:");
    protected static TableView<PostInfo> postsTable = new TableView<>();
    private static HBox postActionBox;
    protected static ComboBox<String> threadComboBox = new ComboBox<>();
    protected static Button button_CreatePost = new Button("Create Post in Thread:");
    protected static Button button_EditPost = new Button("Edit Selected Post");
    protected static Button button_DeletePost = new Button("Delete Selected Post");
    private static HBox repliesHeaderBox;
    protected static Label label_RepliesList = new Label("Replies to Selected Post:");
    protected static ToggleButton toggleUnreadReplies = new ToggleButton("Show Unread Only");
    protected static ListView<ReplyInfo> repliesListView = new ListView<>();
    protected static Button button_AddReply = new Button("Add Reply to Selected Post");
    protected static Button button_Return = new Button("Return to Home");

    /* Attributes (MVC Components) ***************************************************************/
    // --- ** FIX: Correctly reference the static database instance from FoundationsMain ---
    // Why static? Matches the pattern in baseline code (e.g., ViewAdminHome) where controllers
    // access static view fields and the view accesses the static main database instance.
    private static Database theDatabase = FoundationsMain.database;
    // --- End Fix ---
    protected static Stage theStage; // Reference to the main application window stage
    protected static User theUser;   // Reference to the currently logged-in user
    private static Pane theRootPane; // Holds the mainLayout VBox
    public static Scene theDiscussionBoardScene = null; // The scene for this view
    private static ViewDiscussionBoard theView; // Singleton instance

    /* Static Initializer Block ******************************************************************/
    // Define thread options accessible to students (User Story #17)
    private static final ObservableList<String> threadOptions =
        FXCollections.observableArrayList("General", "Homework 1", "Team Project 1", "Homework 2", "Quizzes");
    // Define options for the search filter ComboBox (User Story #16)
    private static final ObservableList<String> searchThreadOptions =
        FXCollections.observableArrayList("All Threads", "General", "Homework 1", "Team Project 1", "Homework 2", "Quizzes");

    // Initialize ComboBox that needs options defined above
    // Why static block? Ensures the ComboBox is initialized with options before the constructor runs.
    static {
        searchThreadComboBox = new ComboBox<>(searchThreadOptions);
    }


    /* Methods ***********************************************************************************/

    /**
     * <p> Method: displayDiscussionBoard() - Static Entry Point </p>
     * <p> Description: Displays the Discussion Board scene on the main stage. Follows singleton pattern.
     * Initializes the view on first call, then resets UI state and populates dynamic data on every call. </p>
     * @param ps The primary stage of the JavaFX application.
     * @param user The User object representing the currently logged-in student.
     */
    public static void displayDiscussionBoard(Stage ps, User user) {
        theStage = ps;
        theUser = user;
        // Initialize singleton view if this is the first time.
        if (theView == null) {
            theView = new ViewDiscussionBoard(); // Calls the private constructor
        }

        // Reset UI state to defaults before showing the scene.
        toggleMyPosts.setSelected(false);
        searchTextField.clear();
        searchThreadComboBox.getSelectionModel().select("All Threads"); // Default search filter
        threadComboBox.getSelectionModel().select("General");           // Default thread for creation
        ControllerDiscussionBoard.populatePostsTable(false);            // Load initial data (all posts view)
        repliesListView.getItems().clear();                             // Clear replies from previous view
        toggleUnreadReplies.setSelected(false);                         // Default reply filter

        // Set window title and show the scene.
        theStage.setTitle("CSE360 - Discussion Board - User: " + theUser.getUserName());
        theStage.setScene(theDiscussionBoardScene);
        theStage.show();
    }

    /**
     * <p> Method: ViewDiscussionBoard() - Private Constructor </p>
     * <p> Description: Initializes all static UI elements, layout containers (VBox, HBox),
     * and sets up event handlers for the Discussion Board view. Called only once for the singleton instance. </p>
     */
    private ViewDiscussionBoard() {
        // --- Main Layout Setup ---
        // Why VBox? Provides simple vertical stacking with consistent spacing.
        mainLayout = new VBox(VERTICAL_SPACING);
        mainLayout.setPadding(new Insets(VERTICAL_SPACING, HORIZONTAL_MARGIN, VERTICAL_SPACING, HORIZONTAL_MARGIN));
        mainLayout.setAlignment(Pos.TOP_CENTER); // Center content within the VBox width.

        // Use Pane as root to easily contain the VBox (consistent with baseline)
        theRootPane = new Pane(mainLayout);
        theDiscussionBoardScene = new Scene(theRootPane, WINDOW_WIDTH, WINDOW_HEIGHT);

        // --- Configure and Arrange UI Sections ---
        // Each setup method configures a part of the UI.
        setupLabelUI(label_PageTitle, "Arial", 28, WINDOW_WIDTH - (2*HORIZONTAL_MARGIN), Pos.CENTER);
        setupFilterSearchArea(); // Configures the HBox with filter/search controls
        setupLabelUI(label_PostsTable, "Arial", 16, WINDOW_WIDTH - (2*HORIZONTAL_MARGIN), Pos.BASELINE_LEFT);
        setupPostsTable();       // Configures the TableView for posts
        setupPostActionArea();   // Configures the HBox with post action buttons/combo box
        setupRepliesArea();      // Configures the HBox header and ListView for replies
        setupButtonUI(button_AddReply, "Dialog", 14, 250, Pos.CENTER);
        setupButtonUI(button_Return, "Dialog", 16, 200, Pos.CENTER);

        // --- Wire Up Event Handlers ---
        // Links UI actions (button clicks, toggle changes) to methods in the Controller.
        // Why lambdas? Concise syntax for simple event handling.
        toggleMyPosts.setOnAction(e -> ControllerDiscussionBoard.handleMyPostsToggle());
        searchButton.setOnAction(e -> ControllerDiscussionBoard.performSearch());
        clearSearchButton.setOnAction(e -> ControllerDiscussionBoard.populatePostsTable(toggleMyPosts.isSelected()));
        button_CreatePost.setOnAction(e -> ControllerDiscussionBoard.performCreatePost());
        button_EditPost.setOnAction(e -> ControllerDiscussionBoard.performEditPost());
        button_DeletePost.setOnAction(e -> ControllerDiscussionBoard.performDeletePost());
        button_AddReply.setOnAction(e -> ControllerDiscussionBoard.performAddReply());
        toggleUnreadReplies.setOnAction(e -> ControllerDiscussionBoard.handleUnreadRepliesToggle());
        button_Return.setOnAction(e -> ControllerDiscussionBoard.performReturn());

        // Add listener to the posts table selection model.
        // Why listener? To dynamically load replies and mark posts as read when the user clicks a post.
        postsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> { // Called whenever the selected item changes
                if (newSelection != null) { // A post row is selected
                    // Tell controller to mark this post as read (updates DB and DTO)
                    ControllerDiscussionBoard.markSelectedPostAsRead(newSelection);
                    // Tell controller to load replies for this post, respecting the reply filter toggle
                    ControllerDiscussionBoard.populateRepliesList(newSelection.getPostID(), toggleUnreadReplies.isSelected());
                    // Force the table to redraw the selected row to reflect read status change immediately
                    postsTable.refresh();
                } else { // No post row is selected (selection cleared)
                    // Clear the replies list view.
                    repliesListView.getItems().clear();
                }
            });

        // --- Add All Configured Sections/Elements to the Main Layout ---
        mainLayout.getChildren().addAll(
                label_PageTitle,
                filterSearchBox,    // HBox with filter/search
                label_PostsTable,
                postsTable,         // TableView for posts
                postActionBox,      // HBox with post actions
                repliesHeaderBox,   // HBox with replies label and filter toggle
                repliesListView,    // ListView for replies
                button_AddReply,
                button_Return
        );
        // Allow table and list view to grow vertically to fill available space.
        // Why Vgrow? Makes the UI responsive if the window height changes.
        VBox.setVgrow(postsTable, Priority.ALWAYS);
        VBox.setVgrow(repliesListView, Priority.ALWAYS);

        // --- Final Layout Adjustments ---
        // Ensure VBox takes available width within margins for proper centering/alignment.
        mainLayout.setPrefWidth(WINDOW_WIDTH - 2*HORIZONTAL_MARGIN);
        mainLayout.setLayoutX(HORIZONTAL_MARGIN); // Position VBox within the Pane
        mainLayout.setLayoutY(VERTICAL_SPACING);

        // Set the size of the root Pane (used by the Scene).
        theRootPane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    /**
     * <p> Method: setupFilterSearchArea() - Private Helper </p>
     * <p> Description: Configures the HBox containing the 'My Posts' toggle, search text field,
     * thread filter ComboBox for search, and search/clear buttons. </p>
     */
    private void setupFilterSearchArea() {
        filterSearchBox = new HBox(VERTICAL_SPACING);
        filterSearchBox.setAlignment(Pos.CENTER_LEFT);

        setupToggleButtonUI(toggleMyPosts, "Dialog", 14, 150, Pos.CENTER);
        searchTextField.setPromptText("Enter keyword(s)...");
        searchTextField.setPrefWidth(250);
        setupComboBoxUI(searchThreadComboBox, "Dialog", 14, 120);
        searchThreadComboBox.getSelectionModel().select("All Threads"); // Default search filter
        setupButtonUI(searchButton, "Dialog", 14, 80, Pos.CENTER);
        setupButtonUI(clearSearchButton, "Dialog", 14, 100, Pos.CENTER);

        // Add elements in desired horizontal order.
        filterSearchBox.getChildren().addAll(toggleMyPosts, searchTextField, searchThreadComboBox, searchButton, clearSearchButton);
    }

    /**
     * <p> Method: setupPostsTable() - Private Helper </p>
     * <p> Description: Configures the TableView for posts: sets size, placeholder text,
     * defines columns (binding to PostInfo properties), and sets a RowFactory for styling unread posts. </p>
     */
    private void setupPostsTable() {
        postsTable.setPrefHeight(220); // Allocate vertical space
        postsTable.setPlaceholder(new Label("No posts found. Use 'Create Post' or 'Show All Posts'."));

        // Define Table Columns
        // Why PropertyValueFactory? Links column data to getter methods in the PostInfo DTO.
        // String format like "replyCount" maps to getReplyCount(). "post.title" maps to getPost().getTitle().
        TableColumn<PostInfo, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title")); // Uses delegated getTitle()
        titleCol.setPrefWidth(240);

        TableColumn<PostInfo, String> threadCol = new TableColumn<>("Thread");
        threadCol.setCellValueFactory(new PropertyValueFactory<>("threadName")); // Uses delegated getThreadName()
        threadCol.setPrefWidth(90);

        TableColumn<PostInfo, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("authorUsername")); // Uses delegated getAuthorUsername()
        authorCol.setPrefWidth(100);

        // HW2 Expanded Scope: Column for total reply count (User Story #9)
        TableColumn<PostInfo, Integer> repliesCol = new TableColumn<>("Replies");
        repliesCol.setCellValueFactory(new PropertyValueFactory<>("replyCount")); // Direct DTO property
        repliesCol.setPrefWidth(60); repliesCol.setStyle("-fx-alignment: CENTER;"); // Center align numbers

        // HW2 Expanded Scope: Column for unread reply count (User Story #9)
        TableColumn<PostInfo, Integer> unreadCol = new TableColumn<>("Unread");
        unreadCol.setCellValueFactory(new PropertyValueFactory<>("unreadReplyCount")); // Direct DTO property
        unreadCol.setPrefWidth(60); unreadCol.setStyle("-fx-alignment: CENTER;"); // Center align numbers

        TableColumn<PostInfo, Timestamp> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp")); // Uses delegated getTimestamp()
        dateCol.setPrefWidth(130);

        // Add all columns to the table. Use setAll to replace any existing columns.
        postsTable.getColumns().setAll(titleCol, threadCol, authorCol, repliesCol, unreadCol, dateCol);

        // HW2 Expanded Scope: Add Row Factory for styling unread posts (User Story #8).
        // Why Row Factory? It allows applying styles (like bolding) to an entire row based on its data item (PostInfo).
        postsTable.setRowFactory(tv -> new TableRow<PostInfo>() {
            @Override
            protected void updateItem(PostInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle(""); // Essential to clear style for empty/reused rows
                } else {
                    // Check the isRead flag from the DTO and apply bold style if false.
                    setStyle(item.isRead() ? "" : "-fx-font-weight: bold;");
                }
            }
        });
    }

    /**
     * <p> Method: setupPostActionArea() - Private Helper </p>
     * <p> Description: Configures the HBox containing the "Create Post" button, thread selection
     * ComboBox for creation, "Edit Post", and "Delete Post" buttons. </p>
     */
     private void setupPostActionArea() {
        postActionBox = new HBox(VERTICAL_SPACING);
        postActionBox.setAlignment(Pos.CENTER_LEFT);

        setupButtonUI(button_CreatePost, "Dialog", 14, 150, Pos.CENTER_LEFT); // Slightly adjust text if needed
        setupComboBoxUI(threadComboBox, "Dialog", 14, 150);
        threadComboBox.setItems(threadOptions); // Populate with student-accessible threads (User Story #10/#17)
        threadComboBox.getSelectionModel().select("General"); // Default thread for creation (User Story #11)

        setupButtonUI(button_EditPost, "Dialog", 14, 150, Pos.CENTER);
        setupButtonUI(button_DeletePost, "Dialog", 14, 150, Pos.CENTER);

        // Add elements to the HBox layout container.
        postActionBox.getChildren().addAll(button_CreatePost, threadComboBox, button_EditPost, button_DeletePost);
     }

     /**
      * <p> Method: setupRepliesArea() - Private Helper </p>
      * <p> Description: Configures the HBox header (label + filter toggle) and the ListView for replies.
      * Sets up the CellFactory for the ListView to handle displaying ReplyInfo objects, marking deleted
      * parent posts (User Story #14), and styling unread replies (User Story #6). </p>
      */
     private void setupRepliesArea() {
         // --- Header Setup ---
         repliesHeaderBox = new HBox(VERTICAL_SPACING * 2); // HBox for label and toggle button
         repliesHeaderBox.setAlignment(Pos.CENTER_LEFT);
         setupLabelUI(label_RepliesList, "Arial", 16, 400, Pos.BASELINE_LEFT); // Label text
         setupToggleButtonUI(toggleUnreadReplies, "Dialog", 14, 150, Pos.CENTER); // Toggle button
         repliesHeaderBox.getChildren().addAll(label_RepliesList, toggleUnreadReplies); // Add both to HBox

         // --- ListView Setup ---
         repliesListView.setPrefHeight(130); // Allocate vertical space

         // Cell Factory defines how each ReplyInfo item is displayed in the ListView.
         // Why Cell Factory? Gives complete control over rendering, needed for complex formatting and styling.
         repliesListView.setCellFactory(lv -> new ListCell<ReplyInfo>() {
             @Override
             protected void updateItem(ReplyInfo item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null) {
                     // Always clear text and style for empty cells to avoid display artifacts on scroll.
                     setText(null);
                     setStyle("");
                 } else {
                     // Check if the parent post still exists (User Story #14).
                     // ** Use ClassName.theDatabase for static access **
                     boolean parentPostExists = (ViewDiscussionBoard.theDatabase.getPost(item.getPostID()) != null);
                     String deletedPrefix = parentPostExists ? "" : "[Original Post Deleted] "; // Prepend if deleted

                     // Construct the display string for the reply.
                     String displayText = deletedPrefix + item.getAuthorUsername() +
                                          " (" + item.getTimestamp() + "):\n" +
                                          item.getContent();
                     setText(displayText);
                     setWrapText(true); // Allow reply text to wrap within the cell width.

                     // Apply bold style if reply DTO indicates unread (User Story #6).
                     setStyle(item.isRead() ? "" : "-fx-font-weight: bold;");
                 }
             }
         });
     }


    /* Helper Methods for UI Styling (Consistent with baseline, added ToggleButton) *****************/
    // Why helper methods? Reduces code duplication in the constructor, makes UI setup cleaner.

    /** Configures common properties for a Label. */
    private static void setupLabelUI(Label l, String ff, double f, double w, Pos p) {
        l.setFont(Font.font(ff, f));
        l.setMinWidth(Control.USE_PREF_SIZE); // Allow label to shrink to fit text if needed
        l.setMaxWidth(w);                     // Prevent label from exceeding bounds
        l.setPrefWidth(w);                    // Preferred width for layout calculations
        l.setAlignment(p);
        // layoutX/Y removed as VBox/HBox handle positioning.
    }

    /** Configures common properties for a Button. */
    private static void setupButtonUI(Button b, String ff, double f, double w, Pos p) {
        b.setFont(Font.font(ff, f));
        b.setMinWidth(w);   // Ensure button is at least this wide
        b.setPrefWidth(w);  // Preferred width
        b.setAlignment(p);
        // layoutX/Y removed.
    }

    /** Configures common properties for a ToggleButton. */
    private static void setupToggleButtonUI(ToggleButton tb, String ff, double f, double w, Pos p) {
        tb.setFont(Font.font(ff, f));
        tb.setMinWidth(w);
        tb.setPrefWidth(w);
        tb.setAlignment(p);
        // layoutX/Y removed.
    }

    /** Configures common properties for a ComboBox. */
    private static void setupComboBoxUI(ComboBox<String> c, String ff, double f, double w) {
        // Apply font using CSS style for ComboBox.
        c.setStyle("-fx-font: " + f + "px '" + ff + "';"); // Explicitly add 'px' unit for clarity
        c.setMinWidth(w);
        c.setPrefWidth(w); // Set preferred width for consistency in HBox layout.
        // layoutX/Y removed.
    }
} // End of ViewDiscussionBoard Class