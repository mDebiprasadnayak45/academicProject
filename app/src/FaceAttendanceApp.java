import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main Application Entry Point
 */
public class FaceAttendanceApp extends Application {

        private Stage primaryStage;
        private Stage registrationStage;
        private Scene loginScene;
        private Scene dashboardScene;
        private String currentUserId;
        private String currentUserRole;
        private int consecutiveFailedLogins = 0;
        private Button forgotPasswordBtn;

        @Override
        public void start(Stage stage) {
                this.primaryStage = stage;
                primaryStage.setTitle("Face Recognition Attendance System");
                primaryStage.setWidth(1200);
                primaryStage.setHeight(700);

                // Initialize login scene
                loginScene = createLoginScene();
                showLoginScene();
                primaryStage.show();
                Platform.runLater(() -> primaryStage.centerOnScreen());
        }

        /**
         * Displays login scene in a stable centered layout.
         */
        private void showLoginScene() {
                closeRegistrationWindowIfOpen();
                primaryStage.setMaximized(false);
                primaryStage.setScene(loginScene);
                primaryStage.setWidth(1200);
                primaryStage.setHeight(700);
                Platform.runLater(() -> primaryStage.centerOnScreen());
        }

        private void closeRegistrationWindowIfOpen() {
                if (registrationStage != null) {
                        registrationStage.close();
                        registrationStage = null;
                }
        }

        /**
         * Create login scene
         */
        private Scene createLoginScene() {
                BorderPane root = new BorderPane();
                root.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 100%);");

                VBox centerBox = new VBox(20);
                centerBox.setPadding(new Insets(50));
                centerBox.setAlignment(Pos.CENTER);

                // Title with icon
                Label titleLabel = new Label("🔐 Face Recognition Attendance");
                titleLabel.setStyle(
                                "-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");

                Label subtitleLabel = new Label("Secure • Modern • Intelligent");
                subtitleLabel.setStyle(
                                "-fx-font-size: 14; -fx-text-fill: rgba(255,255,255,0.9); -fx-padding: 0 0 20 0;");

                // Login form with modern card design
                VBox loginForm = new VBox(15);
                loginForm.setPadding(new Insets(40));
                loginForm.setAlignment(Pos.CENTER);
                loginForm.setMaxWidth(450);
                loginForm.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

                Label emailLabel = new Label("📧 Email Address");
                emailLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                TextField emailField = new TextField();
                emailField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                emailField.setPromptText("Enter your email address");
                emailField.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal == null) {
                                return;
                        }
                        String normalized = ValidationUtils.normalizeEmail(newVal);
                        if (normalized != null && !normalized.equals(newVal)) {
                                emailField.setText(normalized);
                        }
                });
                emailField.focusedProperty().addListener((obs, oldVal, nowFocused) -> {
                        if (!nowFocused) {
                                emailField.setText(ValidationUtils.normalizeEmail(emailField.getText()));
                        }
                });

                Label passwordLabel = new Label("🔑 Password");
                passwordLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                PasswordField passwordField = new PasswordField();
                passwordField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                passwordField.setPromptText("Enter your password");

                // Visible text field for "Show password" toggle
                TextField passwordVisible = new TextField();
                passwordVisible.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                passwordVisible.setPromptText("Enter your password");
                // Keep both fields in sync
                passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
                // Start with visible field hidden
                passwordVisible.setManaged(false);
                passwordVisible.setVisible(false);

                CheckBox showPassword = new CheckBox("Show password");
                showPassword.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        passwordVisible.setManaged(newVal);
                        passwordVisible.setVisible(newVal);
                        passwordField.setManaged(!newVal);
                        passwordField.setVisible(!newVal);
                });

                // Buttons
                HBox buttonBox = new HBox(10);
                buttonBox.setStyle("-fx-alignment: center;");

                Button loginBtn = new Button("🚀 Login");
                loginBtn.setPrefWidth(130);
                loginBtn.setPrefHeight(45);
                loginBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #5568d3; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-scale-y: 1.05; -fx-scale-x: 1.05;"));
                loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));

                Button registerBtn = new Button("✨ Register");
                registerBtn.setPrefWidth(130);
                registerBtn.setPrefHeight(45);
                registerBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #0e877d; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-scale-y: 1.05; -fx-scale-x: 1.05;"));
                registerBtn.setOnMouseExited(e -> registerBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));

                Label captchaInfoLabel = new Label("Captcha verification is enabled for login");
                captchaInfoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

                forgotPasswordBtn = new Button("Forgot Password?");
                forgotPasswordBtn.setVisible(true);
                forgotPasswordBtn.setManaged(true);
                forgotPasswordBtn.setStyle(
                                "-fx-font-size: 12; -fx-background-color: transparent; -fx-text-fill: #667eea; -fx-underline: true; -fx-cursor: hand;");
                forgotPasswordBtn.setOnAction(e -> showForgotPasswordDialog());

                loginBtn.setOnAction(e -> handleLogin(emailField.getText(), passwordField.getText()));
                registerBtn.setOnAction(e -> showRegistrationWindow());

                loginForm.getChildren().addAll(
                                emailLabel, emailField,
                                passwordLabel, passwordField, passwordVisible, showPassword,
                                captchaInfoLabel,
                                forgotPasswordBtn,
                                buttonBox);
                buttonBox.getChildren().addAll(loginBtn, registerBtn);

                centerBox.getChildren().addAll(titleLabel, subtitleLabel, loginForm);
                root.setCenter(centerBox);

                // Footer with icons
                Label footerLabel = new Label("🔒 Secure Login • 👤 Face Recognition • 🗄️ Encrypted Database");
                footerLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11; -fx-font-weight: 500;");
                VBox footerBox = new VBox(footerLabel);
                footerBox.setStyle("-fx-alignment: center; -fx-padding: 15;");
                root.setBottom(footerBox);

                return new Scene(root);
        }

        /**
         * Handle login
         */
        private void handleLogin(String email, String password) {
                email = ValidationUtils.normalizeEmail(email);
                if (email.isEmpty() || password.isEmpty()) {
                        showAlert("Error", "Please enter email and password");
                        AppLogger.warning("Login attempt with empty credentials");
                        return;
                }

                System.out.println("\n[INFO] LOGIN ATTEMPT");
                System.out.println("-----------------------------------------------------");
                System.out.println("[INFO] Email: " + email);
                AppLogger.info("Login attempt for user: " + email);

                Map<String, Object> result = UserManager.authenticateUser(email, password);

                if ((boolean) result.getOrDefault("success", false)) {
                        currentUserId = (String) result.get("userId");
                        currentUserRole = (String) result.get("role");
                        consecutiveFailedLogins = 0;

                        System.out.println("[OK] Authentication successful");
                        System.out.println("[OK] User ID: " + currentUserId);
                        System.out.println("[OK] Role: " + currentUserRole);
                        AppLogger.info("Login successful - User: " + currentUserId + ", Role: " + currentUserRole);

                        showCaptchaDialog();
                } else {
                        consecutiveFailedLogins++;
                        System.out.println("[ERROR] Authentication failed");
                        showAlert("Login Failed", "Invalid email or password");
                        AppLogger.warning("Login failed for user: " + email);
                }
        }

        /**
         * Show captcha verification dialog.
         */
        private void showCaptchaDialog() {
                String captcha = SecurityUtils.generateCaptcha(6);
                Dialog<Boolean> dialog = new Dialog<>();
                dialog.setTitle("Captcha Verification");
                dialog.setHeaderText("Enter captcha to continue: " + captcha);

                VBox content = new VBox(10);
                content.setPadding(new Insets(20));

                TextField captchaField = new TextField();
                captchaField.setPromptText("Enter captcha");
                captchaField.setMaxWidth(220);

                content.getChildren().addAll(
                                new Label("Captcha:"),
                                captchaField);

                dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == ButtonType.OK) {
                                String enteredCaptcha = captchaField.getText() == null ? ""
                                                : captchaField.getText().trim();
                                if (captcha.equalsIgnoreCase(enteredCaptcha)) {
                                        return true;
                                } else {
                                        showAlert("Error", "Invalid captcha");
                                        return false;
                                }
                        }
                        return false;
                });

                if (dialog.showAndWait().orElse(false)) {
                        openDashboard();
                }
        }

        /**
         * Forgot password flow using security question + answer.
         */
        private void showForgotPasswordDialog() {
                showForgotPasswordDialog(null, false);
        }

        private void showForgotPasswordDialogForCurrentUser() {
                if (currentUserId == null || currentUserId.isBlank()) {
                        showForgotPasswordDialog();
                        return;
                }
                showForgotPasswordDialog(currentUserId, true);
        }

        private void showForgotPasswordDialog(String prefilledUserId, boolean lockUserId) {
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Reset Password");
                dialog.setHeaderText("Reset password using your User ID and security question");

                VBox content = new VBox(10);
                content.setPadding(new Insets(18));

                TextField userIdField = new TextField();
                userIdField.setPromptText("Registered User ID (e.g., 23ITM001)");
                if (prefilledUserId != null && !prefilledUserId.isBlank()) {
                        userIdField.setText(prefilledUserId.trim());
                }
                userIdField.setDisable(lockUserId);

                Label questionLabel = new Label("Security question will appear after User ID lookup");
                questionLabel.setWrapText(true);

                java.util.concurrent.atomic.AtomicBoolean questionLoaded = new java.util.concurrent.atomic.AtomicBoolean(
                                false);

                TextField answerField = new TextField();
                answerField.setPromptText("Security answer");

                PasswordField newPasswordField = new PasswordField();
                newPasswordField.setPromptText("New password");

                java.util.concurrent.atomic.AtomicReference<String> resetCaptcha = new java.util.concurrent.atomic.AtomicReference<>(
                                SecurityUtils.generateCaptcha(6));
                Label captchaValueLabel = new Label(resetCaptcha.get());
                captchaValueLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                TextField captchaField = new TextField();
                captchaField.setPromptText("Enter captcha shown above");

                Button refreshCaptchaBtn = new Button("Refresh Captcha");
                refreshCaptchaBtn.setOnAction(e -> {
                        String nextCaptcha = SecurityUtils.generateCaptcha(6);
                        resetCaptcha.set(nextCaptcha);
                        captchaValueLabel.setText(nextCaptcha);
                        captchaField.clear();
                });

                Button lookupBtn = new Button("Load Security Question");
                lookupBtn.setOnAction(e -> {
                        String userId = userIdField.getText() == null ? "" : userIdField.getText().trim();
                        String question = UserManager.getSecurityQuestionByUserId(userId);
                        if (question == null || question.isBlank()) {
                                questionLoaded.set(false);
                                questionLabel.setText("No security question found for this User ID.");
                        } else {
                                questionLoaded.set(true);
                                questionLabel.setText(question);
                        }
                });

                if (lockUserId) {
                        lookupBtn.fire();
                }

                content.getChildren().addAll(
                                new Label("User ID"), userIdField,
                                lookupBtn,
                                new Label("Security Question"), questionLabel,
                                new Label("Answer"), answerField,
                                new Label("New Password"), newPasswordField,
                                new Label("Captcha"), captchaValueLabel, captchaField, refreshCaptchaBtn);

                dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                dialog.setResultConverter(buttonType -> {
                        if (buttonType == ButtonType.OK) {
                                String userId = userIdField.getText() == null ? "" : userIdField.getText().trim();
                                String answer = answerField.getText();
                                String newPassword = newPasswordField.getText();
                                String enteredCaptcha = captchaField.getText() == null
                                                ? ""
                                                : captchaField.getText().trim();

                                if (userId.isEmpty()) {
                                        showAlert("Error", "Please enter User ID.");
                                        return ButtonType.CANCEL;
                                }

                                if (!questionLoaded.get()) {
                                        showAlert("Error", "Please load your security question first.");
                                        return ButtonType.CANCEL;
                                }

                                if (!resetCaptcha.get().equalsIgnoreCase(enteredCaptcha)) {
                                        showAlert("Error", "Invalid captcha. Please try again.");
                                        return ButtonType.CANCEL;
                                }

                                if (!ValidationUtils.isStrongPassword(newPassword)) {
                                        showAlert("Error",
                                                        "Password must be at least 8 characters with uppercase, lowercase, digit, and special character.");
                                        return ButtonType.CANCEL;
                                }

                                boolean reset = UserManager.resetPasswordWithSecurityAnswerByUserId(userId, answer,
                                                newPassword);
                                if (reset) {
                                        showAlert("Success", "Password reset successful.");
                                        consecutiveFailedLogins = 0;
                                        return ButtonType.OK;
                                }
                                showAlert("Error", "Password reset failed. Check User ID, question, and answer.");
                                return ButtonType.CANCEL;
                        }
                        return buttonType;
                });

                dialog.showAndWait();
        }

        /**
         * Open main dashboard
         */
        private void openDashboard() {
                closeRegistrationWindowIfOpen();
                BorderPane root = new BorderPane();

                // Create menu bar
                MenuBar menuBar = createMenuBar();
                root.setTop(menuBar);

                // Create main content based on user role
                if ("ADMIN".equals(currentUserRole)) {
                        ScrollPane adminScrollPane = new ScrollPane(createAdminDashboard());
                        adminScrollPane.setFitToWidth(true);
                        adminScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        root.setCenter(adminScrollPane);
                } else if ("TEACHER".equals(currentUserRole)) {
                        ScrollPane teacherScrollPane = new ScrollPane(createTeacherDashboard());
                        teacherScrollPane.setFitToWidth(true);
                        teacherScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        root.setCenter(teacherScrollPane);
                } else if ("EMPLOYEE".equalsIgnoreCase(currentUserRole)) {
                        ScrollPane employeeScrollPane = new ScrollPane(createEmployeeDashboard());
                        employeeScrollPane.setFitToWidth(true);
                        employeeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        root.setCenter(employeeScrollPane);
                } else {
                        ScrollPane userScrollPane = new ScrollPane(createUserDashboard());
                        userScrollPane.setFitToWidth(true);
                        userScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        root.setCenter(userScrollPane);
                }

                dashboardScene = new Scene(root, 1200, 700);
                primaryStage.setScene(dashboardScene);
        }

        /**
         * Create menu bar with role-based access
         */
        private MenuBar createMenuBar() {
                MenuBar menuBar = new MenuBar();
                int notificationCount = getNotificationCountForCurrentUser();

                Menu fileMenu = new Menu("File");
                MenuItem exitItem = new MenuItem("Exit");
                exitItem.setOnAction(e -> primaryStage.close());
                fileMenu.getItems().add(exitItem);

                Menu userMenu = new Menu("User");
                MenuItem logoutItem = new MenuItem("Logout");
                logoutItem.setOnAction(e -> {
                        currentUserId = null;
                        currentUserRole = null;
                        consecutiveFailedLogins = 0;
                        showLoginScene();
                });
                MenuItem profileItem = new MenuItem("My Profile" + (notificationCount > 0 ? " •" : ""));
                profileItem.setOnAction(e -> showUserProfile());
                MenuItem leaveHistoryItem = new MenuItem("My Leave History");
                leaveHistoryItem.setOnAction(e -> showUserLeaveHistory());
                MenuItem notificationsItem = new MenuItem(notificationCount > 0
                                ? "🔔 Notifications (" + notificationCount + ")"
                                : "Notifications");
                notificationsItem.setOnAction(e -> showNotificationsWindow());

                if (!"STUDENT".equalsIgnoreCase(currentUserRole)) {
                        MenuItem manageMyFaceItem = new MenuItem("🧑 Add/Update My Face");
                        manageMyFaceItem.setOnAction(e -> openFaceEnrollmentForCurrentUser());
                        userMenu.getItems().addAll(profileItem, leaveHistoryItem, notificationsItem, manageMyFaceItem,
                                        new SeparatorMenuItem(), logoutItem);
                } else {
                        userMenu.getItems().addAll(profileItem, leaveHistoryItem, notificationsItem,
                                        new SeparatorMenuItem(), logoutItem);
                }

                if ("ADMIN".equals(currentUserRole)) {
                        Menu adminMenu = new Menu("Admin");
                        MenuItem manageUsersItem = new MenuItem("Manage Users");
                        manageUsersItem.setOnAction(e -> showUserManagement());
                        MenuItem studentAttendanceItem = new MenuItem("View Student Attendance");
                        studentAttendanceItem
                                        .setOnAction(e -> showRoleAttendanceWindow("Students Attendance (All Time)",
                                                        "STUDENT", null));
                        MenuItem teacherAttendanceItem = new MenuItem("View Teacher Attendance");
                        teacherAttendanceItem
                                        .setOnAction(e -> showRoleAttendanceWindow("Teachers Attendance (All Time)",
                                                        "TEACHER", null));
                        MenuItem approveTeacherLeavesItem = new MenuItem("Approve Teacher Leaves");
                        approveTeacherLeavesItem
                                        .setOnAction(e -> showLeaveApprovalWindow("TEACHER", "Approve Teacher Leaves"));
                        MenuItem studentLeaveHistoryItem = new MenuItem("Student Leave History");
                        studentLeaveHistoryItem.setOnAction(e -> showStudentLeaveHistory());
                        MenuItem reportsItem = new MenuItem("Reports");
                        reportsItem.setOnAction(e -> showReports());
                        adminMenu.getItems().addAll(manageUsersItem, studentAttendanceItem, teacherAttendanceItem,
                                        approveTeacherLeavesItem, studentLeaveHistoryItem, reportsItem);
                        menuBar.getMenus().addAll(fileMenu, userMenu, adminMenu);
                } else if ("TEACHER".equals(currentUserRole)) {
                        Menu teacherMenu = new Menu("Teacher");
                        MenuItem studentAttendanceItem = new MenuItem("View Student Attendance");
                        studentAttendanceItem
                                        .setOnAction(e -> showRoleAttendanceWindow("Students Attendance (All Time)",
                                                        "STUDENT", null));
                        MenuItem myAttendanceItem = new MenuItem("My Attendance");
                        myAttendanceItem.setOnAction(e -> showUserAttendanceTable());
                        MenuItem markAttendanceItem = new MenuItem("Mark My Check-In");
                        markAttendanceItem.setOnAction(e -> markMyCheckIn());
                        MenuItem markCheckOutItem = new MenuItem("Mark My Check-Out");
                        markCheckOutItem.setOnAction(e -> markMyCheckOut());
                        MenuItem leavesItem = new MenuItem("Approve Student Leaves");
                        leavesItem.setOnAction(e -> showLeaveApprovalWindow("STUDENT", "Approve Student Leaves"));
                        MenuItem reportsItem = new MenuItem("Reports");
                        reportsItem.setOnAction(e -> showReports());
                        teacherMenu.getItems().addAll(studentAttendanceItem, myAttendanceItem, markAttendanceItem,
                                        markCheckOutItem,
                                        leavesItem, reportsItem);
                        menuBar.getMenus().addAll(fileMenu, userMenu, teacherMenu);
                } else {
                        menuBar.getMenus().addAll(fileMenu, userMenu);
                }

                return menuBar;
        }

        /**
         * Create user dashboard
         */
        private VBox createUserDashboard() {
                VBox dashboard = new VBox(25);
                dashboard.setPadding(new Insets(30));
                dashboard.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label welcomeLabel = new Label("👋 Welcome, " + currentUserId);
                welcomeLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label subLabel = new Label("Your Personal Dashboard");
                subLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #2c3e50;");

                HBox buttonBox = new HBox(20);
                buttonBox.setStyle("-fx-alignment: center; -fx-padding: 20 0 20 0;");

                Button startCameraBtn = new Button("📷 Start Camera");
                startCameraBtn.setPrefWidth(180);
                startCameraBtn.setPrefHeight(60);
                startCameraBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
                startCameraBtn.setOnMouseEntered(e -> startCameraBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #5568d3; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                startCameraBtn.setOnMouseExited(e -> startCameraBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
                startCameraBtn.setOnAction(e -> new CameraWindow(currentUserId));

                Button viewAttendanceBtn = new Button("📊 View Attendance");
                viewAttendanceBtn.setPrefWidth(180);
                viewAttendanceBtn.setPrefHeight(60);
                viewAttendanceBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
                viewAttendanceBtn.setOnMouseEntered(e -> viewAttendanceBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #0e877d; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                viewAttendanceBtn.setOnMouseExited(e -> viewAttendanceBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
                viewAttendanceBtn.setOnAction(e -> showUserAttendanceTable());

                Button requestLeaveBtn = new Button("📋 Request Leave");
                requestLeaveBtn.setPrefWidth(180);
                requestLeaveBtn.setPrefHeight(60);
                requestLeaveBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
                requestLeaveBtn.setOnMouseEntered(e -> requestLeaveBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #d35400; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                requestLeaveBtn.setOnMouseExited(e -> requestLeaveBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
                requestLeaveBtn.setOnAction(e -> showLeaveRequestWindow());

                Button checkOutBtn = new Button("⏱ Check Out");
                checkOutBtn.setPrefWidth(180);
                checkOutBtn.setPrefHeight(60);
                checkOutBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
                checkOutBtn.setOnMouseEntered(e -> checkOutBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #a93226; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                checkOutBtn.setOnMouseExited(e -> checkOutBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
                checkOutBtn.setOnAction(e -> markMyCheckOut());

                Button resetPasswordBtn = createDashboardPasswordResetButton(190, 60);

                buttonBox.getChildren().addAll(startCameraBtn, viewAttendanceBtn, requestLeaveBtn, checkOutBtn,
                                resetPasswordBtn);

                if (!"STUDENT".equalsIgnoreCase(currentUserRole)) {
                        Button manageFaceBtn = new Button("🧑 Add/Update My Face");
                        manageFaceBtn.setPrefWidth(200);
                        manageFaceBtn.setPrefHeight(60);
                        manageFaceBtn.setStyle(
                                        "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
                        manageFaceBtn.setOnMouseEntered(e -> manageFaceBtn.setStyle(
                                        "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #7d3c98; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                        manageFaceBtn.setOnMouseExited(e -> manageFaceBtn.setStyle(
                                        "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
                        manageFaceBtn.setOnAction(e -> openFaceEnrollmentForCurrentUser());
                        buttonBox.getChildren().add(manageFaceBtn);
                }

                // Attendance table with card styling
                Label tableLabel = new Label("📅 My Attendance History (Lifetime)");
                tableLabel.setStyle(
                                "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 10 0 5 0;");

                VBox tableCard = new VBox(10);
                tableCard.setPadding(new Insets(20));
                tableCard.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

                TableView<Map<String, Object>> attendanceTable = createAttendanceTable();
                attendanceTable.setPrefHeight(400);
                attendanceTable.setStyle("-fx-background-radius: 8;");

                tableCard.getChildren().addAll(tableLabel, attendanceTable);

                dashboard.getChildren().addAll(
                                welcomeLabel,
                                subLabel,
                                buttonBox,
                                tableCard);

                return dashboard;
        }

        /**
         * Create teacher dashboard
         */
        private VBox createTeacherDashboard() {
                VBox dashboard = new VBox(25);
                dashboard.setPadding(new Insets(30));
                dashboard.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label welcomeLabel = new Label("👨‍🏫 Teacher Dashboard");
                welcomeLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label subLabel = new Label("Monitor student attendance and manage your own attendance");
                subLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #2c3e50;");

                Map<String, Object> teacherProfile = UserManager.getUserById(currentUserId);
                java.util.Map<Integer, String> batchToSubject = new java.util.HashMap<>();
                for (Map<String, Object> row : UserManager.getTeacherBatchSubjects(currentUserId)) {
                        Object batchObj = row.get("batchYear");
                        if (!(batchObj instanceof Number)) {
                                continue;
                        }
                        int batchYear = ((Number) batchObj).intValue();
                        String subject = row.get("subject") == null ? "" : String.valueOf(row.get("subject")).trim();
                        if (!subject.isEmpty()) {
                                batchToSubject.put(batchYear, subject);
                        }
                }

                Object profileBatchObj = teacherProfile.get("batchYear");
                Object profileSubjectObj = teacherProfile.get("subject");
                if (profileBatchObj instanceof Number) {
                        int batchYear = ((Number) profileBatchObj).intValue();
                        String subject = profileSubjectObj == null ? "" : String.valueOf(profileSubjectObj).trim();
                        if (!subject.isEmpty()) {
                                batchToSubject.putIfAbsent(batchYear, subject);
                        }
                }

                Label batchFilterLabel = new Label("Batch:");
                batchFilterLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                ComboBox<Integer> batchFilterCombo = new ComboBox<>();
                if (batchToSubject.isEmpty()) {
                        for (int year = 2023; year <= 2029; year++) {
                                batchFilterCombo.getItems().add(year);
                        }
                } else {
                        java.util.List<Integer> mappedBatches = new java.util.ArrayList<>(batchToSubject.keySet());
                        java.util.Collections.sort(mappedBatches);
                        batchFilterCombo.getItems().addAll(mappedBatches);
                }

                int defaultBatchYear = 2023;
                if (profileBatchObj instanceof Number) {
                        defaultBatchYear = ((Number) profileBatchObj).intValue();
                }
                if (!batchFilterCombo.getItems().contains(defaultBatchYear)
                                && !batchFilterCombo.getItems().isEmpty()) {
                        defaultBatchYear = batchFilterCombo.getItems().get(0);
                }
                batchFilterCombo.setValue(defaultBatchYear);
                batchFilterCombo.setPrefWidth(140);

                Label subjectInfoLabel = new Label();
                subjectInfoLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

                HBox batchFilterBox = new HBox(10, batchFilterLabel, batchFilterCombo, subjectInfoLabel);
                batchFilterBox.setStyle("-fx-alignment: center-left;");

                Label assignmentsLabel = new Label();
                assignmentsLabel.setWrapText(true);
                assignmentsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
                if (batchToSubject.isEmpty()) {
                        assignmentsLabel.setText("Assigned Batches: Not assigned yet");
                } else {
                        java.util.List<Integer> sortedBatches = new java.util.ArrayList<>(batchToSubject.keySet());
                        java.util.Collections.sort(sortedBatches);
                        StringBuilder mappingText = new StringBuilder("Assigned Batches: ");
                        for (int i = 0; i < sortedBatches.size(); i++) {
                                int batchYear = sortedBatches.get(i);
                                if (i > 0) {
                                        mappingText.append(" | ");
                                }
                                mappingText.append(batchYear).append(" (").append(batchToSubject.get(batchYear))
                                                .append(")");
                        }
                        assignmentsLabel.setText(mappingText.toString());
                }

                // Summary statistics for quick overview
                HBox statsBox = new HBox(20);
                statsBox.setStyle("-fx-alignment: center;");

                final int[] selectedBatchYear = new int[] { batchFilterCombo.getValue() == null ? defaultBatchYear
                                : batchFilterCombo.getValue() };

                // Button controls specific to teacher
                HBox buttonBox = new HBox(20);
                buttonBox.setStyle("-fx-alignment: center; -fx-padding: 20 0 20 0;");

                Button studentAttendanceBtn = new Button("📋 Students Attendance");
                studentAttendanceBtn.setPrefWidth(180);
                studentAttendanceBtn.setPrefHeight(50);
                studentAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                studentAttendanceBtn.setOnMouseEntered(e -> studentAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                studentAttendanceBtn.setOnMouseExited(e -> studentAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                studentAttendanceBtn
                                .setOnAction(e -> showRoleAttendanceWindow("Students Attendance (Lifetime)",
                                                "STUDENT",
                                                null,
                                                selectedBatchYear[0]));

                Button myAttendanceBtn = new Button("🧾 My Attendance");
                myAttendanceBtn.setPrefWidth(180);
                myAttendanceBtn.setPrefHeight(50);
                myAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                myAttendanceBtn.setOnMouseEntered(e -> myAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #2980b9; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                myAttendanceBtn.setOnMouseExited(e -> myAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                myAttendanceBtn.setOnAction(e -> showUserAttendanceTable());

                Button markAttendanceBtn = new Button("✅ Mark My Check-In");
                markAttendanceBtn.setPrefWidth(180);
                markAttendanceBtn.setPrefHeight(50);
                markAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                markAttendanceBtn.setOnMouseEntered(e -> markAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #229954; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                markAttendanceBtn.setOnMouseExited(e -> markAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                markAttendanceBtn.setOnAction(e -> markMyCheckIn());

                Button markCheckOutBtn = new Button("⏱ Mark My Check-Out");
                markCheckOutBtn.setPrefWidth(180);
                markCheckOutBtn.setPrefHeight(50);
                markCheckOutBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                markCheckOutBtn.setOnMouseEntered(e -> markCheckOutBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #a93226; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                markCheckOutBtn.setOnMouseExited(e -> markCheckOutBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                markCheckOutBtn.setOnAction(e -> markMyCheckOut());

                Button requestLeaveBtn = new Button("📋 Request Leave");
                requestLeaveBtn.setPrefWidth(180);
                requestLeaveBtn.setPrefHeight(50);
                requestLeaveBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                requestLeaveBtn.setOnMouseEntered(e -> requestLeaveBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #d35400; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                requestLeaveBtn.setOnMouseExited(e -> requestLeaveBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                requestLeaveBtn.setOnAction(e -> showLeaveRequestWindow());

                Button manageFaceBtn = new Button("🧑 Add/Update My Face");
                manageFaceBtn.setPrefWidth(190);
                manageFaceBtn.setPrefHeight(50);
                manageFaceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                manageFaceBtn.setOnAction(e -> openFaceEnrollmentForCurrentUser());

                Button resetPasswordBtn = createDashboardPasswordResetButton(190, 50);

                buttonBox.getChildren().addAll(studentAttendanceBtn, myAttendanceBtn, markAttendanceBtn,
                                markCheckOutBtn,
                                requestLeaveBtn, manageFaceBtn, resetPasswordBtn);

                // All time attendance table
                Label tableLabel = new Label("📅 Students Attendance (All Time)");
                tableLabel.setStyle(
                                "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 10 0 5 0;");

                TableView<Map<String, Object>> attendanceTable = createRoleAttendanceTable("STUDENT", null,
                                selectedBatchYear[0]);
                attendanceTable.setPrefHeight(300);

                Runnable refreshTeacherBatchView = () -> {
                        int batchYear = selectedBatchYear[0];

                        Map<String, Object> summary = AttendanceManager.getAttendanceSummaryByRole("STUDENT",
                                        batchYear);
                        VBox presentBox = createStatBox("Present", String.valueOf(summary.get("present")), "#27ae60");
                        VBox lateBox = createStatBox("Late", String.valueOf(summary.get("late")), "#f39c12");
                        VBox absentBox = createStatBox("Absent", String.valueOf(summary.get("absent")), "#e74c3c");

                        presentBox.setOnMouseClicked(e -> showRoleAttendanceWindow(
                                        "Students Present (All Time) - Batch " + batchYear,
                                        "STUDENT",
                                        "PRESENT",
                                        batchYear));
                        lateBox.setOnMouseClicked(e -> showRoleAttendanceWindow(
                                        "Students Late (All Time) - Batch " + batchYear,
                                        "STUDENT",
                                        "LATE",
                                        batchYear));
                        absentBox.setOnMouseClicked(e -> showRoleAttendanceWindow(
                                        "Students Absent (All Time) - Batch " + batchYear,
                                        "STUDENT",
                                        "ABSENT",
                                        batchYear));

                        statsBox.getChildren().setAll(presentBox, lateBox, absentBox);

                        java.util.List<Map<String, Object>> rows = AttendanceManager.getRoleAttendanceAllTime(
                                        "STUDENT",
                                        null,
                                        batchYear);
                        attendanceTable.setItems(javafx.collections.FXCollections.observableArrayList(rows));

                        String subject = batchToSubject.get(batchYear);
                        if (subject == null || subject.isBlank()) {
                                subjectInfoLabel.setText("Subject: Not assigned for batch " + batchYear);
                        } else {
                                subjectInfoLabel.setText("Subject: " + subject);
                        }

                        tableLabel.setText("📅 Students Attendance (All Time) - Batch " + batchYear);
                };

                batchFilterCombo.setOnAction(e -> {
                        Integer chosenBatch = batchFilterCombo.getValue();
                        if (chosenBatch == null) {
                                return;
                        }
                        selectedBatchYear[0] = chosenBatch;
                        refreshTeacherBatchView.run();
                });

                refreshTeacherBatchView.run();

                dashboard.getChildren().addAll(
                                welcomeLabel,
                                subLabel,
                                batchFilterBox,
                                assignmentsLabel,
                                new Separator(),
                                new Label("Today's Summary:"),
                                statsBox,
                                buttonBox,
                                new Separator(),
                                tableLabel,
                                attendanceTable);

                return dashboard;
        }

        /**
         * Create admin dashboard
         */
        private VBox createAdminDashboard() {
                VBox dashboard = new VBox(25);
                dashboard.setPadding(new Insets(30));
                dashboard.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label welcomeLabel = new Label("⚙️ Admin Dashboard");
                welcomeLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                // Summary statistics
                HBox statsBox = new HBox(20);
                statsBox.setStyle("-fx-alignment: center;");

                Map<String, Object> summary = AttendanceManager.getAttendanceSummaryByRole("STUDENT");

                VBox presentBox = createStatBox("Present", String.valueOf(summary.get("present")), "#27ae60");
                VBox lateBox = createStatBox("Late", String.valueOf(summary.get("late")), "#f39c12");
                VBox absentBox = createStatBox("Absent", String.valueOf(summary.get("absent")), "#e74c3c");

                presentBox.setOnMouseClicked(e -> showRoleAttendanceWindow("Students Present (Lifetime)", "STUDENT",
                                "PRESENT"));
                lateBox.setOnMouseClicked(
                                e -> showRoleAttendanceWindow("Students Late (Lifetime)", "STUDENT", "LATE"));
                absentBox.setOnMouseClicked(
                                e -> showRoleAttendanceWindow("Students Absent (Lifetime)", "STUDENT", "ABSENT"));

                statsBox.getChildren().addAll(presentBox, lateBox, absentBox);

                HBox buttonBox = new HBox(20);
                buttonBox.setStyle("-fx-alignment: center; -fx-padding: 10 0 10 0;");

                Button studentAttendanceBtn = new Button("📋 Students Attendance");
                studentAttendanceBtn.setPrefWidth(190);
                studentAttendanceBtn.setPrefHeight(50);
                studentAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                studentAttendanceBtn.setOnAction(
                                e -> showRoleAttendanceWindow("Students Attendance (Lifetime)", "STUDENT", null));

                Button teacherAttendanceBtn = new Button("👨‍🏫 Teachers Attendance");
                teacherAttendanceBtn.setPrefWidth(190);
                teacherAttendanceBtn.setPrefHeight(50);
                teacherAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                teacherAttendanceBtn.setOnAction(
                                e -> showRoleAttendanceWindow("Teachers Attendance (Lifetime)", "TEACHER", null));

                Button myAttendanceBtn = new Button("🧾 My Attendance");
                myAttendanceBtn.setPrefWidth(160);
                myAttendanceBtn.setPrefHeight(50);
                myAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #16a085; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                myAttendanceBtn.setOnAction(e -> showUserAttendanceTable());

                Button markAttendanceBtn = new Button("✅ Mark My Check-In");
                markAttendanceBtn.setPrefWidth(180);
                markAttendanceBtn.setPrefHeight(50);
                markAttendanceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                markAttendanceBtn.setOnAction(e -> markMyCheckIn());

                Button markCheckOutBtn = new Button("⏱ Mark My Check-Out");
                markCheckOutBtn.setPrefWidth(180);
                markCheckOutBtn.setPrefHeight(50);
                markCheckOutBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                markCheckOutBtn.setOnAction(e -> markMyCheckOut());

                Button manageFaceBtn = new Button("🧑 Add/Update My Face");
                manageFaceBtn.setPrefWidth(190);
                manageFaceBtn.setPrefHeight(50);
                manageFaceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                manageFaceBtn.setOnAction(e -> openFaceEnrollmentForCurrentUser());

                Button resetPasswordBtn = createDashboardPasswordResetButton(190, 50);

                buttonBox.getChildren().addAll(studentAttendanceBtn, teacherAttendanceBtn, myAttendanceBtn,
                                markAttendanceBtn, markCheckOutBtn, manageFaceBtn, resetPasswordBtn);

                Label teacherMapTitle = new Label("Teacher Batch-Subject Assignments:");
                teacherMapTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                ComboBox<String> teacherSelector = new ComboBox<>();
                teacherSelector.setPromptText("Select Teacher ID");
                teacherSelector.setPrefWidth(220);

                javafx.scene.control.ListView<String> teacherAssignmentsView = new javafx.scene.control.ListView<>();
                teacherAssignmentsView.setPrefHeight(130);

                java.util.Map<String, String> teacherIdToName = new java.util.HashMap<>();
                java.util.List<String> teacherIds = new java.util.ArrayList<>();
                for (Map<String, Object> user : UserManager.getAllUsers()) {
                        if (!"TEACHER".equalsIgnoreCase(String.valueOf(user.get("role")))) {
                                continue;
                        }
                        String teacherId = String.valueOf(user.get("id"));
                        String teacherName = String.valueOf(user.get("name"));
                        teacherIdToName.put(teacherId, teacherName);
                        teacherIds.add(teacherId);
                }
                java.util.Collections.sort(teacherIds);
                teacherSelector.getItems().addAll(teacherIds);

                Runnable refreshTeacherAssignments = () -> {
                        String selectedTeacherId = teacherSelector.getValue();
                        java.util.List<String> rows = new java.util.ArrayList<>();
                        if (selectedTeacherId == null || selectedTeacherId.isBlank()) {
                                rows.add("Select a teacher to view batch mappings.");
                        } else {
                                String teacherName = teacherIdToName.getOrDefault(selectedTeacherId, "");
                                if (!teacherName.isBlank()) {
                                        rows.add("Teacher: " + selectedTeacherId + " - " + teacherName);
                                } else {
                                        rows.add("Teacher: " + selectedTeacherId);
                                }

                                java.util.List<Map<String, Object>> mappings = UserManager
                                                .getTeacherBatchSubjects(selectedTeacherId);
                                if (mappings.isEmpty()) {
                                        rows.add("No batch-subject mapping assigned yet.");
                                } else {
                                        for (Map<String, Object> mapping : mappings) {
                                                rows.add("Batch " + mapping.get("batchYear") + " -> "
                                                                + mapping.get("subject"));
                                        }
                                }
                        }
                        teacherAssignmentsView.setItems(javafx.collections.FXCollections.observableArrayList(rows));
                };
                teacherSelector.setOnAction(e -> refreshTeacherAssignments.run());
                refreshTeacherAssignments.run();

                Button openTeacherAttendanceBtn = new Button("Open Selected Teacher Attendance");
                openTeacherAttendanceBtn.setOnAction(e -> {
                        String selectedTeacherId = teacherSelector.getValue();
                        if (selectedTeacherId == null || selectedTeacherId.isBlank()) {
                                showAlert("Info", "Please select a teacher first.");
                                return;
                        }
                        String teacherName = teacherIdToName.getOrDefault(selectedTeacherId, "");
                        String title = teacherName.isBlank()
                                        ? "Teacher Attendance - " + selectedTeacherId
                                        : "Teacher Attendance - " + teacherName + " (" + selectedTeacherId + ")";
                        showAttendanceTableForUser(selectedTeacherId, title);
                });

                // Students attendance table (lifetime)
                TableView<Map<String, Object>> attendanceTable = createRoleAttendanceTable("STUDENT", null);
                attendanceTable.setPrefHeight(350);

                dashboard.getChildren().addAll(
                                welcomeLabel,
                                new Separator(),
                                new Label("Today's Student Summary:"),
                                statsBox,
                                buttonBox,
                                teacherMapTitle,
                                teacherSelector,
                                teacherAssignmentsView,
                                openTeacherAttendanceBtn,
                                new Separator(),
                                new Label("Students Attendance Records (Lifetime):"),
                                attendanceTable);

                return dashboard;
        }

        /**
         * Create statistic box with modern card design
         */
        private VBox createStatBox(String title, String value, String color) {
                VBox box = new VBox(12);
                box.setPadding(new Insets(25));
                box.setPrefWidth(200);
                box.setStyle("-fx-background-color: " + color
                                + "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");

                // Add icon based on title
                String icon = title.equals("Present") ? "✅" : title.equals("Late") ? "⏰" : "❌";
                Label iconLabel = new Label(icon);
                iconLabel.setStyle("-fx-font-size: 32;");

                Label titleLabel = new Label(title.toUpperCase());
                titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: rgba(255,255,255,0.9);");

                Label valueLabel = new Label(value);
                valueLabel.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: white;");

                box.getChildren().addAll(iconLabel, titleLabel, valueLabel);

                // Hover effect
                box.setOnMouseEntered(e -> box.setStyle("-fx-background-color: " + color
                                + "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 6); -fx-scale-y: 1.03; -fx-scale-x: 1.03;"));
                box.setOnMouseExited(e -> box.setStyle("-fx-background-color: " + color
                                + "; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"));

                return box;
        }

        /**
         * Create attendance table
         */
        private TableView<Map<String, Object>> createAttendanceTable() {
                TableView<Map<String, Object>> table = new TableView<>();

                TableColumn<Map<String, Object>, String> userCol = new TableColumn<>("User ID");
                userCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userId")));

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("date"))));

                TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
                nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userName")));

                TableColumn<Map<String, Object>, String> checkInCol = new TableColumn<>("Check-In Time");
                checkInCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("checkInTime")));

                TableColumn<Map<String, Object>, String> checkOutCol = new TableColumn<>("Check-Out Time");
                checkOutCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().getOrDefault("checkOutTime", "-"))));

                TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
                statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("status")));

                table.getColumns().clear();
                table.getColumns().add(userCol);
                table.getColumns().add(dateCol);
                table.getColumns().add(nameCol);
                table.getColumns().add(checkInCol);
                table.getColumns().add(checkOutCol);
                table.getColumns().add(statusCol);
                table.setItems(javafx.collections.FXCollections.observableArrayList(
                                AttendanceManager.getUserAttendance(currentUserId)));

                return table;
        }

        /**
         * Create attendance table filtered by role and optional status.
         */
        private TableView<Map<String, Object>> createRoleAttendanceTable(String role, String status) {
                return createRoleAttendanceTable(role, status, null);
        }

        /**
         * Create attendance table filtered by role, optional status, and optional
         * batch year.
         */
        private TableView<Map<String, Object>> createRoleAttendanceTable(String role, String status,
                        Integer batchYear) {
                TableView<Map<String, Object>> table = new TableView<>();

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("attendanceDate"))));

                TableColumn<Map<String, Object>, String> userCol = new TableColumn<>("User ID");
                userCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userId")));

                TableColumn<Map<String, Object>, String> roleCol = new TableColumn<>("Role");
                roleCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().getOrDefault("userRole", "-"))));

                TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
                nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userName")));

                TableColumn<Map<String, Object>, String> checkInCol = new TableColumn<>("Check-In Time");
                checkInCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("checkInTime"))));

                TableColumn<Map<String, Object>, String> checkOutCol = new TableColumn<>("Check-Out Time");
                checkOutCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("checkOutTime"))));

                TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
                statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("status")));

                TableColumn<Map<String, Object>, String> faceRegisteredCol = new TableColumn<>("Face Registered");
                faceRegisteredCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().getOrDefault("faceRegistered", "-"))));

                TableColumn<Map<String, Object>, String> faceUsedCol = new TableColumn<>("Face Verified");
                faceUsedCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().getOrDefault("usedFaceRecognition", "-"))));

                table.getColumns().clear();
                table.getColumns().addAll(java.util.List.of(dateCol, userCol, roleCol, nameCol, checkInCol,
                                checkOutCol, statusCol, faceRegisteredCol, faceUsedCol));

                java.util.List<Map<String, Object>> data;
                data = AttendanceManager.getRoleAttendanceAllTime(role,
                                status == null || status.isBlank() ? null : status,
                                batchYear);

                table.setItems(javafx.collections.FXCollections.observableArrayList(data));
                return table;
        }

        /**
         * Show role-filtered attendance in a separate window.
         */
        private void showRoleAttendanceWindow(String title, String role, String status) {
                showRoleAttendanceWindow(title, role, status, null);
        }

        /**
         * Show role-filtered attendance in a separate window with optional batch year.
         */
        private void showRoleAttendanceWindow(String title, String role, String status, Integer batchYear) {
                Stage stage = new Stage();
                stage.setTitle(title);
                stage.setWidth(900);
                stage.setHeight(560);

                final String roleFilter = role != null ? role.toUpperCase() : "ALL";
                final String statusFilter = (status == null || status.isBlank()) ? "ALL" : status.toUpperCase();
                final String batchFilter = batchYear == null ? "ALL" : String.valueOf(batchYear);

                VBox root = new VBox(12);
                root.setPadding(new Insets(18));

                TableView<Map<String, Object>> table = createRoleAttendanceTable(role, status, batchYear);
                table.setPrefHeight(430);

                Label filterLabel = new Label();
                filterLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

                Button refreshBtn = new Button("Refresh");
                Runnable refreshData = () -> {
                        java.util.List<Map<String, Object>> refreshed = AttendanceManager.getRoleAttendanceAllTime(
                                        role,
                                        status == null || status.isBlank() ? null : status,
                                        batchYear);
                        table.setItems(javafx.collections.FXCollections.observableArrayList(refreshed));
                        filterLabel.setText("Filter: Role = " + roleFilter + " | Status = " + statusFilter
                                        + " | Batch = " + batchFilter
                                        + " | Window = All Time | Records = " + refreshed.size());
                };

                refreshData.run();
                refreshBtn.setOnAction(e -> refreshData.run());

                root.getChildren().addAll(new Label(title), filterLabel, table, refreshBtn);
                stage.setScene(new Scene(root));
                stage.show();
        }

        /**
         * Marks current logged-in user's check-in attendance.
         */
        private void markMyCheckIn() {
                if ("STUDENT".equalsIgnoreCase(currentUserRole)) {
                        showAlert("Info", "Students must mark attendance through face recognition camera.");
                        return;
                }

                boolean marked = AttendanceManager.markAttendance(currentUserId, 1.0f, false);
                if (marked) {
                        showAlert("Success", "Check-in marked successfully for " + currentUserId);
                } else {
                        showAlert("Info",
                                        "Check-in not marked. It may already be marked, on approved leave, or a DB issue.");
                }
        }

        /**
         * Marks current logged-in user's check-out attendance.
         */
        private void markMyCheckOut() {
                boolean checkedOut = AttendanceManager.markCheckOut(currentUserId);
                if (checkedOut) {
                        showAlert("Success", "Check-out marked successfully for " + currentUserId);
                } else {
                        showAlert("Info",
                                        "Check-out not marked. You may not be checked in yet, or check-out may already be done.");
                }
        }

        /**
         * Show registration window with face enrollment
         */
        private void showRegistrationWindow() {
                if (registrationStage != null && registrationStage.isShowing()) {
                        registrationStage.toFront();
                        registrationStage.requestFocus();
                        return;
                }

                registrationStage = new Stage();
                Stage regStage = registrationStage;
                regStage.initOwner(primaryStage);
                regStage.setTitle("✨ User Registration");
                regStage.setWidth(550);
                regStage.setHeight(800);
                regStage.setOnHidden(e -> {
                        if (registrationStage == regStage) {
                                registrationStage = null;
                        }
                });

                VBox root = new VBox(15);
                root.setPadding(new Insets(10)); // Reduce top/bottom padding
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label titleLabel = new Label("👤 Create New Account");
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label subtitleLabel = new Label("Fill in your details to register");
                subtitleLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 5 0;"); // Reduce
                                                                                                            // bottom
                                                                                                            // padding

                // Form card
                VBox formCard = new VBox(12);
                formCard.setPadding(new Insets(10)); // Reduce padding for compactness
                formCard.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

                // Create HBox for ID prefix and roll number
                HBox idBox = new HBox(8);
                
                TextField prefixField = new TextField();
                prefixField.setPromptText("Prefix");
                prefixField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                prefixField.setEditable(false);
                prefixField.setPrefWidth(80);
                
                TextField rollNoField = new TextField();
                rollNoField.setPromptText("Roll No. (3 digits)");
                rollNoField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                rollNoField.setPrefWidth(100);
                
                Label idAvailabilityLabel = new Label("ℹ Enter roll number");
                idAvailabilityLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                
                idBox.getChildren().addAll(prefixField, rollNoField);

                Label idLabel = new Label("🆔 Roll No. (Format: Prefix + 3 digits)");
                
                // This field stores the complete ID for submission
                TextField idField = new TextField();
                idField.setVisible(false);
                idField.setManaged(false);

                TextField nameField = new TextField();
                nameField.setPromptText("Full Name");
                nameField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");

                TextField emailField = new TextField();
                emailField.setPromptText("Email Address");
                emailField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");
                emailField.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal == null) {
                                return;
                        }
                        String normalized = ValidationUtils.normalizeEmail(newVal);
                        if (normalized != null && !normalized.equals(newVal)) {
                                emailField.setText(normalized);
                        }
                });
                emailField.focusedProperty().addListener((obs, oldVal, nowFocused) -> {
                        if (!nowFocused) {
                                emailField.setText(ValidationUtils.normalizeEmail(emailField.getText()));
                        }
                });

                PasswordField passwordField = new PasswordField();
                passwordField.setPromptText("Password (min 8 chars, uppercase, lowercase, digit, special)");
                passwordField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");

                TextField securityQuestionField = new TextField();
                securityQuestionField.setPromptText("Security Question (for forgot password)");
                securityQuestionField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");

                TextField securityAnswerField = new TextField();
                securityAnswerField.setPromptText("Security Answer");
                securityAnswerField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");

                Label roleLabel = new Label("Role:");
                roleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                ComboBox<String> roleCombo = new ComboBox<>();
                roleCombo.getItems().addAll("STUDENT", "EMPLOYEE", "TEACHER", "ADMIN");
                roleCombo.setValue("STUDENT");
                roleCombo.setStyle("-fx-padding: 8; -fx-font-size: 13; -fx-background-radius: 8;");
                roleCombo.setPrefWidth(200);

                Label batchLabel = new Label("🎓 Batch:");
                batchLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                ComboBox<Integer> batchCombo = new ComboBox<>();
                for (int year = 2023; year <= 2029; year++) {
                        batchCombo.getItems().add(year);
                }
                batchCombo.setValue(2023);
                batchCombo.setStyle("-fx-padding: 8; -fx-font-size: 13; -fx-background-radius: 8;");
                batchCombo.setPrefWidth(200);

                Label subjectLabel = new Label("📘 Subject:");
                subjectLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                TextField subjectField = new TextField();
                subjectField.setPromptText("Subject handled for selected batch (e.g., MIS)");
                subjectField.setStyle(
                                "-fx-padding: 12; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1.5;");

                formCard.getChildren().addAll(
                                idLabel, idBox, idAvailabilityLabel,
                                new Label("👤 Full Name"), nameField,
                                new Label("📧 Email"), emailField,
                                new Label("🔑 Password"), passwordField,
                                new Label("❓ Security Question"), securityQuestionField,
                                new Label("🛡 Security Answer"), securityAnswerField,
                                roleLabel, roleCombo,
                                batchLabel, batchCombo,
                                subjectLabel, subjectField);

                // Face capture section
                VBox faceSection = new VBox(10);
                faceSection.setPadding(new Insets(8)); // Reduce padding for compactness
                faceSection.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10;");

                Label faceTitleLabel = new Label("📸 Face Capture");
                faceTitleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                // Face enrollment section
                Label faceStatusLabel = new Label("❌ No face captured — required for students");
                faceStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");

                // Stores the confirmed face bytes from the enrollment window.
                // Set inside the callback so it is never null due to a race on
                // enrollmentWindow.
                java.util.concurrent.atomic.AtomicReference<byte[]> confirmedFaceData = new java.util.concurrent.atomic.AtomicReference<>(
                                null);

                // Tracks whether student face enrollment was explicitly completed in
                // this registration session.
                java.util.concurrent.atomic.AtomicBoolean studentFaceEnrollmentCompleted = new java.util.concurrent.atomic.AtomicBoolean(
                                false);

                // Store reference to enrollment window to get captured face data
                java.util.concurrent.atomic.AtomicReference<FaceEnrollmentWindow> enrollmentWindow = new java.util.concurrent.atomic.AtomicReference<>();

                Button registerBtn = new Button("✅ Complete Registration");

                HBox faceButtonBox = new HBox(10);

                Button captureFaceBtn = new Button("📷 Capture Face");
                captureFaceBtn.setPrefWidth(150);
                captureFaceBtn.setPrefHeight(40);
                captureFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                captureFaceBtn.setOnMouseEntered(e -> captureFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #5568d3; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                captureFaceBtn.setOnMouseExited(e -> captureFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                captureFaceBtn.setOnAction(e -> {
                        if (nameField.getText().isEmpty()) {
                                showAlert("Warning", "Please enter your name first");
                                return;
                        }

                        // Open face enrollment window
                        FaceEnrollmentWindow window = new FaceEnrollmentWindow(nameField.getText(), () -> {
                                // Defer to the next UI pulse so enrollmentWindow has been set reliably.
                                Platform.runLater(() -> {
                                        FaceEnrollmentWindow capturedWindow = enrollmentWindow.get();
                                        byte[] capturedData = capturedWindow == null ? null
                                                        : capturedWindow.getCapturedFaceData();
                                        if (capturedData != null && capturedData.length > 0) {
                                                confirmedFaceData.set(capturedData);
                                                studentFaceEnrollmentCompleted.set(true);
                                                faceStatusLabel.setText("✅ Face captured successfully!");
                                                faceStatusLabel.setStyle(
                                                                "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13;");
                                                if ("STUDENT".equalsIgnoreCase(roleCombo.getValue())) {
                                                        registerBtn.setDisable(false);
                                                }
                                        } else {
                                                confirmedFaceData.set(null);
                                                studentFaceEnrollmentCompleted.set(false);
                                                faceStatusLabel.setText("❌ Face capture was cancelled — try again");
                                                faceStatusLabel.setStyle(
                                                                "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");
                                                if ("STUDENT".equalsIgnoreCase(roleCombo.getValue())) {
                                                        registerBtn.setDisable(true);
                                                }
                                        }
                                });
                        });
                        enrollmentWindow.set(window);
                });

                Button exportFaceBtn = new Button("💾 Export Face");
                exportFaceBtn.setPrefWidth(130);
                exportFaceBtn.setPrefHeight(40);
                exportFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                exportFaceBtn.setOnMouseEntered(e -> exportFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #7d3c98; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                exportFaceBtn.setOnMouseExited(e -> exportFaceBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                exportFaceBtn.setOnAction(e -> {
                        String id = idField.getText().trim();
                        if (id.isEmpty()) {
                                showAlert("Warning", "Please enter User ID before exporting");
                                return;
                        }

                        FaceEnrollmentWindow window = enrollmentWindow.get();
                        if (window == null) {
                                showAlert("Warning", "Please capture a face first");
                                return;
                        }

                        byte[] faceData = window.getCapturedFaceData();
                        if (faceData == null || faceData.length == 0) {
                                showAlert("Warning", "No captured face data found");
                                return;
                        }

                        File outputDir = new File(AppConfig.FACE_DATA_DIRECTORY);
                        if (!outputDir.exists() && !outputDir.mkdirs()) {
                                showAlert("Error", "Could not create output directory: " + outputDir.getPath());
                                return;
                        }

                        File outputFile = new File(outputDir, id + ".png");
                        try {
                                Files.write(outputFile.toPath(), faceData);
                                showAlert("Success", "Face image saved to: " + outputFile.getPath());
                        } catch (IOException ex) {
                                showAlert("Error", "Failed to export face image: " + ex.getMessage());
                        }
                });

                faceButtonBox.getChildren().addAll(captureFaceBtn, exportFaceBtn);
                faceSection.getChildren().addAll(faceTitleLabel, faceStatusLabel, faceButtonBox);

                Runnable refreshGeneratedId = () -> {
                        String selectedRole = roleCombo.getValue();
                        Integer selectedBatch = batchCombo.getValue();
                        
                        // Update prefix field based on role and batch
                        if (("STUDENT".equals(selectedRole) || "TEACHER".equals(selectedRole)) 
                                && selectedBatch != null) {
                                String prefix = UserManager.getBatchPrefixForYear(selectedBatch);
                                prefixField.setText(prefix);
                                rollNoField.setPromptText("STUDENT".equals(selectedRole) ? "Roll No. (3 digits)" : "Teacher No. (2 digits)");
                        } else if ("ADMIN".equals(selectedRole)) {
                                prefixField.setText("ADMIN");
                                rollNoField.setPromptText("Number (2 digits)");
                        } else if ("EMPLOYEE".equals(selectedRole)) {
                                prefixField.setText("EMP");
                                rollNoField.setPromptText("Number (3 digits)");
                        }
                        
                        // Clear roll number field when batch/role changes
                        rollNoField.clear();
                        idAvailabilityLabel.setText("ℹ Enter roll number");
                        idAvailabilityLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                        idField.clear();
                };
                
                // Add listener for real-time validation of roll number
                rollNoField.textProperty().addListener((obs, oldVal, newVal) -> {
                        String prefix = prefixField.getText().trim();
                        String selectedRole = roleCombo.getValue();
                        Integer selectedBatch = batchCombo.getValue();
                        
                        if (newVal == null || newVal.isEmpty()) {
                                idAvailabilityLabel.setText("ℹ Enter roll number");
                                idAvailabilityLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                                idField.clear();
                                return;
                        }
                        
                        // Validate format based on role
                        boolean validFormat = false;
                        if ("STUDENT".equals(selectedRole)) {
                                validFormat = newVal.matches("\\d{3}"); // 3 digits
                        } else if ("TEACHER".equals(selectedRole)) {
                                validFormat = newVal.matches("\\d{2}"); // 2 digits
                        } else if ("ADMIN".equals(selectedRole)) {
                                validFormat = newVal.matches("\\d{2}"); // 2 digits
                        } else if ("EMPLOYEE".equals(selectedRole)) {
                                validFormat = newVal.matches("\\d{3}"); // 3 digits
                        }
                        
                        if (!validFormat) {
                                idAvailabilityLabel.setText("❌ Invalid format");
                                idAvailabilityLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
                                idField.clear();
                                return;
                        }
                        
                        // Build complete user ID
                        String completeId = prefix + newVal;
                        idField.setText(completeId);
                        
                        // Check availability based on role
                        boolean isAvailable = false;
                        if ("STUDENT".equals(selectedRole) || "TEACHER".equals(selectedRole)) {
                                isAvailable = UserManager.isRoleCodeAvailableInBatch(completeId, selectedRole,
                                                selectedBatch);
                        } else {
                                isAvailable = !UserManager.checkUserIdExists(completeId);
                        }
                        
                        if (isAvailable) {
                                idAvailabilityLabel.setText("✅ ID available: " + completeId);
                                idAvailabilityLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11;");
                        } else {
                                idAvailabilityLabel.setText("❌ ID already taken: " + completeId);
                                idAvailabilityLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
                        }
                });

                // Face data is mandatory for students and optional for other roles.
                Runnable updateRoleSpecificRegistrationUI = () -> {
                        String selectedRole = roleCombo.getValue();
                        boolean requiresFace = "STUDENT".equals(selectedRole);
                        faceSection.setVisible(requiresFace);
                        faceSection.setManaged(requiresFace);

                        boolean requiresBatch = "STUDENT".equals(selectedRole) || "TEACHER".equals(selectedRole);
                        batchLabel.setVisible(requiresBatch);
                        batchLabel.setManaged(requiresBatch);
                        batchCombo.setVisible(requiresBatch);
                        batchCombo.setManaged(requiresBatch);

                        boolean requiresSubject = "TEACHER".equals(selectedRole);
                        subjectLabel.setVisible(requiresSubject);
                        subjectLabel.setManaged(requiresSubject);
                        subjectField.setVisible(requiresSubject);
                        subjectField.setManaged(requiresSubject);
                        if (!requiresSubject) {
                                subjectField.clear();
                        }

                        // Reset confirmed face data whenever role changes so a previous
                        // capture from a different role cannot pollute the student check.
                        confirmedFaceData.set(null);
                        studentFaceEnrollmentCompleted.set(false);

                        registerBtn.setDisable(requiresFace);

                        if (requiresFace) {
                                faceStatusLabel.setText("❌ Face is mandatory for student registration");
                                faceStatusLabel.setStyle(
                                                "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");
                        } else {
                                faceStatusLabel.setText("ℹ Face is optional for this role");
                                faceStatusLabel.setStyle(
                                                "-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-font-size: 13;");
                        }

                        if ("TEACHER".equals(selectedRole)) {
                                idLabel.setText("🆔 " + ValidationUtils.getUserIdLabelForRole(selectedRole));
                                idField.setPromptText("Auto-generated Teacher ID (e.g., 23ITM01)");
                        } else if ("STUDENT".equals(selectedRole)) {
                                idLabel.setText("🆔 " + ValidationUtils.getUserIdLabelForRole(selectedRole));
                                idField.setPromptText("Auto-generated Roll No. (e.g., 23ITM001)");
                        } else if ("EMPLOYEE".equals(selectedRole)) {
                                idLabel.setText("🆔 " + ValidationUtils.getUserIdLabelForRole(selectedRole));
                                idField.setPromptText("Auto-generated Employee ID (e.g., EMP001)");
                        } else if ("ADMIN".equals(selectedRole)) {
                                idLabel.setText("🆔 " + ValidationUtils.getUserIdLabelForRole(selectedRole));
                                idField.setPromptText("Auto-generated Admin ID (e.g., ADMIN01)");
                        }

                        refreshGeneratedId.run();
                };
                roleCombo.setOnAction(e -> updateRoleSpecificRegistrationUI.run());
                batchCombo.setOnAction(e -> refreshGeneratedId.run());
                updateRoleSpecificRegistrationUI.run();

                // Register button
                registerBtn.setPrefWidth(250);
                registerBtn.setPrefHeight(50);
                registerBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");
                registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #0e877d; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 5);"));
                registerBtn.setOnMouseExited(e -> registerBtn.setStyle(
                                "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"));
                registerBtn.setOnAction(e -> {
                        String id = idField.getText();
                        String name = nameField.getText();
                        String email = ValidationUtils.normalizeEmail(emailField.getText());
                        emailField.setText(email);
                        String password = passwordField.getText();
                        String role = roleCombo.getValue();
                        Integer batchYear = batchCombo.getValue();
                        String subject = subjectField.getText();
                        String securityQuestion = securityQuestionField.getText();
                        String securityAnswer = securityAnswerField.getText();

                        if (id.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()
                                        || securityQuestion == null || securityQuestion.trim().isEmpty()
                                        || securityAnswer == null || securityAnswer.trim().isEmpty()) {
                                showAlert("Error", "Please fill all fields");
                                return;
                        }

                        if (("STUDENT".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role))
                                        && batchYear == null) {
                                showAlert("Error", "Please select a batch year.");
                                return;
                        }

                        if ("TEACHER".equalsIgnoreCase(role)
                                        && (subject == null || subject.trim().isEmpty())) {
                                showAlert("Error", "Please enter teacher subject for the selected batch.");
                                return;
                        }

                        // Validate user ID format and availability
                        if (!UserManager.validateUserIdFormat(id, role, batchYear)) {
                                showAlert("Error", "Invalid user ID format for the selected role and batch.");
                                return;
                        }

                        boolean isIdAvailable = false;
                        if ("STUDENT".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role)) {
                                isIdAvailable = UserManager.isRoleCodeAvailableInBatch(id, role, batchYear);
                        } else {
                                isIdAvailable = !UserManager.checkUserIdExists(id);
                        }

                        if (!isIdAvailable) {
                                showAlert("Error", "User ID '" + id + "' is already taken. Please choose a different roll number.");
                                return;
                        }

                        // Obtain face data — use confirmedFaceData as the authoritative source
                        // (set inside the enrollment callback, never affected by reference ordering).
                        byte[] capturedFaceData = confirmedFaceData.get();

                        if ("STUDENT".equalsIgnoreCase(role)) {
                                if (!studentFaceEnrollmentCompleted.get()
                                                || capturedFaceData == null
                                                || capturedFaceData.length == 0) {
                                        showAlert("Error",
                                                        "⚠ Face data is mandatory for student registration.\nPlease click 'Capture Face' and complete face enrollment first.");
                                        faceStatusLabel.setText(
                                                        "❌ Face is mandatory — please capture before registering");
                                        faceStatusLabel.setStyle(
                                                        "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");
                                        return;
                                }
                        }

                        boolean success = false;
                        boolean storedFaceData = false;

                        if (capturedFaceData != null && capturedFaceData.length > 0) {
                                // Register with face data
                                success = UserManager.registerUserWithFaceData(id, name, email, password, role,
                                                capturedFaceData, securityQuestion, securityAnswer,
                                                batchYear, subject);
                                storedFaceData = success;
                        } else {
                                // No face data — only allowed for non-student roles
                                success = UserManager.registerUser(id, name, email, password, role,
                                                securityQuestion, securityAnswer,
                                                batchYear, subject);
                        }

                        if (success) {
                                String faceMessage = storedFaceData
                                                ? "Face data stored successfully in database."
                                                : "Face data not stored. You can add it later from your profile.";
                                showAlert("Success", "User registered successfully!\n" + faceMessage);
                                regStage.close();
                        } else {
                                refreshGeneratedId.run();
                                showAlert("Error", "Registration failed. Please check your input.");
                        }
                });

                HBox registerBox = new HBox(registerBtn);
                registerBox.setStyle("-fx-alignment: center; -fx-padding: 5 0 0 0;"); // Reduce top padding

                // Create a scrollable form container so all fields fit in the window
                VBox formContent = new VBox(15);
                formContent.getChildren().addAll(formCard, faceSection);

                ScrollPane scrollPane = new ScrollPane(formContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefHeight(700);
                scrollPane.setStyle("-fx-control-inner-background: #f5f7fa;");

                root.getChildren().addAll(
                                titleLabel,
                                subtitleLabel,
                                scrollPane,
                                registerBox);

                regStage.setScene(new Scene(root));
                regStage.show();
        }

        /**
         * Show user management window
         */
        private void showUserManagement() {
                Stage manageStage = new Stage();
                manageStage.setTitle("Manage Users");
                manageStage.setWidth(800);
                manageStage.setHeight(500);

                VBox root = new VBox(10);
                root.setPadding(new Insets(20));

                TableView<Map<String, Object>> userTable = new TableView<>();

                TableColumn<Map<String, Object>, String> idCol = new TableColumn<>("User ID");
                idCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("id")));

                TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
                nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("name")));

                TableColumn<Map<String, Object>, String> emailCol = new TableColumn<>("Email");
                emailCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("email")));

                TableColumn<Map<String, Object>, String> roleCol = new TableColumn<>("Role");
                roleCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("role")));

                TableColumn<Map<String, Object>, String> faceCol = new TableColumn<>("Face Registered");
                faceCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                Boolean.TRUE.equals(param.getValue().get("hasFaceData")) ? "YES" : "NO"));

                userTable.getColumns().add(idCol);
                userTable.getColumns().add(nameCol);
                userTable.getColumns().add(emailCol);
                userTable.getColumns().add(roleCol);
                userTable.getColumns().add(faceCol);
                userTable.setItems(javafx.collections.FXCollections.observableArrayList(UserManager.getAllUsers()));

                HBox buttonBox = new HBox(10);
                Button refreshBtn = new Button("Refresh");
                refreshBtn.setOnAction(e -> userTable.setItems(
                                javafx.collections.FXCollections.observableArrayList(UserManager.getAllUsers())));

                Button deactivateBtn = new Button("Deactivate Selected");
                deactivateBtn.setOnAction(e -> {
                        Map<String, Object> selected = userTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                                UserManager.deactivateUser((String) selected.get("id"));
                                userTable.setItems(javafx.collections.FXCollections
                                                .observableArrayList(UserManager.getAllUsers()));
                        }
                });

                buttonBox.getChildren().addAll(refreshBtn, deactivateBtn);
                root.getChildren().addAll(userTable, buttonBox);

                manageStage.setScene(new Scene(root));
                manageStage.show();
        }

        /**
         * Show attendance records for current user
         */
        private void showUserAttendanceTable() {
                showAttendanceTableForUser(currentUserId, "My Attendance Records");
        }

        private void showAttendanceTableForUser(String userId, String windowTitle) {
                Stage attendanceStage = new Stage();
                attendanceStage.setTitle(windowTitle);
                attendanceStage.setWidth(700);
                attendanceStage.setHeight(500);

                VBox root = new VBox(10);
                root.setPadding(new Insets(20));

                TableView<Map<String, Object>> table = new TableView<>();

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("date")));

                TableColumn<Map<String, Object>, String> checkInCol = new TableColumn<>("Check-In");
                checkInCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("checkInTime")));

                TableColumn<Map<String, Object>, String> checkOutCol = new TableColumn<>("Check-Out");
                checkOutCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().getOrDefault("checkOutTime", "-"))));

                TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
                statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("status")));

                table.getColumns().clear();
                table.getColumns().add(dateCol);
                table.getColumns().add(checkInCol);
                table.getColumns().add(checkOutCol);
                table.getColumns().add(statusCol);
                table.setItems(javafx.collections.FXCollections.observableArrayList(
                                AttendanceManager.getUserAttendance(userId)));

                root.getChildren().addAll(new Label("Attendance history (Lifetime):"),
                                table);

                attendanceStage.setScene(new Scene(root));
                attendanceStage.show();
        }

        /**
         * Create employee dashboard
         */
        private VBox createEmployeeDashboard() {
                VBox dashboard = new VBox(22);
                dashboard.setPadding(new Insets(30));
                dashboard.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label welcomeLabel = new Label("🧑‍💼 Employee Dashboard");
                welcomeLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label subLabel = new Label("Review users, face enrollment, and all-time attendance");
                subLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #7f8c8d;");

                List<Map<String, Object>> users = UserManager.getAllUsers();
                int totalUsers = users.size();
                int usersWithFace = 0;
                for (Map<String, Object> user : users) {
                        if (Boolean.TRUE.equals(user.get("hasFaceData"))) {
                                usersWithFace++;
                        }
                }
                int usersWithoutFace = Math.max(0, totalUsers - usersWithFace);

                Map<String, Object> attendanceSummary = AttendanceManager.getAttendanceStatusSummaryAllTime(null,
                                null);
                int totalAttendanceRecords = ((Number) attendanceSummary.getOrDefault("totalRecords", 0)).intValue();
                int presentCount = ((Number) attendanceSummary.getOrDefault("present", 0)).intValue();
                int lateCount = ((Number) attendanceSummary.getOrDefault("late", 0)).intValue();

                HBox metricsBox = new HBox(18);
                metricsBox.setStyle("-fx-alignment: center;");
                metricsBox.getChildren().addAll(
                                createStatBox("Users", String.valueOf(totalUsers), "#3498db"),
                                createStatBox("Face Registered", String.valueOf(usersWithFace), "#27ae60"),
                                createStatBox("Face Missing", String.valueOf(usersWithoutFace), "#e74c3c"),
                                createStatBox("Attendance", String.valueOf(totalAttendanceRecords), "#f39c12"));

                PieChart attendanceChart = new PieChart();
                attendanceChart.setTitle("All-Time Attendance Breakdown");
                attendanceChart.getData().addAll(
                                new PieChart.Data("Present", presentCount),
                                new PieChart.Data("Late", lateCount));
                attendanceChart.setPrefHeight(320);

                TableView<Map<String, Object>> userTable = new TableView<>();

                TableColumn<Map<String, Object>, String> idCol = new TableColumn<>("User ID");
                idCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("id"))));

                TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
                nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("name"))));

                TableColumn<Map<String, Object>, String> roleCol = new TableColumn<>("Role");
                roleCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("role"))));

                TableColumn<Map<String, Object>, String> emailCol = new TableColumn<>("Email");
                emailCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("email"))));

                TableColumn<Map<String, Object>, String> faceCol = new TableColumn<>("Face Registered");
                faceCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                Boolean.TRUE.equals(param.getValue().get("hasFaceData")) ? "YES" : "NO"));

                userTable.getColumns().addAll(java.util.List.of(idCol, nameCol, roleCol, emailCol, faceCol));
                userTable.setItems(javafx.collections.FXCollections.observableArrayList(users));
                userTable.setPrefHeight(280);

                TableView<Map<String, Object>> attendanceTable = createRoleAttendanceTable(null, null, null);
                attendanceTable.setPrefHeight(300);

                Button refreshBtn = new Button("Refresh Data");
                refreshBtn.setOnAction(e -> {
                        List<Map<String, Object>> refreshedUsers = UserManager.getAllUsers();
                        userTable.setItems(javafx.collections.FXCollections.observableArrayList(refreshedUsers));
                        attendanceTable.setItems(javafx.collections.FXCollections.observableArrayList(
                                        AttendanceManager.getRoleAttendanceAllTime(null,
                                                        null,
                                                        null)));
                });

                Button manageFaceBtn = new Button("🧑 Add/Update My Face");
                manageFaceBtn.setPrefWidth(190);
                manageFaceBtn.setPrefHeight(50);
                manageFaceBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #8e44ad; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                manageFaceBtn.setOnAction(e -> openFaceEnrollmentForCurrentUser());

                Button myAttendanceBtn = new Button("📊 My Attendance");
                myAttendanceBtn.setOnAction(e -> showUserAttendanceTable());

                Button resetPasswordBtn = createDashboardPasswordResetButton(190, 50);

                HBox actionBox = new HBox(12, refreshBtn, manageFaceBtn, myAttendanceBtn, resetPasswordBtn);
                actionBox.setStyle("-fx-alignment: center;");

                dashboard.getChildren().addAll(
                                welcomeLabel,
                                subLabel,
                                metricsBox,
                                attendanceChart,
                                new Label("Users and Face Enrollment Status:"),
                                userTable,
                                actionBox,
                                new Separator(),
                                new Label("All Time Attendance Records:"),
                                attendanceTable);

                return dashboard;
        }

        /**
         * Show reports with enhanced statistics and styling
         */
        private void showReports() {
                Stage reportStage = new Stage();
                reportStage.setTitle("📊 Attendance Reports");
                reportStage.setWidth(1000);
                reportStage.setHeight(700);

                VBox root = new VBox(15);
                root.setPadding(new Insets(25));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                // Title
                Label titleLabel = new Label("📊 Attendance Report - Last 7 Days");
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                // Statistics cards
                HBox statsBox = new HBox(20);
                statsBox.setStyle("-fx-alignment: center;");

                java.time.LocalDate weekStart = java.time.LocalDate.now().minusDays(6);
                java.util.List<Map<String, Object>> weeklyReport = AttendanceManager.generateWeeklyReport(weekStart);

                // Calculate overall statistics
                int totalPresent = 0, totalLate = 0, totalAbsent = 0;
                for (Map<String, Object> record : weeklyReport) {
                        totalPresent += ((Number) record.get("presentCount")).intValue();
                        totalLate += ((Number) record.get("lateCount")).intValue();
                        totalAbsent += ((Number) record.get("absentCount")).intValue();
                }
                int totalRecords = totalPresent + totalLate + totalAbsent;

                double presentPercent = totalRecords > 0 ? (totalPresent * 100.0) / totalRecords : 0;
                double latePercent = totalRecords > 0 ? (totalLate * 100.0) / totalRecords : 0;
                double absentPercent = totalRecords > 0 ? (totalAbsent * 100.0) / totalRecords : 0;

                VBox presentStatBox = createStatBox("Present",
                                totalPresent + " (" + String.format("%.1f", presentPercent) + "%)", "#27ae60");
                VBox lateStatBox = createStatBox("Late", totalLate + " (" + String.format("%.1f", latePercent) + "%)",
                                "#f39c12");
                VBox absentStatBox = createStatBox("Absent",
                                totalAbsent + " (" + String.format("%.1f", absentPercent) + "%)",
                                "#e74c3c");

                statsBox.getChildren().addAll(presentStatBox, lateStatBox, absentStatBox);

                // Report table with enhanced styling
                Label reportLabel = new Label("📋 Detailed Report by User:");
                reportLabel
                                .setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 10 0;");

                TableView<Map<String, Object>> table = new TableView<>();
                table.setStyle("-fx-background-radius: 8; -fx-font-size: 12;");

                TableColumn<Map<String, Object>, String> userCol = new TableColumn<>("User Name");
                userCol.setPrefWidth(150);
                userCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userName")));

                TableColumn<Map<String, Object>, String> presentCol = new TableColumn<>("Present");
                presentCol.setPrefWidth(80);
                presentCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("presentCount").toString()));

                TableColumn<Map<String, Object>, String> lateCol = new TableColumn<>("Late");
                lateCol.setPrefWidth(80);
                lateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("lateCount").toString()));

                TableColumn<Map<String, Object>, String> absentCol = new TableColumn<>("Absent");
                absentCol.setPrefWidth(80);
                absentCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("absentCount").toString()));

                TableColumn<Map<String, Object>, String> attendanceRateCol = new TableColumn<>("Attendance %");
                attendanceRateCol.setPrefWidth(120);
                attendanceRateCol.setCellValueFactory(param -> {
                        int present = ((Number) param.getValue().get("presentCount")).intValue();
                        int late = ((Number) param.getValue().get("lateCount")).intValue();
                        int total = present + late + ((Number) param.getValue().get("absentCount")).intValue();
                        double rate = total > 0 ? ((present + late) * 100.0) / total : 0;
                        return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", rate));
                });

                table.getColumns().clear();
                table.getColumns()
                                .addAll(java.util.List.of(userCol, presentCol, lateCol, absentCol, attendanceRateCol));
                table.setItems(javafx.collections.FXCollections.observableArrayList(weeklyReport));
                table.setPrefHeight(350);

                // CSV Export Button
                Button exportCsvBtn = new Button("⬇️ Export as CSV");
                exportCsvBtn.setPrefWidth(180);
                exportCsvBtn.setPrefHeight(40);
                exportCsvBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #16a085; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #138d75; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                exportCsvBtn.setOnMouseExited(e -> exportCsvBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #16a085; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
                exportCsvBtn.setOnAction(e -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Report as CSV");
                        fileChooser.getExtensionFilters()
                                        .add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                        java.io.File file = fileChooser.showSaveDialog(reportStage);
                        if (file != null) {
                                try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
                                        // Write header
                                        writer.println("User Name,Present,Late,Absent,Attendance %");
                                        for (Map<String, Object> record : weeklyReport) {
                                                String userName = (String) record.get("userName");
                                                int present = ((Number) record.get("presentCount")).intValue();
                                                int late = ((Number) record.get("lateCount")).intValue();
                                                int absent = record.containsKey("absentCount")
                                                                ? ((Number) record.get("absentCount")).intValue()
                                                                : 0;
                                                int total = present + late + absent;
                                                double rate = total > 0 ? ((present + late) * 100.0) / total : 0;
                                                writer.printf("%s,%d,%d,%d,%.1f%%\n", userName, present, late, absent,
                                                                rate);
                                        }
                                        showAlert("Success", "Report exported successfully as CSV!");
                                } catch (Exception ex) {
                                        showAlert("Error", "Failed to export CSV: " + ex.getMessage());
                                }
                        }
                });

                HBox exportBox = new HBox(exportCsvBtn);
                exportBox.setStyle("-fx-alignment: center; -fx-padding: 10 0 10 0;");

                root.getChildren().addAll(titleLabel, statsBox, reportLabel, table, exportBox);

                reportStage.setScene(new Scene(root));
                reportStage.show();
        }

        /**
         * Show user profile
         */
        private void showUserProfile() {
                Map<String, Object> user = UserManager.getUserById(currentUserId);
                if (user == null || user.isEmpty()) {
                        showAlert("Error", "Unable to load profile details.");
                        return;
                }

                Stage profileStage = new Stage();
                profileStage.setTitle("My Profile");
                profileStage.setWidth(700);
                profileStage.setHeight(620);

                HBox root = new HBox(20);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                VBox facePane = new VBox(12);
                facePane.setPadding(new Insets(18));
                facePane.setPrefWidth(250);
                facePane.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 12; -fx-border-color: #dcdde1; -fx-border-radius: 12;");

                Label faceTitle = new Label("Face Data");
                faceTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label faceStatus = new Label(Boolean.TRUE.equals(user.get("hasFaceData"))
                                ? "Status: Registered"
                                : "Status: Not Registered");
                faceStatus.setStyle("-fx-font-size: 13; -fx-text-fill: #34495e;");

                Button enrollFaceBtn = new Button("Capture / Update Face");
                enrollFaceBtn.setPrefWidth(200);
                enrollFaceBtn.setOnAction(e -> {
                        java.util.concurrent.atomic.AtomicReference<FaceEnrollmentWindow> ref = new java.util.concurrent.atomic.AtomicReference<>();
                        FaceEnrollmentWindow window = new FaceEnrollmentWindow(String.valueOf(user.get("name")), () -> {
                                FaceEnrollmentWindow capturedWindow = ref.get();
                                byte[] captured = capturedWindow == null ? null : capturedWindow.getCapturedFaceData();
                                if (captured != null && captured.length > 0
                                                && UserManager.storeFaceData(currentUserId, captured)) {
                                        faceStatus.setText("Status: Registered");
                                        showAlert("Success", "Face data updated successfully.");
                                } else {
                                        showAlert("Error", "Face update failed. Please try again.");
                                }
                        });
                        ref.set(window);
                });

                facePane.getChildren().addAll(faceTitle, faceStatus, enrollFaceBtn);

                VBox detailsPane = new VBox(10);
                detailsPane.setPadding(new Insets(18));
                detailsPane.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 12; -fx-border-color: #dcdde1; -fx-border-radius: 12;");

                String role = String.valueOf(user.get("role"));
                String roleIdLabel = ValidationUtils.getUserIdLabelForRole(role);

                Label title = new Label("Profile Details");
                title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label nameLbl = new Label("Name: " + user.get("name"));
                Label idLbl = new Label(roleIdLabel + ": " + user.get("id"));
                Label emailLbl = new Label("Email: " + user.get("email"));
                Label roleLbl = new Label("Role: " + role);
                java.util.List<Label> profileLabels = new java.util.ArrayList<>();
                profileLabels.add(nameLbl);
                profileLabels.add(idLbl);
                profileLabels.add(emailLbl);
                profileLabels.add(roleLbl);

                Object batchValue = user.get("batchYear");
                if (batchValue != null) {
                        Label batchLbl = new Label("Batch: " + batchValue);
                        profileLabels.add(batchLbl);
                }

                Object subjectValue = user.get("subject");
                if (subjectValue != null && String.valueOf(subjectValue).trim().length() > 0) {
                        Label subjectLbl = new Label("Subject: " + subjectValue);
                        profileLabels.add(subjectLbl);
                }

                for (Label lbl : profileLabels) {
                        lbl.setStyle("-fx-font-size: 14; -fx-text-fill: #2c3e50;");
                }

                Button resetPasswordFromProfileBtn = new Button("🔐 Change / Reset Password");
                resetPasswordFromProfileBtn.setPrefWidth(220);
                resetPasswordFromProfileBtn.setOnAction(e -> showForgotPasswordDialogForCurrentUser());

                detailsPane.getChildren().add(title);
                detailsPane.getChildren().addAll(profileLabels);
                detailsPane.getChildren().add(resetPasswordFromProfileBtn);

                if ("TEACHER".equalsIgnoreCase(role)) {
                        Label assignmentTitle = new Label("Batch Subject Assignments");
                        assignmentTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                        javafx.scene.control.ListView<String> assignmentList = new javafx.scene.control.ListView<>();
                        assignmentList.setPrefHeight(170);

                        ComboBox<Integer> teacherBatchCombo = new ComboBox<>();
                        for (int year = 2023; year <= 2029; year++) {
                                teacherBatchCombo.getItems().add(year);
                        }
                        teacherBatchCombo.setValue(2023);
                        teacherBatchCombo.setPrefWidth(120);

                        TextField teacherSubjectField = new TextField();
                        teacherSubjectField.setPromptText("Subject for selected batch");
                        teacherSubjectField.setPrefWidth(220);

                        Runnable reloadAssignments = () -> {
                                java.util.List<String> rows = new java.util.ArrayList<>();
                                for (Map<String, Object> row : UserManager.getTeacherBatchSubjects(currentUserId)) {
                                        rows.add(row.get("batchYear") + " -> " + row.get("subject"));
                                }
                                if (rows.isEmpty()) {
                                        rows.add("No batch-subject mapping added yet.");
                                }
                                assignmentList.setItems(javafx.collections.FXCollections.observableArrayList(rows));
                        };

                        Button saveAssignmentBtn = new Button("Save Batch Subject");
                        saveAssignmentBtn.setOnAction(e -> {
                                Integer selectedBatch = teacherBatchCombo.getValue();
                                String subjectText = teacherSubjectField.getText() == null ? ""
                                                : teacherSubjectField.getText().trim();
                                if (selectedBatch == null || subjectText.isEmpty()) {
                                        showAlert("Error", "Select batch and enter subject.");
                                        return;
                                }

                                if (UserManager.upsertTeacherBatchSubject(currentUserId, selectedBatch, subjectText)) {
                                        showAlert("Success", "Batch subject saved successfully.");
                                        teacherSubjectField.clear();
                                        reloadAssignments.run();
                                } else {
                                        showAlert("Error", "Failed to save batch subject.");
                                }
                        });

                        HBox assignmentForm = new HBox(8, teacherBatchCombo, teacherSubjectField, saveAssignmentBtn);
                        assignmentForm.setStyle("-fx-alignment: center-left;");

                        reloadAssignments.run();
                        detailsPane.getChildren().addAll(assignmentTitle, assignmentList, assignmentForm);
                }

                root.getChildren().addAll(facePane, detailsPane);

                profileStage.setScene(new Scene(root));
                profileStage.show();
        }

        /**
         * Quick dashboard action to enroll/update current user's face data.
         */
        private void openFaceEnrollmentForCurrentUser() {
                Map<String, Object> user = UserManager.getUserById(currentUserId);
                String name = user.isEmpty() ? currentUserId : String.valueOf(user.get("name"));
                java.util.concurrent.atomic.AtomicReference<FaceEnrollmentWindow> ref = new java.util.concurrent.atomic.AtomicReference<>();
                FaceEnrollmentWindow window = new FaceEnrollmentWindow(name, () -> {
                        FaceEnrollmentWindow capturedWindow = ref.get();
                        byte[] captured = capturedWindow == null ? null : capturedWindow.getCapturedFaceData();
                        if (captured != null && captured.length > 0
                                        && UserManager.storeFaceData(currentUserId, captured)) {
                                showAlert("Success", "Face data saved successfully.");
                        } else {
                                showAlert("Error", "Face data was not saved.");
                        }
                });
                ref.set(window);
        }

        private Button createDashboardPasswordResetButton(double width, double height) {
                Button resetPasswordBtn = new Button("🔐 Reset Password");
                resetPasswordBtn.setPrefWidth(width);
                resetPasswordBtn.setPrefHeight(height);
                resetPasswordBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                resetPasswordBtn.setOnAction(e -> showForgotPasswordDialogForCurrentUser());
                return resetPasswordBtn;
        }

        /**
         * Counts role-specific notifications for the menu badge.
         */
        private int getNotificationCountForCurrentUser() {
                if (currentUserId == null || currentUserRole == null) {
                        return 0;
                }

                if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
                        return LeaveManager.getPendingLeavesByRequesterRole("TEACHER").size()
                                        + countUsersWithoutFaceData("TEACHER")
                                        + countTodayAbsentByRole("TEACHER");
                }
                if ("TEACHER".equalsIgnoreCase(currentUserRole)) {
                        int count = LeaveManager.getPendingLeavesByRequesterRole("STUDENT").size();
                        if (!Boolean.TRUE.equals(UserManager.getUserById(currentUserId).get("hasFaceData"))) {
                                count++;
                        }
                        count += countTodayAbsentByRole("STUDENT");
                        return count;
                }

                int count = 0;
                for (Map<String, Object> row : LeaveManager.getUserLeaveHistory(currentUserId)) {
                        String status = String.valueOf(row.get("status"));
                        if ("APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
                                count++;
                        }
                }
                if (!Boolean.TRUE.equals(UserManager.getUserById(currentUserId).get("hasFaceData"))) {
                        count++;
                }
                return count;
        }

        /**
         * Shows a simple notifications window for the current user role.
         */
        private void showNotificationsWindow() {
                Stage stage = new Stage();
                stage.setTitle("Notifications");
                stage.setWidth(600);
                stage.setHeight(420);

                VBox root = new VBox(10);
                root.setPadding(new Insets(16));

                javafx.scene.control.ListView<String> listView = new javafx.scene.control.ListView<>();
                java.util.List<String> notifications = new java.util.ArrayList<>();

                if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
                        int pendingTeacherLeaves = LeaveManager.getPendingLeavesByRequesterRole("TEACHER").size();
                        notifications.add("Pending teacher leave requests: " + pendingTeacherLeaves);
                        notifications.add("Teachers without face data: " + countUsersWithoutFaceData("TEACHER"));
                        notifications.add("Teachers absent today: " + countTodayAbsentByRole("TEACHER"));
                } else if ("TEACHER".equalsIgnoreCase(currentUserRole)) {
                        int pendingStudentLeaves = LeaveManager.getPendingLeavesByRequesterRole("STUDENT").size();
                        notifications.add("Pending student leave requests: " + pendingStudentLeaves);
                        if (!Boolean.TRUE.equals(UserManager.getUserById(currentUserId).get("hasFaceData"))) {
                                notifications.add("Your face data is not registered yet.");
                        }
                        notifications.add("Students absent today: " + countTodayAbsentByRole("STUDENT"));
                } else {
                        for (Map<String, Object> row : LeaveManager.getUserLeaveHistory(currentUserId)) {
                                String status = String.valueOf(row.get("status"));
                                if ("APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
                                        notifications.add("Leave " + row.get("startDate") + " to " + row.get("endDate")
                                                        + " is " + status);
                                }
                        }
                        if (!Boolean.TRUE.equals(UserManager.getUserById(currentUserId).get("hasFaceData"))) {
                                notifications.add("Please register your face data to use attendance.");
                        }
                }

                if (notifications.isEmpty()) {
                        notifications.add("No new notifications.");
                }

                listView.setItems(javafx.collections.FXCollections.observableArrayList(notifications));
                root.getChildren().addAll(new Label("Notifications"), listView);

                stage.setScene(new Scene(root));
                stage.show();
        }

        /**
         * Show alert dialog
         */
        private void showAlert(String title, String message) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
        }

        private int countUsersWithoutFaceData(String role) {
                int count = 0;
                for (Map<String, Object> user : UserManager.getAllUsers()) {
                        String userRole = String.valueOf(user.get("role"));
                        if (role != null && !role.equalsIgnoreCase(userRole)) {
                                continue;
                        }
                        if (!Boolean.TRUE.equals(user.get("hasFaceData"))) {
                                count++;
                        }
                }
                return count;
        }

        private int countTodayAbsentByRole(String role) {
                int count = 0;
                for (Map<String, Object> row : AttendanceManager.getTodayAttendanceByRole(role)) {
                        if ("ABSENT".equalsIgnoreCase(String.valueOf(row.get("status")))) {
                                count++;
                        }
                }
                return count;
        }

        /**
         * Show leave request window for students
         */
        private void showLeaveRequestWindow() {
                Stage leaveStage = new Stage();
                leaveStage.setTitle("📋 Request Leave");
                leaveStage.setWidth(600);
                leaveStage.setHeight(700);

                VBox root = new VBox(15);
                root.setPadding(new Insets(25));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label titleLabel = new Label("📋 Request Leave");
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                // Form card
                VBox formCard = new VBox(15);
                formCard.setPadding(new Insets(25));
                formCard.setStyle(
                                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

                Label leaveTypeLabel = new Label("Leave Type:");
                leaveTypeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
                ComboBox<String> leaveTypeCombo = new ComboBox<>();
                leaveTypeCombo.getItems().addAll("SICK", "CASUAL", "EMERGENCY", "PERSONAL");
                leaveTypeCombo.setValue("CASUAL");
                leaveTypeCombo.setStyle("-fx-padding: 8; -fx-font-size: 12; -fx-background-radius: 8;");
                leaveTypeCombo.setPrefWidth(200);

                Label startDateLabel = new Label("Start Date (YYYY-MM-DD):");
                startDateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
                TextField startDateField = new TextField();
                startDateField.setPromptText("2026-02-15");
                startDateField
                                .setStyle("-fx-padding: 10; -fx-font-size: 12; -fx-background-radius: 8; -fx-border-color: #e0e0e0;");

                Label endDateLabel = new Label("End Date (YYYY-MM-DD):");
                endDateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
                TextField endDateField = new TextField();
                endDateField.setPromptText("2026-02-17");
                endDateField
                                .setStyle("-fx-padding: 10; -fx-font-size: 12; -fx-background-radius: 8; -fx-border-color: #e0e0e0;");

                Label reasonLabel = new Label("Reason:");
                reasonLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
                javafx.scene.control.TextArea reasonArea = new javafx.scene.control.TextArea();
                reasonArea.setWrapText(true);
                reasonArea.setPrefRowCount(4);
                reasonArea.setStyle("-fx-padding: 10; -fx-font-size: 12; -fx-background-radius: 8;");
                reasonArea.setPromptText("Provide reason for your leave request...");

                formCard.getChildren().addAll(
                                leaveTypeLabel, leaveTypeCombo,
                                startDateLabel, startDateField,
                                endDateLabel, endDateField,
                                reasonLabel, reasonArea);

                Button submitBtn = new Button("✅ Submit Request");
                submitBtn.setPrefWidth(200);
                submitBtn.setPrefHeight(45);
                submitBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
                submitBtn.setOnMouseEntered(e -> submitBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #229954; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));
                submitBtn.setOnMouseExited(e -> submitBtn.setStyle(
                                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;"));

                submitBtn.setOnAction(e -> {
                        try {
                                if (startDateField.getText().isEmpty() || endDateField.getText().isEmpty()) {
                                        showAlert("Error", "Please fill in all fields");
                                        return;
                                }

                                java.time.LocalDate startDate = java.time.LocalDate.parse(startDateField.getText());
                                java.time.LocalDate endDate = java.time.LocalDate.parse(endDateField.getText());
                                String leaveType = leaveTypeCombo.getValue();
                                String reason = reasonArea.getText();

                                if (LeaveManager.requestLeave(currentUserId, startDate, endDate, reason, leaveType)) {
                                        showAlert("Success", "Leave request submitted successfully!");
                                        leaveStage.close();
                                } else {
                                        showAlert("Error", "Failed to submit leave request. Please try again.");
                                }
                        } catch (Exception ex) {
                                showAlert("Error", "Invalid date format. Use YYYY-MM-DD");
                        }
                });

                HBox buttonBox = new HBox(submitBtn);
                buttonBox.setStyle("-fx-alignment: center; -fx-padding: 10 0 0 0;");

                root.getChildren().addAll(titleLabel, formCard, buttonBox);
                leaveStage.setScene(new Scene(root));
                leaveStage.show();
        }

        /**
         * Show leave approval window filtered by requester role.
         *
         * @param requesterRoleFilter Role of users whose leave requests are shown
         *                            (e.g., STUDENT, TEACHER). If null/blank, all
         *                            pending leaves are shown.
         * @param windowTitle         Window title text
         */
        private void showLeaveApprovalWindow(String requesterRoleFilter, String windowTitle) {
                Stage approvalStage = new Stage();
                approvalStage.setTitle(windowTitle);
                approvalStage.setWidth(1000);
                approvalStage.setHeight(700);

                final String roleFilter = requesterRoleFilter != null ? requesterRoleFilter.trim() : "";

                VBox root = new VBox(15);
                root.setPadding(new Insets(25));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label titleLabel = new Label("✅ Pending Leave Requests" +
                                (roleFilter.isEmpty() ? "" : " (" + roleFilter + ")"));
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                // Create table for pending leaves
                TableView<Map<String, Object>> leaveTable = new TableView<>();

                TableColumn<Map<String, Object>, String> userCol = new TableColumn<>("User");
                userCol.setPrefWidth(100);
                userCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userName")));

                TableColumn<Map<String, Object>, String> typeCol = new TableColumn<>("Leave Type");
                typeCol.setPrefWidth(100);
                typeCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("leaveType")));

                TableColumn<Map<String, Object>, String> daysCol = new TableColumn<>("Days");
                daysCol.setPrefWidth(60);
                daysCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("days").toString()));

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Period");
                dateCol.setPrefWidth(150);
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("startDate") + " to " + param.getValue().get("endDate")));

                TableColumn<Map<String, Object>, String> reasonCol = new TableColumn<>("Reason");
                reasonCol.setPrefWidth(200);
                reasonCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("reason")));

                leaveTable.getColumns().addAll(java.util.List.of(userCol, typeCol, daysCol, dateCol, reasonCol));
                java.util.List<Map<String, Object>> initialLeaves = roleFilter.isEmpty()
                                ? LeaveManager.getPendingLeaves()
                                : LeaveManager.getPendingLeavesByRequesterRole(roleFilter);
                leaveTable.setItems(javafx.collections.FXCollections.observableArrayList(initialLeaves));
                leaveTable.setPrefHeight(400);

                // Action buttons for selected row
                HBox actionBox = new HBox(10);
                actionBox.setStyle("-fx-alignment: center; -fx-padding: 15 0 0 0;");

                Button approveBtn = new Button("✅ Approve");
                approveBtn.setPrefWidth(120);
                approveBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8;");
                approveBtn.setOnAction(e -> {
                        Map<String, Object> selected = leaveTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                                int leaveId = (int) selected.get("leaveId");
                                if (LeaveManager.approveLeave(leaveId)) {
                                        showAlert("Success", "Leave approved successfully!");
                                        java.util.List<Map<String, Object>> refreshed = roleFilter.isEmpty()
                                                        ? LeaveManager.getPendingLeaves()
                                                        : LeaveManager.getPendingLeavesByRequesterRole(roleFilter);
                                        leaveTable.setItems(javafx.collections.FXCollections.observableArrayList(
                                                        refreshed));
                                } else {
                                        showAlert("Error", "Failed to approve leave");
                                }
                        } else {
                                showAlert("Warning", "Please select a leave request");
                        }
                });

                Button rejectBtn = new Button("❌ Reject");
                rejectBtn.setPrefWidth(120);
                rejectBtn.setStyle(
                                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8;");
                rejectBtn.setOnAction(e -> {
                        Map<String, Object> selected = leaveTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                                int leaveId = (int) selected.get("leaveId");
                                Dialog<String> dialog = new Dialog<>();
                                dialog.setTitle("Rejection Reason");
                                dialog.setHeaderText("Provide reason for rejection:");
                                javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea();
                                textArea.setPrefRowCount(4);
                                dialog.getDialogPane().setContent(textArea);
                                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                                dialog.setResultConverter(
                                                buttonType -> buttonType == ButtonType.OK ? textArea.getText() : null);

                                var result = dialog.showAndWait();
                                if (result.isPresent()) {
                                        if (LeaveManager.rejectLeave(leaveId, result.get())) {
                                                showAlert("Success", "Leave rejected successfully!");
                                                java.util.List<Map<String, Object>> refreshed = roleFilter.isEmpty()
                                                                ? LeaveManager.getPendingLeaves()
                                                                : LeaveManager.getPendingLeavesByRequesterRole(
                                                                                roleFilter);
                                                leaveTable.setItems(
                                                                javafx.collections.FXCollections.observableArrayList(
                                                                                refreshed));
                                        } else {
                                                showAlert("Error", "Failed to reject leave");
                                        }
                                }
                        } else {
                                showAlert("Warning", "Please select a leave request");
                        }
                });

                actionBox.getChildren().addAll(approveBtn, rejectBtn);

                root.getChildren().addAll(titleLabel, leaveTable, actionBox);
                approvalStage.setScene(new Scene(root));
                approvalStage.show();
        }

        /**
         * Show user's leave history
         */
        private void showUserLeaveHistory() {
                Stage historyStage = new Stage();
                historyStage.setTitle("📋 My Leave History");
                historyStage.setWidth(1200);
                historyStage.setHeight(600);

                VBox root = new VBox(15);
                root.setPadding(new Insets(25));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label titleLabel = new Label("📋 Leave History");
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                TableView<Map<String, Object>> leaveTable = new TableView<>();

                TableColumn<Map<String, Object>, String> typeCol = new TableColumn<>("Type");
                typeCol.setPrefWidth(80);
                typeCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("leaveType")));

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Period");
                dateCol.setPrefWidth(150);
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("startDate") + " to " + param.getValue().get("endDate")));

                TableColumn<Map<String, Object>, String> daysCol = new TableColumn<>("Days");
                daysCol.setPrefWidth(60);
                daysCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("days").toString()));

                TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
                statusCol.setPrefWidth(100);
                statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("status")));

                TableColumn<Map<String, Object>, String> requestDateCol = new TableColumn<>("Requested On");
                requestDateCol.setPrefWidth(130);
                requestDateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("requestDate"))));

                dateCol.setCellFactory(col -> {
                        javafx.scene.control.TableCell<Map<String, Object>, String> cell = new javafx.scene.control.TableCell<>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                        super.updateItem(item, empty);
                                        setText(empty ? null : item);
                                        if (empty) {
                                                setStyle("");
                                        } else {
                                                setStyle("-fx-text-fill: #2c3e50; -fx-underline: true;");
                                        }
                                }
                        };
                        cell.setOnMouseClicked(event -> {
                                if (!cell.isEmpty()) {
                                        leaveTable.getSelectionModel().select(cell.getIndex());
                                }
                        });
                        return cell;
                });

                leaveTable.getColumns().addAll(
                                java.util.List.of(typeCol, dateCol, daysCol, statusCol, requestDateCol));
                leaveTable.setItems(javafx.collections.FXCollections.observableArrayList(
                                LeaveManager.getUserLeaveHistory(currentUserId)));
                leaveTable.setPrefHeight(450);

                Label hintLabel = new Label("Click a period/date row to view full reason below.");
                hintLabel.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12;");

                VBox detailsDrawer = new VBox(8);
                detailsDrawer.setPadding(new Insets(12));
                detailsDrawer.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 10; -fx-border-color: #dfe6e9; -fx-border-radius: 10;");
                detailsDrawer.setVisible(false);
                detailsDrawer.setManaged(false);

                Label detailsHeader = new Label("Leave Details");
                detailsHeader.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label reasonLabel = new Label("Reason:");
                reasonLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                javafx.scene.control.TextArea reasonArea = new javafx.scene.control.TextArea();
                reasonArea.setEditable(false);
                reasonArea.setWrapText(true);
                reasonArea.setPrefRowCount(3);

                Label rejectionLabel = new Label("Rejection Reason:");
                rejectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                javafx.scene.control.TextArea rejectionArea = new javafx.scene.control.TextArea();
                rejectionArea.setEditable(false);
                rejectionArea.setWrapText(true);
                rejectionArea.setPrefRowCount(2);

                detailsDrawer.getChildren().addAll(detailsHeader, reasonLabel, reasonArea, rejectionLabel,
                                rejectionArea);

                leaveTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, selected) -> {
                        if (selected == null) {
                                detailsDrawer.setVisible(false);
                                detailsDrawer.setManaged(false);
                                return;
                        }

                        String period = String.valueOf(selected.get("startDate")) + " to "
                                        + String.valueOf(selected.get("endDate"));
                        String status = String.valueOf(selected.get("status"));
                        detailsHeader.setText("Leave Details - " + period + " (" + status + ")");

                        Object reasonValue = selected.get("reason");
                        Object rejectionValue = selected.get("rejectionReason");
                        reasonArea.setText(reasonValue != null && !reasonValue.toString().isBlank()
                                        ? reasonValue.toString()
                                        : "-");
                        rejectionArea
                                        .setText(rejectionValue != null && !rejectionValue.toString().isBlank()
                                                        ? rejectionValue.toString()
                                                        : "N/A");

                        detailsDrawer.setVisible(true);
                        detailsDrawer.setManaged(true);
                });

                root.getChildren().addAll(titleLabel, leaveTable, hintLabel, detailsDrawer);
                historyStage.setScene(new Scene(root));
                historyStage.show();
        }

        /**
         * Show all student leave history (admin view)
         */
        private void showStudentLeaveHistory() {
                Stage historyStage = new Stage();
                historyStage.setTitle("📋 Student Leave History");
                historyStage.setWidth(1100);
                historyStage.setHeight(650);

                VBox root = new VBox(15);
                root.setPadding(new Insets(25));
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

                Label titleLabel = new Label("📋 Student Leave History");
                titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                TableView<Map<String, Object>> leaveTable = new TableView<>();

                TableColumn<Map<String, Object>, String> userIdCol = new TableColumn<>("User ID");
                userIdCol.setPrefWidth(120);
                userIdCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userId")));

                TableColumn<Map<String, Object>, String> userNameCol = new TableColumn<>("Name");
                userNameCol.setPrefWidth(160);
                userNameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("userName")));

                TableColumn<Map<String, Object>, String> typeCol = new TableColumn<>("Type");
                typeCol.setPrefWidth(90);
                typeCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("leaveType")));

                TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("Period");
                dateCol.setPrefWidth(180);
                dateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                param.getValue().get("startDate") + " to " + param.getValue().get("endDate")));

                TableColumn<Map<String, Object>, String> daysCol = new TableColumn<>("Days");
                daysCol.setPrefWidth(70);
                daysCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                String.valueOf(param.getValue().get("days"))));

                TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
                statusCol.setPrefWidth(100);
                statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("status")));

                TableColumn<Map<String, Object>, String> reasonCol = new TableColumn<>("Reason");
                reasonCol.setPrefWidth(300);
                reasonCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                                (String) param.getValue().get("reason")));

                leaveTable.getColumns().addAll(java.util.List.of(
                                userIdCol, userNameCol, typeCol, dateCol, daysCol, statusCol, reasonCol));
                leaveTable.setItems(javafx.collections.FXCollections.observableArrayList(
                                LeaveManager.getStudentLeaveHistory()));
                leaveTable.setPrefHeight(500);

                root.getChildren().addAll(titleLabel, leaveTable);
                historyStage.setScene(new Scene(root));
                historyStage.show();
        }

        public static void main(String[] args) {
                // Suppress noisy JavaFX CSS warnings
                java.util.logging.Logger.getLogger("javafx.scene.CssStyleHelper")
                                .setLevel(java.util.logging.Level.SEVERE);
                java.util.logging.Logger.getLogger("com.sun.javafx.css").setLevel(java.util.logging.Level.SEVERE);
                java.util.logging.Logger.getLogger("javafx.css").setLevel(java.util.logging.Level.SEVERE);
                java.util.logging.Logger.getLogger("javafx.scene").setLevel(java.util.logging.Level.SEVERE);
                java.util.logging.Logger.getLogger("javafx").setLevel(java.util.logging.Level.SEVERE);

                System.out.println("=======================================================");
                System.out.println("   Face Recognition Attendance System v1.0");
                System.out.println("=======================================================");

                AppLogger.info("Application started");
                AppLogger.info("Java Version: " + System.getProperty("java.version"));
                AppLogger.info("OS: " + System.getProperty("os.name"));

                // Initialize and test database
                System.out.println("\n[INFO] DATABASE INITIALIZATION");
                System.out.println("-----------------------------------------------------");
                try {
                        AppLogger.info("Connecting to database...");
                        DatabaseConnection.initializeDatabase();
                        if (DatabaseConnection.testConnection()) {
                                System.out.println("[OK] Database connected successfully!");
                                AppLogger.info("Database connection established");
                        } else {
                                System.out.println("[ERROR] Database connection test failed");
                                AppLogger.error("Database connection test failed");
                        }
                } catch (java.sql.SQLException e) {
                        System.err.println("[ERROR] Failed to initialize database: " + e.getMessage());
                        AppLogger.error("Failed to initialize database", e);
                        System.exit(1);
                }

                // Load configuration
                System.out.println("\n[INFO] CONFIGURATION LOADED");
                System.out.println("-----------------------------------------------------");
                System.out.println("[OK] App Name: " + AppConfig.APP_NAME);
                System.out.println("[OK] Version: " + AppConfig.APP_VERSION);
                System.out.println("[OK] Camera Index: " + AppConfig.DEFAULT_CAMERA_INDEX);
                System.out.println("[OK] Face Confidence: " + AppConfig.CONFIDENCE_THRESHOLD * 100 + "%");
                System.out.println(
                                "[OK] Database: " + AppConfig.SQLSERVER_DB_NAME + " (" + AppConfig.SQLSERVER_HOST
                                                + ")");

                System.out.println("\n[INFO] LAUNCHING USER INTERFACE");
                System.out.println("-----------------------------------------------------");
                AppLogger.info("Launching JavaFX application");
                launch(args);
        }
}
