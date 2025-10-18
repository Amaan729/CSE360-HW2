package guiStudent;

// HW2 Change: Import the new Discussion Board View to navigate to it.
import guiDiscussionBoard.ViewDiscussionBoard;

/**
 * <p> Title: ControllerStudentHome Class. </p>
 * * <p> Description: This Controller handles actions initiated from the ViewStudentHome.
 * It provides methods for logging out, quitting the application, and navigating
 * to other parts of the application like the User Update screen or the Discussion Board. </p>
 * * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * * @author Amaan Sayed
 * * @version 2.10 HW2 Integration - Added navigation to Discussion Board
 */
public class ControllerStudentHome {

    /* Original TP1 Methods ***********************************************************************/

    /**
     * <p> Method: performLogout() </p>
     * <p> Description: Logs out the current user by returning to the main login screen. </p>
     */
    protected static void performLogout() {
        // Why call ViewUserLogin? This is the standard entry point for logging in/out.
        guiUserLogin.ViewUserLogin.displayUserLogin(ViewStudentHome.theStage);
    }

    /**
     * <p> Method: performQuit() </p>
     * <p> Description: Terminates the Java Virtual Machine, effectively closing the application. </p>
     */
    protected static void performQuit() {
        // Why System.exit(0)? Standard way to signal a normal program termination.
        System.exit(0);
    }

    // Note: The performUpdate() method was implicitly present via the setOnAction in ViewStudentHome
    // calling ViewUserUpdate directly. Keeping that structure as per baseline. If more logic
    // were needed for update, a dedicated method would be added here.

    /* HW2 Change: New Method for Discussion Board Navigation ***********************************/

    /**
     * <p> Method: performGoToDiscussionBoard() </p>
     * <p> Description: Handles the action when the "Go to Discussion Board" button is clicked.
     * It calls the static display method of the ViewDiscussionBoard to switch the scene. </p>
     */
    protected static void performGoToDiscussionBoard() {
        // Why call ViewDiscussionBoard.displayDiscussionBoard? This follows the singleton display
        // pattern used throughout the application for navigating between views.
        ViewDiscussionBoard.displayDiscussionBoard(ViewStudentHome.theStage, ViewStudentHome.theUser);
    }
    // --- End of HW2 Change ---
}