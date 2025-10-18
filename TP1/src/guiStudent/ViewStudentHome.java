package guiStudent;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox; // Using VBox for layout
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;  // For padding
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import database.Database;
import entityClasses.User;
import guiUserUpdate.ViewUserUpdate; // Keep this import for the Account Update button

/**
 * <p> Title: ViewStudentHome Class. </p>
 * * <p> Description: The Java/FX-based Student Home Page. This view provides the main
 * navigation point for users logged in with the student role. It allows access to
 * account updates and the discussion board. </p>
 * * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * * @author Amaan Sayed
 * * @version 2.10 HW2 Integration - Added Discussion Board button
 */
public class ViewStudentHome {

    /* Constants *********************************************************************************/
    private static final double WINDOW_WIDTH = applicationMain.FoundationsMain.WINDOW_WIDTH;
    private static final double WINDOW_HEIGHT = applicationMain.FoundationsMain.WINDOW_HEIGHT;
    private static final double HORIZONTAL_MARGIN = 20.0;
    private static final double VERTICAL_SPACING = 10.0;

    /* Attributes (UI Elements) ******************************************************************/
    // Main layout container
    private static VBox mainLayout;

    // Top section (unchanged)
    protected static Label label_PageTitle = new Label("Student Home Page");
    protected static Label label_UserDetails = new Label(); // User details text set dynamically
    protected static Button button_UpdateThisUser = new Button("Account Update");
    protected static Line line_Separator1 = new Line(); // Positioned by layout container

    // Middle section
    // HW2 Change: Added a new button to navigate to the Discussion Board
    protected static Button button_GoToDiscussion = new Button("Go to Discussion Board");

    // Bottom section (unchanged)
    protected static Line line_Separator4 = new Line(); // Positioned by layout container
    protected static Button button_Logout = new Button("Logout");
    protected static Button button_Quit = new Button("Quit");

    /* Attributes (MVC Components) ***************************************************************/
    private static ViewStudentHome theView; // Singleton instance
    // Why static? Consistent with baseline, allows access from Controller without passing instance.
    private static Database theDatabase = applicationMain.FoundationsMain.database;
    protected static Stage theStage;         // Reference to the main application stage
    protected static Pane theRootPane;      // Original root pane, now holds mainLayout
    protected static User theUser;           // Currently logged-in user
    private static Scene theViewStudentHomeScene; // The scene for this view

    // Constant role identifier (unchanged)
    protected static final int theRole = 2; // Student role ID

    /* Methods ***********************************************************************************/

    /**
     * <p> Method: displayStudentHome() - Static Entry Point </p>
     * * <p> Description: Displays the Student Home screen. Follows the singleton pattern.
     * Initializes the view if needed, updates dynamic content (user details label),
     * and shows the scene on the stage. </p>
     * * @param ps The primary stage of the application.
     * @param user The User object representing the logged-in student.
     */
    public static void displayStudentHome(Stage ps, User user) {
        theStage = ps;
        theUser = user;

        // Instantiate the singleton view if it's the first time displaying it.
        // Why Singleton? Matches baseline architecture, ensures only one instance exists.
        if (theView == null) {
            theView = new ViewStudentHome(); // Calls the private constructor
        }

        // Update dynamic content before showing.
        // Why update here? Ensures the correct username is displayed for the current session.
        theDatabase.getUserAccountDetails(user.getUserName()); // Refresh cache just in case
        applicationMain.FoundationsMain.activeHomePage = theRole; // Set active role context
        label_UserDetails.setText("User: " + theUser.getUserName()); // Set the username label

        // Set the window title and display the scene.
        theStage.setTitle("CSE 360 Foundations: Student Home Page");
        theStage.setScene(theViewStudentHomeScene);
        theStage.show();
    }

    /**
     * <p> Method: ViewStudentHome() - Private Constructor </p>
     * * <p> Description: Initializes all static UI elements and layout for the Student Home screen.
     * Sets up styles, positions (handled by VBox), and event handlers. Called only once
     * when the singleton instance is created. </p>
     */
    private ViewStudentHome() {
        // Use VBox for primary layout - simplifies vertical arrangement.
        mainLayout = new VBox(VERTICAL_SPACING);
        mainLayout.setPadding(new Insets(VERTICAL_SPACING, HORIZONTAL_MARGIN, VERTICAL_SPACING, HORIZONTAL_MARGIN));
        mainLayout.setAlignment(Pos.TOP_CENTER); // Center content horizontally

        // Root Pane setup (consistent with baseline structure)
        theRootPane = new Pane(mainLayout); // Place VBox onto the Pane
        theViewStudentHomeScene = new Scene(theRootPane, WINDOW_WIDTH, WINDOW_HEIGHT);

        // --- Configure UI Elements ---

        // 1. Top Section (Title, User Details, Update Button)
        setupLabelUI(label_PageTitle, "Arial", 28, WINDOW_WIDTH - 2 * HORIZONTAL_MARGIN, Pos.CENTER);
        // Note: label_UserDetails text is set dynamically in displayStudentHome()
        setupLabelUI(label_UserDetails, "Arial", 20, WINDOW_WIDTH - 2 * HORIZONTAL_MARGIN, Pos.BASELINE_LEFT);
        setupButtonUI(button_UpdateThisUser, "Dialog", 18, 170, Pos.CENTER);
        // Event handler linking to User Update view (unchanged from TP1)
        button_UpdateThisUser.setOnAction((event) -> {
            ViewUserUpdate.displayUserUpdate(theStage, theUser);
        });
        // Separator line configuration (width set dynamically)
        line_Separator1.setStartX(0); line_Separator1.setEndX(WINDOW_WIDTH - 2 * HORIZONTAL_MARGIN);
        line_Separator1.setStrokeWidth(1);

        // 2. Middle Section (HW2 Change: Discussion Board Button)
        setupButtonUI(button_GoToDiscussion, "Dialog", 18, 250, Pos.CENTER);
        // Event handler linking to the Controller method that opens the discussion board.
        // Why Controller method? Follows MVC - View delegates actions to Controller.
        button_GoToDiscussion.setOnAction((event) -> {
            ControllerStudentHome.performGoToDiscussionBoard();
        });

        // 3. Bottom Section (Logout, Quit Buttons)
        setupButtonUI(button_Logout, "Dialog", 18, 250, Pos.CENTER);
        button_Logout.setOnAction((event) -> { ControllerStudentHome.performLogout(); }); // Link to Controller

        setupButtonUI(button_Quit, "Dialog", 18, 250, Pos.CENTER);
        button_Quit.setOnAction((event) -> { ControllerStudentHome.performQuit(); });     // Link to Controller
        // Separator line configuration
        line_Separator4.setStartX(0); line_Separator4.setEndX(WINDOW_WIDTH - 2 * HORIZONTAL_MARGIN);
        line_Separator4.setStrokeWidth(1);

        // --- Add elements to the VBox layout in display order ---
        // Using HBox for buttons that should appear side-by-side or centered relative to each other.
        HBox topRow = new HBox(); // To potentially place user details and update button side-by-side if needed
        topRow.getChildren().addAll(label_UserDetails); // Simplified for now
        // Could add button_UpdateThisUser here too if horizontal layout desired.
        
        HBox bottomButtons = new HBox(VERTICAL_SPACING); // HBox for Logout/Quit
        bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.getChildren().addAll(button_Logout, button_Quit);

        mainLayout.getChildren().addAll(
            label_PageTitle,
            topRow, // Contains user details
            button_UpdateThisUser, // Keeping it separate for now
            line_Separator1,
            // Add spacing or specific layout panes if more structure is needed
            new Pane() {{ setPrefHeight(100); }}, // Add some vertical space before the button
            button_GoToDiscussion, // HW2 Change: Added discussion button
            new Pane() {{ setPrefHeight(100); }}, // Add space after button
            line_Separator4,
            bottomButtons // Contains Logout/Quit
        );
        
        // Ensure VBox fills the pane width
         mainLayout.setPrefWidth(WINDOW_WIDTH - 2*HORIZONTAL_MARGIN);
         
         // VBox manages positioning within the Pane. Set Pane size.
         theRootPane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    }


    /* Helper Methods for UI Styling (Consistent with baseline) *******************************/
    // These helpers reduce code duplication when setting common properties for UI elements.

    /**
     * Helper to configure standard Label properties.
     */
    private static void setupLabelUI(Label l, String ff, double f, double w, Pos p){
        l.setFont(Font.font(ff, f));
        l.setMinWidth(w);
        l.setAlignment(p);
        // Removed layoutX/Y as VBox manages position.
    }

    /**
     * Helper to configure standard Button properties.
     */
    private static void setupButtonUI(Button b, String ff, double f, double w, Pos p){
        b.setFont(Font.font(ff, f));
        b.setMinWidth(w);
        b.setAlignment(p);
        // Removed layoutX/Y as VBox/HBox manage position.
    }
}