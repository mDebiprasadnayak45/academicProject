import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Camera Window for Real-time Face Detection and Recognition using OpenCV.
 * Runs camera capture in a background thread to prevent UI freezing.
 * Displays live video feed with face detection overlays and allows
 * capturing faces for attendance marking.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class CameraWindow {
    private Stage stage;
    private javafx.scene.image.ImageView imageView;
    private Label statusLabel;
    private Button captureButton;
    private Button toggleButton;
    private FaceDetectionEngine faceDetector;
    private FrameGrabber frameGrabber;
    private OpenCVFrameConverter.ToMat converter;
    private Rect smoothedFace;
    private static final double FACE_SMOOTHING = 0.6;

    /** Flag indicating if camera is currently running */
    private boolean isRunning = false;

    /** Current user ID for attendance marking */
    private String currentUserId;

    /** Background thread for camera capture loop */
    private Thread cameraThread;

    /**
     * Constructs a new CameraWindow for the specified user.
     * Initializes OpenCV, sets up the UI, and automatically starts the camera.
     * 
     * @param userId The ID of the user operating the camera
     */
    public CameraWindow(String userId) {
        this.currentUserId = userId;

        // Bytedeco automatically loads OpenCV natives from opencv-platform.jar
        // No manual Loader.load() needed - it causes UnsatisfiedLinkError

        this.faceDetector = new FaceDetectionEngine();
        this.converter = new OpenCVFrameConverter.ToMat();
        initializeWindow();

        // Auto-start camera on window creation
        Platform.runLater(() -> {
            if (!isRunning) {
                toggleCamera();
            }
        });
    }

    /**
     * Initializes the JavaFX window UI with all controls and layout.
     * Sets up image view, status label, and control buttons.
     */
    private void initializeWindow() {
        stage = new Stage();
        stage.setTitle("🎬 Face Detection - " + currentUserId);
        stage.setWidth(AppConfig.CAMERA_WINDOW_WIDTH);
        stage.setHeight(AppConfig.CAMERA_WINDOW_HEIGHT);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

        Label titleLabel = new Label("📹 Real-time Face Detection");
        titleLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label userLabel = new Label("👤 User: " + currentUserId);
        userLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 10 0;");

        // Camera view with card styling
        VBox cameraCard = new VBox(10);
        cameraCard.setStyle(
                "-fx-background-color: black; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");
        cameraCard.setPadding(new Insets(10));

        imageView = new ImageView();
        imageView.setFitWidth(AppConfig.CAMERA_WIDTH);
        imageView.setFitHeight(AppConfig.CAMERA_HEIGHT);
        imageView.setStyle("-fx-background-radius: 10;");

        cameraCard.getChildren().add(imageView);

        statusLabel = new Label("⏸ ⏸ Ready to start camera");
        statusLabel.setStyle(
                "-fx-font-size: 13; -fx-font-weight: 600; -fx-padding: 12; -fx-background-color: white; -fx-background-radius: 8; -fx-text-fill: #667eea; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        HBox buttonBox = new HBox(15);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 10 0 0 0;");

        toggleButton = new Button("▶️ Start Camera");
        toggleButton.setPrefWidth(140);
        toggleButton.setPrefHeight(45);
        toggleButton.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        toggleButton.setOnMouseEntered(e -> {
            if (!toggleButton.isDisabled()) {
                toggleButton.setStyle(
                        "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #0e877d; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");
            }
        });
        toggleButton.setOnMouseExited(e -> {
            if (!toggleButton.isDisabled()) {
                String color = isRunning ? "#e74c3c" : "#11998e";
                String text = isRunning ? "⏹️ Stop Camera" : "▶️ Start Camera";
                toggleButton.setText(text);
                toggleButton.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: " + color
                        + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
            }
        });
        toggleButton.setOnAction(e -> toggleCamera());

        captureButton = new Button("📸 Capture Attendance");
        captureButton.setPrefWidth(200);
        captureButton.setPrefHeight(45);
        captureButton.setDisable(true);
        captureButton.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        captureButton.setOnMouseEntered(e -> {
            if (!captureButton.isDisabled()) {
                captureButton.setStyle(
                        "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #5568d3; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");
            }
        });
        captureButton.setOnMouseExited(e -> {
            if (!captureButton.isDisabled()) {
                captureButton.setStyle(
                        "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
            }
        });
        captureButton.setOnAction(e -> captureFaceForAttendance());

        Button exitButton = new Button("❌ Exit");
        exitButton.setPrefWidth(110);
        exitButton.setPrefHeight(45);
        exitButton.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        exitButton.setOnMouseEntered(e -> exitButton.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);"));
        exitButton.setOnMouseExited(e -> exitButton.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        exitButton.setOnAction(e -> {
            stopCamera();
            if (stage != null) {
                stage.close();
            }
        });

        buttonBox.getChildren().addAll(toggleButton, captureButton, exitButton);

        root.getChildren().addAll(titleLabel, userLabel, cameraCard, statusLabel, buttonBox);

        // Wrap root in ScrollPane for scrollability
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

        Scene scene = new Scene(scrollPane);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> stopCamera());
        stage.show();
    }

    /**
     * Toggles camera on/off based on current state.
     * Starts camera in a separate thread to avoid UI blocking.
     */
    private synchronized void toggleCamera() {
        if (!isRunning) {
            toggleButton.setText("⏹️ Stop Camera");
            toggleButton.setStyle(
                    "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
            toggleButton.setDisable(true);
            captureButton.setDisable(true);
            updateStatus("⏳ Starting camera...");

            // Start camera in background thread to avoid UI freezing
            Thread startThread = new Thread(() -> {
                boolean success = startCamera();
                Platform.runLater(() -> {
                    toggleButton.setDisable(false);
                    if (success) {
                        captureButton.setDisable(false);
                    } else {
                        toggleButton.setText("Start Camera");
                        toggleButton.setStyle(
                                "-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #27ae60; -fx-text-fill: white;");
                    }
                });
            });
            startThread.setDaemon(true);
            startThread.start();
        } else {
            stopCamera();
        }
    }

    /**
     * Starts the camera and initializes the capture loop in a background thread.
     * Prevents UI thread blocking by running camera operations asynchronously.
     * 
     * @return true if camera started successfully, false otherwise
     */
    private boolean startCamera() {
        try {
            System.out.println("[INFO] Starting OpenCV camera...");
            updateStatus("⚙️ Initializing OpenCV...");

            // OpenCV already pre-loaded in constructor

            frameGrabber = createAndStartGrabber(AppConfig.DEFAULT_CAMERA_INDEX);

            System.out.println("[OK] Camera started");
            updateStatus("✅ Camera active - Face detection ready");

            isRunning = true;

            // Create dedicated background thread for camera capture loop
            cameraThread = new Thread(() -> captureFrames());
            cameraThread.setDaemon(true);
            cameraThread.setName("CameraCapture");
            cameraThread.start();

            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] Camera failed: " + e.getMessage());
            e.printStackTrace();

            // Log error to file for debugging
            String errorMsg = "Camera Error: " + e.getClass().getName() + " - " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += "\nCaused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage();
            }
            AppLogger.error(errorMsg);

            // Build complete error message
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("Failed to start camera:\n\n");
            errorBuilder.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            if (e.getCause() != null) {
                errorBuilder.append("\n\nCaused by:\n");
                errorBuilder.append(e.getCause().getClass().getSimpleName()).append(": ")
                        .append(e.getCause().getMessage());
            }
            final String finalMsg = errorBuilder.toString();

            updateStatus("ERROR: " + e.getMessage());
            Platform.runLater(() -> showErrorDialog("Camera Error", finalMsg));
            return false;
        }
    }

    private FrameGrabber createAndStartGrabber(int cameraIndex) throws FrameGrabber.Exception {
        FrameGrabber grabber = new OpenCVFrameGrabber(cameraIndex);
        grabber.setImageWidth(AppConfig.CAMERA_WIDTH);
        grabber.setImageHeight(AppConfig.CAMERA_HEIGHT);
        grabber.setFrameRate(AppConfig.CAMERA_FPS);
        grabber.start();

        Frame testFrame = tryGrabFrame(grabber, 3);
        if (testFrame != null) {
            return grabber;
        }

        System.out.println("[WARN] OpenCV grabber returned no frames. Falling back to VideoInput.");
        grabber.stop();
        grabber.release();

        grabber = new VideoInputFrameGrabber(cameraIndex);
        grabber.setImageWidth(AppConfig.CAMERA_WIDTH);
        grabber.setImageHeight(AppConfig.CAMERA_HEIGHT);
        grabber.setFrameRate(AppConfig.CAMERA_FPS);
        grabber.start();

        testFrame = tryGrabFrame(grabber, 3);
        if (testFrame == null) {
            grabber.stop();
            grabber.release();
            throw new FrameGrabber.Exception("No frames received from camera.");
        }

        return grabber;
    }

    private Frame tryGrabFrame(FrameGrabber grabber, int attempts) throws FrameGrabber.Exception {
        for (int i = 0; i < attempts; i++) {
            Frame frame = grabber.grab();
            if (frame != null) {
                return frame;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /**
     * Main camera capture loop running in background thread.
     * Continuously grabs frames, detects faces, and updates the UI.
     * Runs at approximately 30 FPS to provide smooth video display.
     */
    private void captureFrames() {
        try {
            while (isRunning) {
                try {
                    Frame frame = frameGrabber.grab();
                    if (frame == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    Mat mat = converter.convert(frame);
                    if (mat != null && !mat.empty()) {
                        List<Rect> faces = faceDetector.detectFaces(mat);

                        if (!faces.isEmpty()) {
                            Rect primaryFace = selectPrimaryFace(faces);
                            Rect smoothed = smoothFaceRect(primaryFace);
                            List<Rect> drawFaces = new ArrayList<>(1);
                            if (smoothed != null) {
                                drawFaces.add(smoothed);
                            }
                            faceDetector.drawBoundingBoxes(mat, drawFaces);
                            updateStatus("[OK] Detected " + faces.size() + " face(s)");
                        } else {
                            smoothedFace = null;
                            updateStatus("Searching for faces...");
                        }

                        BufferedImage bufferedImage = faceDetector.convertMatToBufferedImage(mat);
                        if (bufferedImage != null) {
                            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                            Platform.runLater(() -> imageView.setImage(image));
                        }
                    }

                    if (mat != null) {
                        mat.release();
                    }

                    // Sleep to maintain ~30 FPS
                    Thread.sleep(33);

                } catch (InterruptedException ie) {
                    if (!isRunning)
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Camera thread error: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Camera error: " + e.getMessage());
            isRunning = false;
        } finally {
            if (frameGrabber != null) {
                try {
                    frameGrabber.stop();
                    frameGrabber.release();
                    System.out.println("[INFO] Camera released");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            isRunning = false;
        }
    }

    private Rect selectPrimaryFace(List<Rect> faces) {
        Rect best = faces.get(0);
        int bestArea = best.width() * best.height();
        for (int i = 1; i < faces.size(); i++) {
            Rect candidate = faces.get(i);
            int area = candidate.width() * candidate.height();
            if (area > bestArea) {
                best = candidate;
                bestArea = area;
            }
        }
        return best;
    }

    private Rect smoothFaceRect(Rect current) {
        if (current == null) {
            return null;
        }
        if (smoothedFace == null) {
            smoothedFace = new Rect(current.x(), current.y(), current.width(), current.height());
            return smoothedFace;
        }
        int x = (int) Math.round(FACE_SMOOTHING * current.x() + (1.0 - FACE_SMOOTHING) * smoothedFace.x());
        int y = (int) Math.round(FACE_SMOOTHING * current.y() + (1.0 - FACE_SMOOTHING) * smoothedFace.y());
        int w = (int) Math.round(FACE_SMOOTHING * current.width() + (1.0 - FACE_SMOOTHING) * smoothedFace.width());
        int h = (int) Math.round(FACE_SMOOTHING * current.height() + (1.0 - FACE_SMOOTHING) * smoothedFace.height());
        smoothedFace = new Rect(x, y, w, h);
        return smoothedFace;
    }

    /**
     * Stops the camera and releases all resources.
     * Waits for the camera thread to terminate gracefully.
     */
    private synchronized void stopCamera() {
        isRunning = false;
        if (cameraThread != null) {
            try {
                cameraThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        updateStatus("[OK] Camera stopped");
        toggleButton.setText("Start Camera");
        toggleButton
                .setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #27ae60; -fx-text-fill: white;");
        captureButton.setDisable(true);

        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Captures the current frame, detects and recognizes faces,
     * and marks attendance if a face is successfully recognized.
     */
    /**
     * PROPER FACE REGISTRATION PIPELINE:
     * Captures a face from camera, encodes it properly, and stores in database.
     * This is the ONLY way to register a student's face for recognition.
     * 
     * Workflow:
     * 1. Capture frame from camera
     * 2. Detect face
     * 3. Extract and normalize 100x100 grayscale
     * 4. ENCODE using OpenCV imencode (proper serialization)
     * 5. Store encoded bytes in database
     * 
     * @param userId The student ID to register face for
     * @return true if registration successful, false otherwise
     */
    public boolean registerFaceForStudent(String userId) {
        try {
            if (frameGrabber == null) {
                updateStatus("ERROR: Camera not active for registration");
                return false;
            }

            // STEP 1: Capture frame
            Frame frame = frameGrabber.grab();
            if (frame == null) {
                updateStatus("ERROR: Could not grab frame for registration");
                return false;
            }

            // STEP 2: Detect face
            Mat mat = converter.convert(frame);
            List<org.bytedeco.opencv.opencv_core.Rect> faces = faceDetector.detectFaces(mat);

            if (faces.isEmpty()) {
                updateStatus("[ERROR] Registration failed: No face detected!");
                return false;
            }

            // STEP 3: Extract and normalize face (100x100 grayscale)
            Mat detectedFace = faceDetector.extractFaceRegion(mat, faces.get(0));
            if (detectedFace == null || detectedFace.empty()) {
                updateStatus("ERROR: Could not extract face region");
                return false;
            }

            // STEP 4: ENCODE face data using OpenCV imencode (proper serialization)
            byte[] encodedFaceData = FaceDetectionEngine.encodeFaceData(detectedFace);
            if (encodedFaceData == null || encodedFaceData.length == 0) {
                updateStatus("ERROR: Face encoding failed");
                detectedFace.release();
                return false;
            }

            // STEP 5: Store in database
            boolean stored = UserManager.storeFaceData(userId, encodedFaceData);
            if (!stored) {
                updateStatus("ERROR: Failed to store face data in database");
                detectedFace.release();
                return false;
            }

            detectedFace.release();
            System.out.println("[OK] FACE REGISTRATION SUCCESSFUL");
            System.out.println("[OK] StudentId: " + userId);
            System.out.println("[OK] Face size: 100x100 grayscale");
            System.out.println("[OK] Encoded size: " + encodedFaceData.length + " bytes");
            System.out.println("[OK] Stored in database");

            updateStatus("[OK] Face registered successfully for student: " + userId);
            return true;

        } catch (Exception e) {
            System.err.println("[ERROR] Face registration error: " + e.getMessage());
            e.printStackTrace();
            updateStatus("ERROR: " + e.getMessage());
            return false;
        }
    }

    private void captureFaceForAttendance() {
        try {
            if (frameGrabber == null) {
                updateStatus("ERROR: Camera not active");
                return;
            }

            Frame frame = frameGrabber.grab();
            if (frame == null) {
                updateStatus("ERROR: Could not grab frame");
                return;
            }

            Mat mat = converter.convert(frame);
            List<org.bytedeco.opencv.opencv_core.Rect> faces = faceDetector.detectFaces(mat);

            if (faces.isEmpty()) {
                updateStatus("[ERROR] No face detected! Ensure your face is visible.");
                return;
            }

            Mat detectedFace = faceDetector.extractFaceRegion(mat, faces.get(0));
            if (detectedFace == null || detectedFace.empty()) {
                updateStatus("ERROR: Could not extract face region");
                return;
            }

            List<UserFaceData> registeredUsers = loadRegisteredUsers();
            if (registeredUsers.isEmpty()) {
                updateStatus("[ERROR] No registered face data found. Please register first.");
                return;
            }
            IdentificationResult result = faceDetector.identifyUser(detectedFace, registeredUsers);

            System.out.println("[DEBUG] Face recognition result: isMatch=" + result.isMatch()
                    + ", userId=" + result.getUserId()
                    + ", userName=" + result.getUserName()
                    + ", confidenceScore=" + result.getConfidenceScore());

            if (result.isMatch()) {
                System.out.println("[DEBUG] Attempting to mark attendance for userId=" + result.getUserId()
                        + ", confidenceScore=" + result.getConfidenceScore());
                boolean success = AttendanceManager.markAttendance(result.getUserId(), result.getConfidenceScore(),
                        true);
                if (success) {
                    System.out.println("[DEBUG] Attendance marked successfully for userId=" + result.getUserId());
                    updateStatus("[OK] Attendance: " + result.getUserName() +
                            " (Confidence: " + String.format("%.1f", result.getConfidenceScore() * 100) + "%)");
                } else {
                    boolean checkOutSuccess = AttendanceManager.markCheckOut(result.getUserId());
                    if (checkOutSuccess) {
                        System.out.println("[DEBUG] Check-out marked for userId=" + result.getUserId());
                        updateStatus("[OK] Check-out marked: " + result.getUserName());
                    } else {
                        System.out.println("[DEBUG] Attendance marking failed for userId=" + result.getUserId()
                                + " (possible reasons: already marked+checked-out, leave approved, or DB issue)");
                        updateStatus(
                                "[ERROR] Attendance not marked (already checked in/out / invalid time / leave / DB issue)");
                    }
                }
            } else {
                System.out.println("[DEBUG] Face not recognized. Confidence: " + result.getConfidenceScore());
                updateStatus("[ERROR] Face not recognized (Confidence: " +
                        String.format("%.1f", result.getConfidenceScore() * 100) + "%)");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Capture error: " + e.getMessage());
            e.printStackTrace();
            updateStatus("ERROR: " + e.getMessage());
        }
    }

    /**
     * Loads all registered users with their face data from the database.
     * 
     * @return List of UserFaceData objects for all registered users
     */
    /**
     * CRITICAL METHOD FOR FACE RECOGNITION:
     * Loads all registered students with their stored face data from the database.
     * Uses proper OpenCV imdecode to deserialize face data.
     * The face data is essential for face recognition to work properly.
     * 
     * @return List of UserFaceData objects with valid face data loaded from
     *         database
     */
    private List<UserFaceData> loadRegisteredUsers() {
        List<UserFaceData> users = new ArrayList<>();

        try {
            // Query database for all active users and their face_data
            try (java.sql.Connection conn = DatabaseConnection.getConnection();
                    java.sql.Statement stmt = conn.createStatement();
                    java.sql.ResultSet rs = stmt.executeQuery(
                            "SELECT id, name, face_data FROM users WHERE is_active = 1 AND face_data IS NOT NULL ORDER BY name")) {

                while (rs.next()) {
                    String userId = rs.getString("id");
                    String userName = rs.getString("name");
                    byte[] faceDataBytes = rs.getBytes("face_data");

                    if (faceDataBytes != null && faceDataBytes.length > 0) {
                        try {
                            // CRITICAL FIX: Use FaceDetectionEngine.decodeFaceData() for proper
                            // deserialization
                            // This uses OpenCV imdecode instead of broken Mat.createFrom()
                            org.bytedeco.opencv.opencv_core.Mat faceData = FaceDetectionEngine
                                    .decodeFaceData(faceDataBytes);

                            if (faceData != null && faceData.rows() == 100 && faceData.cols() == 100) {
                                users.add(new UserFaceData(userId, userName, faceData));
                                System.out.println("[OK] Loaded face data for: " + userName + " (ID: " + userId +
                                        ") - 100x100 grayscale verified");
                            } else {
                                System.out.println("[!] Face data invalid or wrong size for " + userName);
                            }
                        } catch (Exception e) {
                            System.err.println("[ERROR] Failed to decode face data for " + userId +
                                    ": " + e.getMessage());
                        }
                    } else {
                        System.out.println("[!] No face data stored for student: " + userName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load registered users: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[INFO] Successfully loaded " + users.size() + " students with valid face data");
        return users;
    }

    /**
     * Updates the status label text from any thread (thread-safe).
     * 
     * @param message The status message to display
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    /**
     * Displays an error dialog to the user.
     * 
     * @param title   Dialog title
     * @param message Error message to display
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}