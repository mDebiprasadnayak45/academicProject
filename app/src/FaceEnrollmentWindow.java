import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
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

public class FaceEnrollmentWindow {
    private Stage stage;
    private ImageView imageView;
    private Label statusLabel;
    private Button captureButton;
    private Button confirmButton;
    private Button cancelButton;
    private Button previewButton;
    private Button retryButton;
    private HBox buttonBox;
    private FaceDetectionEngine faceDetector;
    private FrameGrabber frameGrabber;
    private OpenCVFrameConverter.ToMat converter;
    private boolean isRunning = false;
    private Thread cameraThread;
    private AtomicReference<Mat> capturedFaceData = new AtomicReference<>();
    private Runnable onEnrollmentComplete;

    public FaceEnrollmentWindow(String userName, Runnable onComplete) {
        this.onEnrollmentComplete = onComplete;
        this.faceDetector = new FaceDetectionEngine();
        this.converter = new OpenCVFrameConverter.ToMat();
        initializeWindow(userName);
        startCamera();
    }

    private void initializeWindow(String userName) {
        stage = new Stage();
        stage.setTitle("✨ Face Enrollment - " + userName);
        stage.setWidth(800);
        stage.setHeight(850);
        stage.setResizable(true); // Allow resizing for better accessibility

        VBox root = new VBox(6); // Reduce spacing further for more compact layout
        root.setPadding(new Insets(18, 40, 18, 40)); // Less top/bottom padding for higher alignment
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa 0%, #c3cfe2 100%);");

        Label titleLabel = new Label("📸 Face Enrollment Process");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label nameLabel = new Label("👤 " + userName);
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #7f8c8d; -fx-padding: 0 0 10 0;");

        Label instructions = new Label(
                "ℹ️ Align your face inside the box.\n" +
                        "💡 Ensure good lighting and your face is fully visible.\n" +
                        "✅ Click 'Capture Face' or hold still for auto-capture.");
        instructions.setStyle(
                "-fx-font-size: 13; -fx-text-fill: #34495e; -fx-wrap-text: true; -fx-background-color: rgba(255,255,255,0.7); -fx-padding: 15; -fx-background-radius: 8;");

        VBox cameraCard = new VBox(5);
        cameraCard.setStyle(
                "-fx-background-color: black; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");
        cameraCard.setPadding(new Insets(10, 10, 10, 10));

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setStyle("-fx-background-radius: 10;");
        imageView.setFitWidth(600); // Larger width
        imageView.setFitHeight(400); // Larger height
        VBox.setVgrow(imageView, javafx.scene.layout.Priority.NEVER); // Don't let it grow
        cameraCard.getChildren().add(imageView);

        // Place cameraCard in a VBox with top alignment and leave space below
        VBox cameraContainer = new VBox(cameraCard);
        cameraContainer.setStyle("-fx-alignment: top-center;");
        VBox.setVgrow(cameraContainer, javafx.scene.layout.Priority.NEVER);

        statusLabel = new Label("⏳ Starting camera...");
        statusLabel.setStyle(
                "-fx-text-alignment: center; -fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #667eea; -fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        buttonBox = new HBox(12);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 0 0 0 0;"); // Remove extra top padding

        captureButton = new Button("📸 Capture Face");
        captureButton.setPrefWidth(160);
        captureButton.setPrefHeight(45);
        captureButton.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        captureButton.setDisable(true);
        captureButton.setOnAction(e -> captureFace());

        previewButton = new Button("👁 Preview");
        previewButton.setPrefWidth(120);
        previewButton.setPrefHeight(45);
        previewButton.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #f1c40f; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        previewButton.setDisable(true);
        previewButton.setOnAction(e -> previewCapturedFace());

        retryButton = new Button("🔄 Retry");
        retryButton.setPrefWidth(120);
        retryButton.setPrefHeight(45);
        retryButton.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        retryButton.setDisable(true);
        retryButton.setOnAction(e -> retryCapture());

        confirmButton = new Button("✅ Save & Close");
        confirmButton.setPrefWidth(160);
        confirmButton.setPrefHeight(45);
        confirmButton.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #11998e; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        confirmButton.setDisable(true);
        confirmButton.setOnAction(e -> confirmEnrollment());

        cancelButton = new Button("❌ Cancel");
        cancelButton.setPrefWidth(110);
        cancelButton.setPrefHeight(45);
        cancelButton.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        cancelButton.setOnAction(e -> cancelEnrollment());

        buttonBox.getChildren().addAll(captureButton, previewButton, retryButton, confirmButton, cancelButton);

        // Add a smaller spacer to push options only slightly down
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        spacer.setMinHeight(20); // Only a small gap
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.SOMETIMES);

        root.getChildren().addAll(
                titleLabel,
                nameLabel,
                instructions,
                cameraContainer, // camera box at top
                statusLabel,
                buttonBox, // Move buttonBox above spacer
                spacer); // Only a small gap below

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); // Allow vertical expansion
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    private void startCamera() {
        cameraThread = new Thread(() -> {
            try {
                boolean cameraFound = false;
                int activeCameraIndex = -1;
                for (int idx = 0; idx < 2; idx++) {
                    try {
                        frameGrabber = createAndStartGrabber(idx);
                        isRunning = true;
                        activeCameraIndex = idx;
                        final int cameraIdxForLambda = activeCameraIndex;
                        Platform.runLater(() -> {
                            statusLabel.setText(
                                    "[OK] Camera " + cameraIdxForLambda
                                            + " ready - Position your face and click Capture");
                            captureButton.setDisable(false);
                        });
                        cameraFound = true;
                        break;
                    } catch (Exception e) {
                        System.err.println("[WARN] Camera index " + idx + " failed: " + e.getMessage());
                    }
                }
                if (!cameraFound) {
                    throw new Exception("No available camera found.");
                }
                captureFrames();
            } catch (Exception e) {
                System.err.println("[ERROR] Camera error: " + e.getMessage());
                e.printStackTrace();
                AppLogger.error("Camera initialization failed: " + e.getMessage());
                final String errorMsg = e.getMessage();
                Platform.runLater(() -> {
                    statusLabel.setText("[ERROR] Camera failed to start");
                    showErrorDialog("Camera Error", errorMsg != null ? errorMsg : "Unknown camera error");
                });
            } finally {
                try {
                    if (frameGrabber != null) {
                        frameGrabber.stop();
                        frameGrabber.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private FrameGrabber createAndStartGrabber(int cameraIndex) throws Exception {
        FrameGrabber grabber = null;
        Exception lastException = null;
        try {
            grabber = new OpenCVFrameGrabber(cameraIndex);
            grabber.setImageWidth(AppConfig.CAMERA_WIDTH);
            grabber.setImageHeight(AppConfig.CAMERA_HEIGHT);
            grabber.setFrameRate(AppConfig.CAMERA_FPS);
            grabber.start();
            return grabber;
        } catch (Exception e) {
            lastException = e;
            if (grabber != null) {
                try {
                    grabber.release();
                } catch (Exception ex) {
                }
            }
        }
        if (cameraIndex == 0) {
            try {
                grabber = new OpenCVFrameGrabber(1);
                grabber.setImageWidth(AppConfig.CAMERA_WIDTH);
                grabber.setImageHeight(AppConfig.CAMERA_HEIGHT);
                grabber.setFrameRate(AppConfig.CAMERA_FPS);
                grabber.start();
                return grabber;
            } catch (Exception e) {
                if (grabber != null) {
                    try {
                        grabber.release();
                    } catch (Exception ex) {
                    }
                }
            }
        }
        throw new Exception("Could not access camera. Please ensure:\n" +
                "1. Camera is connected and working\n" +
                "2. No other application is using the camera\n" +
                "3. Camera permissions are granted\n\n" +
                "Original error: " + (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private void captureFrames() {
        int stableFaceCount = 0;
        int countdownSeconds = 3;
        try {
            while (isRunning) {
                try {
                    Frame frame = frameGrabber.grab();
                    if (frame == null) {
                        Thread.sleep(100);
                        continue;
                    }
                    Mat mat = converter.convert(frame);
                    List<Rect> faces = null;
                    if (mat != null && !mat.empty()) {
                        faces = faceDetector.detectFaces(mat);
                        if (!faces.isEmpty()) {
                            faceDetector.drawBoundingBoxes(mat, faces);
                        }
                        BufferedImage bufferedImage = faceDetector.convertMatToBufferedImage(mat);
                        if (bufferedImage != null) {
                            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                            Platform.runLater(() -> imageView.setImage(image));
                        }
                    }
                    if (faces != null && faces.size() == 1) {
                        stableFaceCount++;
                        if (stableFaceCount >= 30) {
                            for (int i = countdownSeconds; i > 0; i--) {
                                final int sec = i;
                                Platform.runLater(() -> statusLabel.setText("Hold still, capturing in " + sec + "..."));
                                Thread.sleep(1000);
                            }
                            Platform.runLater(() -> statusLabel.setText("Capturing face..."));
                            Platform.runLater(this::captureFace);
                            stableFaceCount = 0;
                            Thread.sleep(2000);
                        }
                    } else {
                        stableFaceCount = 0;
                    }
                    if (mat != null) {
                        mat.release();
                    }
                    Thread.sleep(33);
                } catch (InterruptedException ie) {
                    if (!isRunning)
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Frame capture error: " + e.getMessage());
        }
    }

    private void captureFace() {
        try {
            if (frameGrabber == null) {
                statusLabel.setText("[ERROR] Camera not active");
                return;
            }
            Frame frame = frameGrabber.grab();
            if (frame == null) {
                statusLabel.setText("[ERROR] Could not grab frame");
                return;
            }
            Mat mat = converter.convert(frame);
            List<Rect> faces = faceDetector.detectFaces(mat);
            if (faces.isEmpty()) {
                statusLabel.setText("[ERROR] No face detected! Ensure your face is visible.");
                return;
            }
            Mat detectedFace = faceDetector.extractFaceRegion(mat, faces.get(0));
            if (detectedFace == null || detectedFace.empty()) {
                statusLabel.setText("[ERROR] Could not extract face");
                return;
            }
            capturedFaceData.set(detectedFace.clone());
            statusLabel.setText("[OK] Face captured successfully! Click 'Confirm & Close' to save.");
            captureButton.setDisable(true);
            confirmButton.setDisable(false);
            previewButton.setDisable(false);
            retryButton.setDisable(false);
        } catch (Exception e) {
            System.err.println("[ERROR] Capture error: " + e.getMessage());
            statusLabel.setText("[ERROR] " + e.getMessage());
        }
    }

    private void previewCapturedFace() {
        Mat faceMat = capturedFaceData.get();
        if (faceMat == null || faceMat.empty()) {
            statusLabel.setText("[ERROR] No face data to preview.");
            return;
        }
        BufferedImage bufferedImage = faceDetector.convertMatToBufferedImage(faceMat);
        if (bufferedImage != null) {
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            Stage previewStage = new Stage();
            previewStage.setTitle("Preview Captured Face");
            ImageView previewView = new ImageView(image);
            previewView.setFitWidth(300);
            previewView.setFitHeight(300);
            previewView.setPreserveRatio(true);
            VBox previewBox = new VBox(previewView);
            previewBox.setPadding(new Insets(20));
            Scene scene = new Scene(previewBox);
            previewStage.setScene(scene);
            previewStage.show();
        }
    }

    private void retryCapture() {
        capturedFaceData.set(null);
        statusLabel.setText("🔄 Retake your face photo. Align your face and click Capture or wait for auto-capture.");
        captureButton.setDisable(false);
        confirmButton.setDisable(true);
        previewButton.setDisable(true);
        retryButton.setDisable(true);
    }

    private void confirmEnrollment() {
        isRunning = false;
        try {
            if (cameraThread != null) {
                cameraThread.join(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stage.close();
        if (onEnrollmentComplete != null) {
            Platform.runLater(onEnrollmentComplete);
        }
    }

    private void cancelEnrollment() {
        isRunning = false;
        capturedFaceData.set(null);
        try {
            if (cameraThread != null) {
                cameraThread.join(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stage.close();
    }

    public byte[] getCapturedFaceData() {
        Mat faceMat = capturedFaceData.get();
        if (faceMat == null || faceMat.empty()) {
            return null;
        }
        return FaceDetectionEngine.encodeFaceData(faceMat);
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
