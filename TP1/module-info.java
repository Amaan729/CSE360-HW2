module FoundationsF25 { // Your module name is FoundationsF25, based on the error
	requires javafx.controls;
	requires java.sql;
	requires javafx.base; // Make sure this is present

	// Original 'opens' directives from your project
	opens applicationMain to javafx.graphics, javafx.fxml;
	opens entityClasses to javafx.base; // Good to have this for the 'post.title' access
	
	// --- THIS IS THE FIX ---
	// This line allows the PropertyValueFactory (in javafx.base)
	// to use reflection to access your PostInfo/ReplyInfo DTOs.
	opens dto to javafx.base; 
	// --- End of Fix ---
}