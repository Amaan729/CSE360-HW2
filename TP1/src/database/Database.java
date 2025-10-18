package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import entityClasses.UserForList;
import entityClasses.User;
import java.time.Instant; // Original import

// HW2 Expanded Scope: Import Post and Reply entities
import entityClasses.Post;
import entityClasses.Reply;

// HW2 Expanded Scope: Import DTOs (Data Transfer Objects) for displaying extended info
// Note: These DTO classes need to be created in a separate 'dto' package.
import dto.PostInfo;
import dto.ReplyInfo;


/**
 * <p> Title: Database Class </p>
 * <p> Description: Manages the connection to the H2 database and provides methods
 * for all data operations, including user management, invitations, OTPs, and now
 * discussion board posts, replies, and read status tracking. Encapsulates all direct database
 * interactions for the application, following the Model part of MVC. </p>
 * <p> Copyright: Lynn Robert Carter © 2025 </p> // Copied from baseline
 * @author Amaan Sayed
 * @version 3.00 HW2 Expanded Scope Implementation
 */
public class Database {

    /* Constants *********************************************************************************/
    // JDBC driver and database URL constants, consistent with FoundationsF25/TP1.
    static final String JDBC_DRIVER = "org.h2.Driver";
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";
    // Database credentials (default for H2 embedded)
    static final String USER = "sa";
    static final String PASS = "";

    /* Attributes ********************************************************************************/
    // Made connection public temporarily for direct access in tests, consider a getter for better encapsulation.
    public Connection connection = null; // Singleton connection object to manage the DB session.
    private Statement statement = null;   // Used for executing general, non-parameterized SQL statements like CREATE TABLE.

    // Attributes for caching the currently logged-in user's details (original from TP1).
    // Why cache? Reduces database lookups for frequently needed info about the current user.
    private String currentUsername;
    private int currentUserID = -1; // HW2 Expanded: Cache User ID as it's needed often now for read status checks. Initialized to -1 (invalid).
    private String currentPassword;
    private String currentFirstName;
    private String currentMiddleName;
    private String currentLastName;
    private String currentPreferredFirstName;
    private String currentEmailAddress;
    private boolean currentAdminRole;
    private boolean currentNewStudent;
    private boolean currentNewStaff;

    /* Constructors ******************************************************************************/
    /**
     * <p> Method: Database() - Default constructor. </p>
     * <p> Description: Initializes the Database object. Connection to the database
     * is established separately via the connectToDatabase() method. </p>
     */
    public Database () {
        // No specific initialization needed in constructor itself. Relies on connectToDatabase().
    }

    /* Database Connection and Setup Methods *****************************************************/
    /**
     * <p> Method: connectToDatabase() </p>
     * <p> Description: Establishes a connection to the H2 in-memory/file database using the defined
     * constants. It loads the JDBC driver, gets the connection, creates a Statement object,
     * and calls createTables() to ensure the database schema is correctly set up. </p>
     * @throws SQLException if a database access error occurs during connection (e.g., database file locked).
     */
    public void connectToDatabase() throws SQLException {
        try {
            // Step 1: Load the H2 JDBC driver class into memory.
            Class.forName(JDBC_DRIVER);
            // Step 2: Establish the actual connection to the database file.
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            // Step 3: Create a Statement object for executing basic SQL.
            statement = connection.createStatement();

            // Step 4: Ensure all required tables exist or are created.
            createTables();
        } catch (ClassNotFoundException e) {
            // Why printStackTrace here? Indicates a setup/classpath problem, critical for developers.
            System.err.println("H2 JDBC Driver not found. Check classpath. Error: " + e.getMessage());
            e.printStackTrace();
            // Optional: Could re-throw as a RuntimeException if the app cannot function without the DB.
            throw new SQLException("H2 Driver not found", e);
        }
    }

    /**
     * <p> Method: createTables() - Private Helper </p>
     * <p> Description: Creates all necessary tables (userDB, InvitationCodes, otpsTable, posts, replies,
     * post_read_status, reply_read_status) using 'CREATE TABLE IF NOT EXISTS'. This ensures the application
     * can start correctly whether it's the first run or a subsequent run. It also handles schema alterations
     * (adding columns) gracefully using try-catch blocks. </p>
     * @throws SQLException if a database access error occurs during table creation or alteration.
     */
    private void createTables() throws SQLException {
        // --- Original User Table --- (Added NOT NULL for username)
        String userTable = "CREATE TABLE IF NOT EXISTS userDB ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, " // This ID is crucial now for foreign keys
                + "userName VARCHAR(255) UNIQUE NOT NULL, " // Added NOT NULL - critical identifier
                + "password VARCHAR(255), "
                + "firstName VARCHAR(255), "
                + "middleName VARCHAR(255), "
                + "lastName VARCHAR (255), "
                + "preferredFirstName VARCHAR(255), "
                + "emailAddress VARCHAR(255), "
                + "adminRole BOOL DEFAULT FALSE, "      // Role flags
                + "newStudent BOOL DEFAULT FALSE, "
                + "newStaff BOOL DEFAULT FALSE)";
        statement.execute(userTable);

        // --- Original Invitation & OTP Tables (Unchanged functionally) ---
        String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
                 + "code VARCHAR(10) PRIMARY KEY, emailAddress VARCHAR(255), role VARCHAR(255))";
        statement.execute(invitationCodesTable);
        // Why try-catch here? ALTER TABLE fails if the column already exists. Safe for repeated runs.
        try { statement.execute("ALTER TABLE InvitationCodes ADD COLUMN expiresAt TIMESTAMP"); } catch (SQLException ignore) {}
        try { statement.execute("ALTER TABLE InvitationCodes ADD COLUMN usesRemaining INT DEFAULT 0"); } catch (SQLException ignore) {}
        // Why MERGE? Ensures this specific test code exists with updated expiry/uses each time app starts.
        try (PreparedStatement ps = connection.prepareStatement(
            "MERGE INTO InvitationCodes (code, emailAddress, role, expiresAt, usesRemaining) KEY(code) VALUES (?,?,?,?,?)")) {
            ps.setString(1, "CSE360A1"); ps.setString(2, null); ps.setString(3, "MEMBER");
            ps.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(60))); ps.setInt(5, 1);
            ps.executeUpdate();
        } catch (SQLException ignore) {} // Ignore if merge fails (e.g., concurrent access)

        String otpsTable = "CREATE TABLE IF NOT EXISTS otpsTable (username VARCHAR(255) PRIMARY KEY, otp VARCHAR(10))";
        statement.execute(otpsTable);

        // --- HW2 Revision: Posts Table (Added NOT NULL constraints, default timestamp) ---
        String postsTable = "CREATE TABLE IF NOT EXISTS posts ("
                + "postID INT AUTO_INCREMENT PRIMARY KEY, "
                + "title VARCHAR(255) NOT NULL, "                   // Title is required per validation rules
                + "authorUsername VARCHAR(255) NOT NULL, "          // Posts must have an author
                + "content CLOB, "                                  // Main post text (CLOB for large text)
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " // Default timestamp on insert
                + "threadName VARCHAR(255) DEFAULT 'General')";     // Thread category, defaults to "General" per user story
        statement.execute(postsTable);

        // --- HW2 Revision: Replies Table (Removed Cascade, added NOT NULL, default timestamp) ---
        String repliesTable = "CREATE TABLE IF NOT EXISTS replies ("
                + "replyID INT AUTO_INCREMENT PRIMARY KEY, "
                + "postID INT, "                                    // Foreign key linking to the posts table
                + "authorUsername VARCHAR(255) NOT NULL, "          // Replies must have an author
                + "content CLOB, "                                  // Main reply text
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " // Default timestamp on insert
                // HW2 Revision: Removed 'ON DELETE CASCADE' based on user story requirement (#14).
                // Why? User story specifies replies should remain even if the original post is deleted.
                + "FOREIGN KEY (postID) REFERENCES posts(postID))"; // Link to posts table, NO cascade delete
        statement.execute(repliesTable);

        // --- HW2 Expanded Scope: New Tables for Read Status Tracking ---
        // Why separate tables? Normalizes the data - avoids storing read status per user in the post/reply tables.
        // Links users to posts they have read. Composite primary key prevents duplicates.
        String postReadStatusTable = "CREATE TABLE IF NOT EXISTS post_read_status ("
                + "userID INT NOT NULL, "                              // Foreign key to userDB.id
                + "postID INT NOT NULL, "                              // Foreign key to posts.postID
                + "PRIMARY KEY (userID, postID), "                     // Ensures a user can only read a post once
                + "FOREIGN KEY (userID) REFERENCES userDB(id) ON DELETE CASCADE, " // Link to user
                + "FOREIGN KEY (postID) REFERENCES posts(postID) ON DELETE CASCADE)"; // Link to post
                // Why CASCADE here? If a user or post is deleted, their read status records become irrelevant and should be cleaned up.
        statement.execute(postReadStatusTable);

        // Links users to replies they have read. Similar structure to post_read_status.
        String replyReadStatusTable = "CREATE TABLE IF NOT EXISTS reply_read_status ("
                + "userID INT NOT NULL, "                             // Foreign key to userDB.id
                + "replyID INT NOT NULL, "                            // Foreign key to replies.replyID
                + "PRIMARY KEY (userID, replyID), "                   // Composite key
                + "FOREIGN KEY (userID) REFERENCES userDB(id) ON DELETE CASCADE, " // Link to user
                + "FOREIGN KEY (replyID) REFERENCES replies(replyID) ON DELETE CASCADE)"; // Link to reply
                // Why CASCADE? Same reason as post_read_status.
        statement.execute(replyReadStatusTable);
        // --- End of HW2 Expanded Scope Changes ---
    }

    // --- Helper Method to Get User ID (Needed often for Read Status) ---
    /**
     * <p> Method: getUserID() </p>
     * <p> Description: Retrieves the integer primary key (ID) for a given username. Uses the internal
     * cache (currentUserID) if the requested username matches the cached username to avoid unnecessary
     * database queries. Otherwise, queries the userDB table. </p>
     * @param username The username whose ID is needed.
     * @return The integer user ID, or -1 if the username is not found or a database error occurs.
     */
     public int getUserID(String username) {
         // Why check cache? Performance optimization. Reduces DB hits if operating on the current user.
         if (username != null && username.equals(this.currentUsername) && this.currentUserID != -1) {
             return this.currentUserID;
         }

         // If not in cache or different user, query the database.
         String sql = "SELECT id FROM userDB WHERE userName = ?";
         try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setString(1, username);
             try (ResultSet rs = pstmt.executeQuery()) { // Ensure ResultSet is also closed
                 if (rs.next()) {
                     int foundID = rs.getInt("id");
                     // If this query was for the 'current' user, update the cache.
                     if (username != null && username.equals(this.currentUsername)) {
                         this.currentUserID = foundID;
                     }
                     return foundID; // Return the found ID.
                 }
             } // ResultSet closed here.
         } catch (SQLException e) {
             System.err.println("Error getting userID for username '" + username + "': " + e.getMessage());
             e.printStackTrace(); // Log error.
         }
         return -1; // Return -1 to indicate user not found or error.
     }

    // --- Original User Methods (from TP1 - with Javadoc/Error Handling/getUserAccountDetails Cache) ---

    /**
     * <p> Method: isDatabaseEmpty() </p>
     * <p> Description: Checks if the user table (userDB) contains any records. Used on startup
     * to determine if the first-time admin setup is needed. </p>
     * @return true if no user records exist in the userDB table, false otherwise.
     */
    public boolean isDatabaseEmpty() {
        String query = "SELECT COUNT(*) AS count FROM userDB";
        try (ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt("count") == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * <p> Method: getNumberOfUsers() </p>
     * <p> Description: Returns the total number of user accounts currently registered in the userDB table. </p>
     * @return The integer count of users. Returns 0 if the table is empty or if an SQL error occurs.
     */
    public int getNumberOfUsers() {
        String query = "SELECT COUNT(*) AS count FROM userDB";
        try (ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return 0;
    }

    /**
     * <p> Method: register() </p>
     * <p> Description: Inserts a new user record into the userDB table using the details
     * provided in the User object. It also updates the internal cache of the 'current' user details
     * to reflect the newly registered user. </p>
     * @param user The User object containing username, password, name details, email, and role flags.
     * @throws SQLException if a database access error occurs during the INSERT operation (e.g., duplicate username).
     */
    public void register(User user) throws SQLException {
        String insertUser = "INSERT INTO userDB (userName, password, firstName, middleName, "
                + "lastName, preferredFirstName, emailAddress, adminRole, newStudent, newStaff) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            // Update internal cache
            currentUsername = user.getUserName();
            currentPassword = user.getPassword();
            currentFirstName = user.getFirstName();
            currentMiddleName = user.getMiddleName();
            currentLastName = user.getLastName();
            currentPreferredFirstName = user.getPreferredFirstName();
            currentEmailAddress = user.getEmailAddress();
            currentAdminRole = user.getAdminRole();
            currentNewStudent = user.getNewStudent();
            currentNewStaff = user.getNewStaff();
            currentUserID = -1; // Invalidate ID cache, needs refresh via getUserID or getUserAccountDetails

            // Bind parameters
            pstmt.setString(1, currentUsername);
            pstmt.setString(2, currentPassword);
            pstmt.setString(3, currentFirstName);
            pstmt.setString(4, currentMiddleName);
            pstmt.setString(5, currentLastName);
            pstmt.setString(6, currentPreferredFirstName);
            pstmt.setString(7, currentEmailAddress);
            pstmt.setBoolean(8, currentAdminRole);
            pstmt.setBoolean(9, currentNewStudent);
            pstmt.setBoolean(10, currentNewStaff);

            pstmt.executeUpdate();
        }
    }

    /**
     * <p> Method: getUserList() </p>
     * <p> Description: Retrieves a list of all usernames currently in the userDB table,
     * ordered alphabetically, primarily for populating UI selection elements.
     * Includes a placeholder entry "<Select a User>" at the beginning. </p>
     * @return A List of String objects containing usernames. The first element is "<Select a User>".
     * Returns null if a database error occurs.
     */
    public List<String> getUserList () {
        List<String> userList = new ArrayList<>();
        userList.add("<Select a User>");
        String query = "SELECT userName FROM userDB ORDER BY userName ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                userList.add(rs.getString("userName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return userList;
    }

    /**
     * <p> Method: loginAdmin() </p>
     * <p> Description: Authenticates a user attempting to log in with admin privileges. </p>
     * @param user The User object containing the username and password attempt.
     * @return true if the credentials match an existing admin user, false otherwise or on error.
     */
    public boolean loginAdmin(User user){
        String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND adminRole = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            return pstmt.executeQuery().next();
        } catch  (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * <p> Method: loginStudent() </p>
     * <p> Description: Authenticates a user attempting to log in with student privileges. </p>
     * @param user The User object containing the username and password attempt.
     * @return true if the credentials match an existing student user, false otherwise or on error.
     */
    public boolean loginStudent(User user) {
        String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND newStudent = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            return pstmt.executeQuery().next();
        } catch  (SQLException e) {
               e.printStackTrace();
        }
        return false;
    }

    /**
     * <p> Method: loginStaff() </p>
     * <p> Description: Authenticates a user attempting to log in with staff privileges. </p>
     * @param user The User object containing the username and password attempt.
     * @return true if the credentials match an existing staff user, false otherwise or on error.
     */
    public boolean loginStaff(User user) {
        String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND newStaff = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            return pstmt.executeQuery().next();
        } catch  (SQLException e) {
               e.printStackTrace();
        }
        return false;
    }

    /**
     * <p> Method: doesUserExist() </p>
     * <p> Description: Checks quickly if a username is already taken in the userDB table. </p>
     * @param userName The username to check for existence.
     * @return true if a user with that username exists, false otherwise or if an error occurs.
     */
    public boolean doesUserExist(String userName) {
        String query = "SELECT COUNT(*) FROM userDB WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * <p> Method: getNumberOfRoles() </p>
     * <p> Description: Calculates the number of assigned roles (Admin, Student, Staff) for a given User object. </p>
     * @param user The User object whose roles are to be counted.
     * @return An integer representing the count of active roles (0, 1, 2, or 3).
     */
    public int getNumberOfRoles (User user) {
        int numberOfRoles = 0;
        if (user.getAdminRole()) numberOfRoles++;
        if (user.getNewStudent()) numberOfRoles++;
        if (user.getNewStaff()) numberOfRoles++;
        return numberOfRoles;
    }

    /**
     * <p> Method: generateInvitationCode() </p>
     * <p> Description: Generates a unique 6-character invitation code, stores it with email, role(s),
     * a 1-minute expiry, and a single-use limit. </p>
     * @param emailAddress The email address for the invitation.
     * @param role The role string (e.g., "Student", "Admin, Staff") granted on registration.
     * @return The 6-character code. Returns code even if DB insert fails (error logged).
     */
    public String generateInvitationCode(String emailAddress, String role) {
        String code = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String sql = "INSERT INTO InvitationCodes (code, emailAddress, role, expiresAt, usesRemaining) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, emailAddress);
            pstmt.setString(3, role);
            pstmt.setTimestamp(4, Timestamp.from(Instant.now().plusSeconds(60))); // 1 min expiry
            pstmt.setInt(5, 1); // one-time use
            pstmt.executeUpdate();
            System.out.println("[INVITE] Created code: " + code + " -> " + role + " for " + emailAddress);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * <p> Method: getNumberOfInvitations() </p>
     * <p> Description: Counts the total number of records in the InvitationCodes table. </p>
     * @return Integer count of all invitation codes. Returns 0 on error.
     */
    public int getNumberOfInvitations() {
        String query = "SELECT COUNT(*) AS count FROM InvitationCodes";
        try (ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * <p> Method: emailaddressHasBeenUsed() </p>
     * <p> Description: Checks if an *active* invitation code exists for the given email. </p>
     * @param emailAddress The email address to check.
     * @return true if an active invitation exists, false otherwise or on error.
     */
    public boolean emailaddressHasBeenUsed(String emailAddress) {
        String query = "SELECT COUNT(*) AS count FROM InvitationCodes WHERE emailAddress = ? AND expiresAt > CURRENT_TIMESTAMP AND usesRemaining > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, emailAddress);
            ResultSet rs = pstmt.executeQuery();
            // System.out.println(rs); // Original debug print
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * <p> Method: getRoleGivenAnInvitationCode() </p>
     * <p> Description: Retrieves the role string for a code if it's valid, active, and usable. </p>
     * @param code The 6-character invitation code.
     * @return Role string if valid, "" otherwise.
     */
    public String getRoleGivenAnInvitationCode(String code) {
        String sql = "SELECT role FROM InvitationCodes WHERE code = ? AND expiresAt > CURRENT_TIMESTAMP AND usesRemaining > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code == null ? "" : code.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    /**
     * <p> Method: getEmailAddressUsingCode() </p>
     * <p> Description: Retrieves the email address for a code if it's valid, active, and usable. </p>
     * @param code The 6-character invitation code.
     * @return Email address string if valid, "" otherwise.
     */
    public String getEmailAddressUsingCode(String code) {
        String sql = "SELECT emailAddress FROM InvitationCodes WHERE code = ? AND expiresAt > CURRENT_TIMESTAMP AND usesRemaining > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, code == null ? "" : code.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("emailAddress");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    /**
     * <p> Method: removeInvitationAfterUse() </p>
     * <p> Description: Deletes an invitation code record after successful registration. </p>
     * @param code The 6-character code to delete.
     */
    public void removeInvitationAfterUse(String code) {
        String deleteQuery = "DELETE FROM InvitationCodes WHERE code = ?";
        try (PreparedStatement pstmtDelete = connection.prepareStatement(deleteQuery)) {
            pstmtDelete.setString(1, code == null ? "" : code.trim());
            int rowsAffected = pstmtDelete.executeUpdate();
            if (rowsAffected > 0) System.out.println("[INVITE] Consumed code: " + code);
        } catch (SQLException eDelete) {
            eDelete.printStackTrace();
        }
    }

    /**
     * <p> Method: updatePassword() </p>
     * <p> Description: Updates the password for a given username. Updates cache if applicable. </p>
     * @param username The username of the account.
     * @param newPassword The new password value.
     */
    public void updatePassword(String username, String newPassword) {
        String query = "UPDATE userDB SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            if (username != null && username.equals(this.currentUsername)) {
                 currentPassword = newPassword;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> Method: generateOTPCode() </p>
     * <p> Description: Generates and stores a 6-character OTP for a username (overwrites existing). </p>
     * @param username The username for the OTP.
     * @return The 6-character OTP string.
     */
    public String generateOTPCode(String username) {
        String otp = UUID.randomUUID().toString().substring(0, 6);
        String query = "MERGE INTO otpsTable (username, otp) KEY(username) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, otp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Generated OTP for " + username + ": " + otp); // Use println for standard log
        return otp;
    }

    /**
     * <p> Method: otpHasBeenUsed() </p>
     * <p> Description: Validates OTP against stored value. Deletes record if valid (consumes OTP). </p>
     * @param username The username using the OTP.
     * @param otp The 6-character OTP provided.
     * @return true if OTP was valid and consumed, false otherwise.
     */
    public boolean otpHasBeenUsed(String username, String otp) {
        String query = "SELECT otp FROM otpsTable WHERE username = ?";
        String remove = "DELETE FROM otpsTable WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            // System.out.println(rs); // Original debug print
            if (rs.next()) {
                String storedOtp = rs.getString("otp");
                if (storedOtp != null && storedOtp.equals(otp)) {
                    try (PreparedStatement del = connection.prepareStatement(remove)) {
                        del.setString(1, username);
                        del.executeUpdate();
                    }
                    System.out.println("OTP validated and consumed for user: " + username);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("OTP validation failed for user: " + username);
        return false;
    }

    // --- Original Getters/Setters/Updaters for User Details (with Javadoc) ---

    /** Retrieves the first name for a given username. */
    public String getFirstName(String username) {
        String query = "SELECT firstName FROM userDB WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("firstName");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates the first name for the specified user. */
    public void updateFirstName(String username, String firstName) {
        String query = "UPDATE userDB SET firstName = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
             if (username != null && username.equals(this.currentUsername)) currentFirstName = firstName;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Retrieves the middle name for a given username. */
    public String getMiddleName(String username) {
        String query = "SELECT middleName FROM userDB WHERE userName = ?"; // Corrected column name
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("middleName");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates the middle name for the specified user. */
    public void updateMiddleName(String username, String middleName) {
        String query = "UPDATE userDB SET middleName = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, middleName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            if (username != null && username.equals(this.currentUsername)) currentMiddleName = middleName;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Retrieves the last name for a given username. */
    public String getLastName(String username) {
        String query = "SELECT lastName FROM userDB WHERE userName = ?"; // Corrected column name
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("lastName");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates the last name for the specified user. */
    public void updateLastName(String username, String lastName) {
        String query = "UPDATE userDB SET lastName = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, lastName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
             if (username != null && username.equals(this.currentUsername)) currentLastName = lastName;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Retrieves the preferred first name for a given username. */
    public String getPreferredFirstName(String username) {
        String query = "SELECT preferredFirstName FROM userDB WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("preferredFirstName"); // Corrected column name usage
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates the preferred first name for the specified user. */
    public void updatePreferredFirstName(String username, String preferredFirstName) {
        String query = "UPDATE userDB SET preferredFirstName = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, preferredFirstName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            if (username != null && username.equals(this.currentUsername)) currentPreferredFirstName = preferredFirstName;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Retrieves the email address for a given username. */
    public String getEmailAddress(String username) {
        String query = "SELECT emailAddress FROM userDB WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("emailAddress");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates the email address for the specified user. */
    public void updateEmailAddress(String username, String emailAddress) {
        String query = "UPDATE userDB SET emailAddress = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, emailAddress);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
             if (username != null && username.equals(this.currentUsername)) currentEmailAddress = emailAddress;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Fetches all account details and updates the internal cache including userID. */
    public boolean getUserAccountDetails(String username) {
        if (username == null || username.trim().isEmpty() || username.equals("<Select a User>")) {
             currentUserID = -1; currentUsername = null; /* reset other fields */ return false;
        }
        String query = "SELECT * FROM userDB WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUserID = rs.getInt("id"); // Cache the ID
                currentUsername = rs.getString("userName");
                currentPassword = rs.getString("password");
                currentFirstName = rs.getString("firstName");
                currentMiddleName = rs.getString("middleName");
                currentLastName = rs.getString("lastName");
                currentPreferredFirstName = rs.getString("preferredFirstName");
                currentEmailAddress = rs.getString("emailAddress");
                currentAdminRole = rs.getBoolean("adminRole");
                currentNewStudent = rs.getBoolean("newStudent");
                currentNewStaff = rs.getBoolean("newStaff");
                System.out.println("Cached details for user: " + currentUsername + " (ID: " + currentUserID + ")");
                return true;
            } else {
                 currentUserID = -1; currentUsername = null; /* reset other fields */ return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
             currentUserID = -1; currentUsername = null; /* reset other fields */ return false;
        }
    }

    /** Updates a specific role flag for the specified user. */
    public boolean updateUserRole(String username, String role, String value) {
        String roleColumn;
        boolean booleanValue = Boolean.parseBoolean(value);
        if (role.equalsIgnoreCase("Admin")) roleColumn = "adminRole";
        else if (role.equalsIgnoreCase("Student")) roleColumn = "newStudent";
        else if (role.equalsIgnoreCase("Staff")) roleColumn = "newStaff";
        else { System.err.println("Invalid role: " + role); return false; }

        String query = "UPDATE userDB SET " + roleColumn + " = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setBoolean(1, booleanValue);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
             if (username != null && username.equals(this.currentUsername)) {
                 if (roleColumn.equals("adminRole")) currentAdminRole = booleanValue;
                 else if (roleColumn.equals("newStudent")) currentNewStudent = booleanValue;
                 else if (roleColumn.equals("newStaff")) currentNewStaff = booleanValue;
             }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Original Getters for Cached User Details ---
    public String getCurrentUsername() { return currentUsername;}
    public String getCurrentPassword() { return currentPassword;}
    public String getCurrentFirstName() { return currentFirstName;}
    public String getCurrentMiddleName() { return currentMiddleName;}
    public String getCurrentLastName() { return currentLastName;}
    public String getCurrentPreferredFirstName() { return currentPreferredFirstName;}
    public String getCurrentEmailAddress() { return currentEmailAddress;}
    public boolean getCurrentAdminRole() { return currentAdminRole;}
    public boolean getCurrentNewStudent() { return currentNewStudent;}
    public boolean getCurrentNewStaff() { return currentNewStaff;}
    // HW2 Expanded: Getter for cached user ID
    public int getCurrentUserID() { return currentUserID; }


    /** Removes a user record from the userDB table. */
    public void deleteUser(String username) {
        String query = "DELETE FROM userDB WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
             System.out.println("Deleted user: " + username);
        } catch (SQLException e) {
            System.err.println("Error deleting user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Dumps the userDB table content to the console for debugging. */
    public void dump() throws SQLException {
        System.out.println("\n--- Dumping userDB Table ---");
        String query = "SELECT * FROM userDB";
        try (Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query)) {
            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();
            for (int i = 1; i <= columnCount; i++) System.out.printf("%-20s | ", meta.getColumnLabel(i));
            System.out.println("\n" + "-".repeat(columnCount * 23)); // Dynamic separator line
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) System.out.printf("%-20s | ", resultSet.getString(i));
                System.out.println();
            }
        }
         System.out.println("--- End of userDB Table Dump ---\n");
    }

    /** Closes the database statement and connection. */
    public void closeConnection() {
        try {
            if(statement!=null && !statement.isClosed()) { statement.close(); System.out.println("DB statement closed."); }
        } catch(SQLException se2) { se2.printStackTrace(); }
        try {
            if(connection!=null && !connection.isClosed()) { connection.close(); System.out.println("DB connection closed."); }
        } catch(SQLException se){ se.printStackTrace(); }
    }

    /** Retrieves all users formatted for display in lists/tables. */
    public List<UserForList> getAllUsersForList() {
        List<UserForList> userList = new ArrayList<>();
        String query = "SELECT userName, firstName, middleName, lastName, emailAddress, adminRole, newStudent, newStaff FROM userDB ORDER BY userName ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("userName");
                String first = rs.getString("firstName");
                String middle = rs.getString("middleName");
                String last = rs.getString("lastName");
                String name = first + (middle == null || middle.trim().isEmpty() ? "" : " " + middle) + (last == null || last.trim().isEmpty() ? "" : " " + last);
                String email = rs.getString("emailAddress");
                StringBuilder rolesBuilder = new StringBuilder();
                if (rs.getBoolean("adminRole")) rolesBuilder.append("Admin ");
                if (rs.getBoolean("newStudent")) rolesBuilder.append("Student ");
                if (rs.getBoolean("newStaff")) rolesBuilder.append("Staff ");
                String roles = rolesBuilder.toString().trim();
                userList.add(new UserForList(username, name.trim(), email, roles));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    // --- HW2 Expanded Scope: Post/Reply CRUD Methods (Simpler versions + WithInfo versions) ---

    /** Original createPost - updated for threadName & default timestamp */
    public void createPost(Post post) {
        String sql = "INSERT INTO posts (title, authorUsername, content, threadName) VALUES (?, ?, ?, ?)"; // Timestamp uses DB default
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getAuthorUsername());
            pstmt.setString(3, post.getContent());
            pstmt.setString(4, post.getThreadName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
             System.err.println("Error creating post: " + e.getMessage());
             e.printStackTrace();
         }
    }

    /** Original getAllPosts - less info than getAllPostsWithInfo */
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts ORDER BY timestamp DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                posts.add(new Post(
                        rs.getInt("postID"), rs.getString("title"), rs.getString("authorUsername"),
                        rs.getString("content"), rs.getTimestamp("timestamp"), rs.getString("threadName")
                ));
            }
        } catch (SQLException e) {
             System.err.println("Error retrieving all posts: " + e.getMessage());
             e.printStackTrace();
         }
        return posts;
    }

    /** Original updatePost */
    public void updatePost(Post post) {
        String sql = "UPDATE posts SET title = ?, content = ? WHERE postID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getContent());
            pstmt.setInt(3, post.getPostID());
            pstmt.executeUpdate();
        } catch (SQLException e) {
             System.err.println("Error updating post " + post.getPostID() + ": " + e.getMessage());
             e.printStackTrace();
         }
    }

    /** Original deletePost */
    public void deletePost(int postID) {
        String sql = "DELETE FROM posts WHERE postID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, postID);
            pstmt.executeUpdate();
             System.out.println("Deleted post with ID: " + postID);
        } catch (SQLException e) {
             System.err.println("Error deleting post " + postID + ": " + e.getMessage());
             e.printStackTrace();
         }
    }

    /** Original getPost helper */
     public Post getPost(int postID) {
         String sql = "SELECT * FROM posts WHERE postID = ?";
         try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setInt(1, postID);
             ResultSet rs = pstmt.executeQuery();
             if (rs.next()) {
                 return new Post(
                         rs.getInt("postID"), rs.getString("title"), rs.getString("authorUsername"),
                         rs.getString("content"), rs.getTimestamp("timestamp"), rs.getString("threadName")
                 );
             }
         } catch (SQLException e) {
              System.err.println("Error retrieving post " + postID + ": " + e.getMessage());
              e.printStackTrace();
          }
         return null;
     }

    /** Original createReply - uses default timestamp */
    public void createReply(Reply reply) {
        String sql = "INSERT INTO replies (postID, authorUsername, content) VALUES (?, ?, ?)"; // Timestamp uses DB default
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, reply.getPostID());
            pstmt.setString(2, reply.getAuthorUsername());
            pstmt.setString(3, reply.getContent());
            pstmt.executeUpdate();
        } catch (SQLException e) {
             System.err.println("Error creating reply for post " + reply.getPostID() + ": " + e.getMessage());
             e.printStackTrace();
         }
    }

    /** Original getRepliesForPost - less info, no filtering */
    public List<Reply> getRepliesForPost(int postID) {
        List<Reply> replies = new ArrayList<>();
        String sql = "SELECT * FROM replies WHERE postID = ? ORDER BY timestamp ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, postID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                replies.add(new Reply(
                        rs.getInt("replyID"), rs.getInt("postID"), rs.getString("authorUsername"),
                        rs.getString("content"), rs.getTimestamp("timestamp")
                ));
            }
        } catch (SQLException e) {
             System.err.println("Error getting replies for post " + postID + ": " + e.getMessage());
             e.printStackTrace();
         }
        return replies;
    }

    /** Original updateReply */
    public void updateReply(Reply reply) {
        String sql = "UPDATE replies SET content = ? WHERE replyID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reply.getContent());
            pstmt.setInt(2, reply.getReplyID());
            pstmt.executeUpdate();
        } catch (SQLException e) {
             System.err.println("Error updating reply " + reply.getReplyID() + ": " + e.getMessage());
             e.printStackTrace();
         }
    }

    /** Original deleteReply */
    public void deleteReply(int replyID) {
        String sql = "DELETE FROM replies WHERE replyID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, replyID);
            pstmt.executeUpdate();
             System.out.println("Deleted reply with ID: " + replyID);
        } catch (SQLException e) {
             System.err.println("Error deleting reply " + replyID + ": " + e.getMessage());
             e.printStackTrace();
         }
    }

    // --- HW2 Expanded Scope: New DTO-returning Methods ---
    // (These are the primary methods the Controller will use for displaying data)
    

    /**
     * <p> Method: markPostAsRead() </p>
     * <p> Description: Records that a specific user has read a specific post by inserting (or merging)
     * a record into the post_read_status table. Ignores errors if the record already exists. </p>
     * @param postID The ID of the post that was read.
     * @param userID The ID of the user who read the post. If -1, the method returns without action.
     */
     public void markPostAsRead(int postID, int userID) { // <-- Must be public
         if (userID == -1) {
             System.err.println("Attempted to mark post read with invalid userID (-1).");
             return;
         }
         String sql = "MERGE INTO post_read_status (userID, postID) KEY(userID, postID) VALUES (?, ?)";
         try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setInt(1, userID);
             pstmt.setInt(2, postID);
             pstmt.executeUpdate();
         } catch (SQLException e) {
             if (!e.getSQLState().equals("23505")) { // 23505 is unique constraint violation
                  System.err.println("Error marking post " + postID + " as read for user " + userID + ": " + e.getMessage());
                 e.printStackTrace();
             }
         }
     }

    /**
     * <p> Method: markReplyAsRead() </p>
     * <p> Description: Records that a specific user has read a specific reply by inserting/merging
     * a record into the reply_read_status table. Handles potential existing records gracefully. </p>
     * @param replyID The ID of the reply that was read.
     * @param userID The ID of the user who read the reply. If -1, the method returns without action.
     */
     public void markReplyAsRead(int replyID, int userID) { // <-- Must be public
         if (userID == -1) {
             System.err.println("Attempted to mark reply read with invalid userID (-1).");
             return;
         }
         String sql = "MERGE INTO reply_read_status (userID, replyID) KEY(userID, replyID) VALUES (?, ?)";
         try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setInt(1, userID);
             pstmt.setInt(2, replyID);
             pstmt.executeUpdate();
         } catch (SQLException e) {
              if (!e.getSQLState().equals("23505")) {
                  System.err.println("Error marking reply " + replyID + " as read for user " + userID + ": " + e.getMessage());
                 e.printStackTrace();
              }
         }
     }

    /** Retrieves all posts with counts and read status for the viewing user. */
    public List<PostInfo> getAllPostsWithInfo(int viewingUserID) {
        List<PostInfo> postsInfo = new ArrayList<>();
        if (viewingUserID == -1) return postsInfo;
        String sql = "SELECT p.*, " +
                     " COALESCE(rc.reply_count, 0) AS totalReplies, " +
                     " COALESCE(urc.unread_reply_count, 0) AS unreadReplies, " +
                     " CASE WHEN prs.userID IS NOT NULL THEN TRUE ELSE FALSE END AS isRead " +
                     "FROM posts p " +
                     "LEFT JOIN (SELECT postID, COUNT(*) AS reply_count FROM replies GROUP BY postID) rc ON p.postID = rc.postID " +
                     "LEFT JOIN (SELECT r.postID, COUNT(r.replyID) AS unread_reply_count " +
                     "           FROM replies r LEFT JOIN reply_read_status rrs ON r.replyID = rrs.replyID AND rrs.userID = ? " +
                     "           WHERE rrs.userID IS NULL GROUP BY r.postID) urc ON p.postID = urc.postID " +
                     "LEFT JOIN post_read_status prs ON p.postID = prs.postID AND prs.userID = ? " +
                     "ORDER BY p.timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, viewingUserID);
            pstmt.setInt(2, viewingUserID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Post post = new Post(rs.getInt("postID"), rs.getString("title"), rs.getString("authorUsername"), rs.getString("content"), rs.getTimestamp("timestamp"), rs.getString("threadName"));
                PostInfo info = new PostInfo(post);
                info.setReplyCount(rs.getInt("totalReplies"));
                info.setUnreadReplyCount(rs.getInt("unreadReplies"));
                info.setRead(rs.getBoolean("isRead"));
                postsInfo.add(info);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return postsInfo;
    }

    /** Searches posts with keyword/thread, returning DTOs with counts and read status. */
    public List<PostInfo> searchPostsWithInfo(String keyword, String threadName, int viewingUserID) {
         List<PostInfo> postsInfo = new ArrayList<>();
         if (viewingUserID == -1) return postsInfo;
         boolean filterByThread = (threadName != null && !threadName.trim().isEmpty() && !threadName.equalsIgnoreCase("All"));
         String searchPattern = "%" + keyword.toUpperCase() + "%";
         String sqlBase = "SELECT p.*, COALESCE(rc.reply_count, 0) AS totalReplies, COALESCE(urc.unread_reply_count, 0) AS unreadReplies, CASE WHEN prs.userID IS NOT NULL THEN TRUE ELSE FALSE END AS isRead " +
                         "FROM posts p " +
                         "LEFT JOIN (SELECT postID, COUNT(*) AS reply_count FROM replies GROUP BY postID) rc ON p.postID = rc.postID " +
                         "LEFT JOIN (SELECT r.postID, COUNT(r.replyID) AS unread_reply_count FROM replies r LEFT JOIN reply_read_status rrs ON r.replyID = rrs.replyID AND rrs.userID = ? WHERE rrs.userID IS NULL GROUP BY r.postID) urc ON p.postID = urc.postID " +
                         "LEFT JOIN post_read_status prs ON p.postID = prs.postID AND prs.userID = ? ";
        String sqlWhere = filterByThread ? "WHERE (UPPER(p.title) LIKE ? OR UPPER(p.content) LIKE ?) AND UPPER(p.threadName) = ? " : "WHERE (UPPER(p.title) LIKE ? OR UPPER(p.content) LIKE ?) ";
        String sqlOrder = "ORDER BY p.timestamp DESC";
        String sql = sqlBase + sqlWhere + sqlOrder;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setInt(paramIndex++, viewingUserID);
            pstmt.setInt(paramIndex++, viewingUserID);
            pstmt.setString(paramIndex++, searchPattern);
            pstmt.setString(paramIndex++, searchPattern);
            if (filterByThread) pstmt.setString(paramIndex++, threadName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                 Post post = new Post(rs.getInt("postID"), rs.getString("title"), rs.getString("authorUsername"), rs.getString("content"), rs.getTimestamp("timestamp"), rs.getString("threadName"));
                 PostInfo info = new PostInfo(post);
                 info.setReplyCount(rs.getInt("totalReplies"));
                 info.setUnreadReplyCount(rs.getInt("unreadReplies"));
                 info.setRead(rs.getBoolean("isRead"));
                 postsInfo.add(info);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return postsInfo;
    }

    /** Retrieves posts by a specific author, returning DTOs with counts and read status. */
    public List<PostInfo> getPostsByAuthorWithInfo(String authorUsername, int viewingUserID) {
         List<PostInfo> postsInfo = new ArrayList<>();
         if (viewingUserID == -1 || authorUsername == null || authorUsername.trim().isEmpty()) return postsInfo;
         String sql = "SELECT p.*, COALESCE(rc.reply_count, 0) AS totalReplies, COALESCE(urc.unread_reply_count, 0) AS unreadReplies, CASE WHEN prs.userID IS NOT NULL THEN TRUE ELSE FALSE END AS isRead " +
                      "FROM posts p " +
                      "LEFT JOIN (SELECT postID, COUNT(*) AS reply_count FROM replies GROUP BY postID) rc ON p.postID = rc.postID " +
                      "LEFT JOIN (SELECT r.postID, COUNT(r.replyID) AS unread_reply_count FROM replies r LEFT JOIN reply_read_status rrs ON r.replyID = rrs.replyID AND rrs.userID = ? WHERE rrs.userID IS NULL GROUP BY r.postID) urc ON p.postID = urc.postID " +
                      "LEFT JOIN post_read_status prs ON p.postID = prs.postID AND prs.userID = ? " +
                      "WHERE p.authorUsername = ? ORDER BY p.timestamp DESC";
         try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setInt(1, viewingUserID);
             pstmt.setInt(2, viewingUserID);
             pstmt.setString(3, authorUsername);
             ResultSet rs = pstmt.executeQuery();
             while (rs.next()) {
                  Post post = new Post(rs.getInt("postID"), rs.getString("title"), rs.getString("authorUsername"), rs.getString("content"), rs.getTimestamp("timestamp"), rs.getString("threadName"));
                  PostInfo info = new PostInfo(post);
                  info.setReplyCount(rs.getInt("totalReplies"));
                  info.setUnreadReplyCount(rs.getInt("unreadReplies"));
                  info.setRead(rs.getBoolean("isRead"));
                  postsInfo.add(info);
             }
         } catch (SQLException e) { e.printStackTrace(); }
         return postsInfo;
    }

    /** Retrieves replies for a post, returning DTOs with read status, optionally filtering unread. */
    public List<ReplyInfo> getRepliesForPostWithInfo(int postID, int viewingUserID, boolean unreadOnly) {
        List<ReplyInfo> repliesInfo = new ArrayList<>();
        if (viewingUserID == -1) return repliesInfo;
        String sqlBase = "SELECT r.*, CASE WHEN rrs.userID IS NOT NULL THEN TRUE ELSE FALSE END AS isRead FROM replies r LEFT JOIN reply_read_status rrs ON r.replyID = rrs.replyID AND rrs.userID = ? ";
        String sqlWhere = "WHERE r.postID = ? ";
        String sqlFilter = unreadOnly ? "AND rrs.userID IS NULL " : "";
        String sqlOrder = "ORDER BY r.timestamp ASC";
        String sql = sqlBase + sqlWhere + sqlFilter + sqlOrder;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, viewingUserID);
            pstmt.setInt(2, postID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Reply reply = new Reply(rs.getInt("replyID"), rs.getInt("postID"), rs.getString("authorUsername"), rs.getString("content"), rs.getTimestamp("timestamp"));
                ReplyInfo info = new ReplyInfo(reply);
                info.setRead(rs.getBoolean("isRead"));
                repliesInfo.add(info);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return repliesInfo;
    }

} // End of Database Class