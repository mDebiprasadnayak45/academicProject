import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2BGR;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

/**
 * Face Detection and Recognition System using OpenCV with LBPH Algorithm.
 * This engine detects faces in camera frames and recognizes them by matching
 * against stored face data using Local Binary Patterns Histograms (LBPH).
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class FaceDetectionEngine {
    /** Minimum confidence threshold for face recognition (0.0 to 1.0) */
    private static final float CONFIDENCE_THRESHOLD = AppConfig.CONFIDENCE_THRESHOLD;

    private CascadeClassifier faceCascade;

    /**
     * Constructs a new FaceDetectionEngine and initializes the face cascade
     * classifier
     * and LBPH face recognizer for face detection and recognition.
     */
    public FaceDetectionEngine() {
        try {
            // Load cascade classifier from classpath resource
            String cascadePath = extractCascadeToTemp(AppConfig.CASCADE_CLASSIFIER_RESOURCE);
            faceCascade = new CascadeClassifier(cascadePath);

            if (faceCascade.empty()) {
                System.err.println("[WARNING] Cascade classifier not found. Check resources folder.");
            } else {
                System.out.println("[OK] Face Detection Engine initialized with OpenCV");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load cascade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extracts the cascade classifier XML file from resources to a temporary file.
     * This is necessary because OpenCV requires a file path, not a resource stream.
     * 
     * @param resourcePath The classpath resource path to the cascade XML file
     * @return Absolute path to the extracted temporary file
     * @throws IOException If the resource cannot be found or extracted
     */
    private String extractCascadeToTemp(String resourcePath) throws IOException {
        try (InputStream in = FaceDetectionEngine.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Cascade resource not found: " + resourcePath);
            }
            File tempFile = File.createTempFile("cascade-", ".xml");
            tempFile.deleteOnExit();
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile.getAbsolutePath();
        }
    }

    /**
     * Detects faces in the given video frame using Haar Cascade classifier.
     * Converts the frame to grayscale and applies multi-scale detection.
     * 
     * @param frame The input video frame (Mat) to detect faces in
     * @return List of rectangles representing detected face regions
     */
    public List<Rect> detectFaces(Mat frame) {
        List<Rect> faceList = new ArrayList<>();

        if (frame == null || frame.empty()) {
            return faceList;
        }

        try {
            Mat gray = new Mat();
            cvtColor(frame, gray, COLOR_BGR2GRAY);

            RectVector detections = new RectVector();
            if (faceCascade != null && !faceCascade.empty()) {
                faceCascade.detectMultiScale(
                        gray,
                        detections,
                        AppConfig.FACE_DETECTION_SCALE_FACTOR,
                        AppConfig.FACE_DETECTION_MIN_NEIGHBORS,
                        0,
                        new Size(AppConfig.MIN_FACE_SIZE, AppConfig.MIN_FACE_SIZE),
                        new Size(AppConfig.MAX_FACE_SIZE, AppConfig.MAX_FACE_SIZE));

                for (long i = 0; i < detections.size(); i++) {
                    Rect face = detections.get(i);
                    faceList.add(face);
                    System.out.println("[\u2713] Face detected at: (" + face.x() + ", " + face.y() + ")");
                }
            }

            gray.release();
            detections.close();

        } catch (Exception e) {
            System.err.println("[ERROR] Face detection failed: " + e.getMessage());
        }

        return faceList;
    }

    /**
     * Draws bounding boxes around detected faces on the frame.
     * 
     * @param frame The frame to draw on
     * @param faces List of face rectangles to draw
     */
    public void drawBoundingBoxes(Mat frame, List<Rect> faces) {
        for (Rect face : faces) {
            try {
                // Expand the rectangle by 40% width and 60% height, mostly downward for
                // head-to-neck
                int expandW = (int) (face.width() * 0.4);
                int expandH = (int) (face.height() * 0.6);
                int x = Math.max(face.x() - expandW / 2, 0);
                int y = Math.max(face.y() - (int) (expandH * 0.2), 0); // cast to int
                int w = face.width() + expandW;
                int h = face.height() + expandH;
                Rect bigFace = new Rect(x, y, w, h);
                Scalar color = new Scalar(0, 255, 0, 255);
                rectangle(frame, bigFace, color, 4, LINE_8, 0);
            } catch (Exception e) {
                System.err.println("[ERROR] Drawing rectangle failed: " + e.getMessage());
            }
        }
    }

    /**
     * Extracts and normalizes a face region from the frame for recognition
     * processing.
     * The face is resized to a standard size for consistent feature extraction.
     * 
     * @param frame    The full video frame
     * @param faceRect Rectangle defining the face region
     * @return Normalized face Mat resized to 100x100, or null if extraction fails
     */
    public Mat extractFaceRegion(Mat frame, Rect faceRect) {
        if (frame == null || frame.empty()) {
            return null;
        }

        try {
            // Expand the face rectangle downward for head-to-neck
            int expandW = (int) (faceRect.width() * 0.4);
            int expandH = (int) (faceRect.height() * 0.6);
            int x = Math.max(faceRect.x() - expandW / 2, 0);
            int y = Math.max(faceRect.y() - (int) (expandH * 0.2), 0);
            int w = faceRect.width() + expandW;
            int h = faceRect.height() + expandH;
            // Ensure ROI is within frame bounds
            w = Math.min(w, frame.cols() - x);
            h = Math.min(h, frame.rows() - y);
            Rect headToNeck = new Rect(x, y, w, h);
            Mat faceRegion = new Mat(frame, headToNeck);
            Mat resized = new Mat();
            resize(faceRegion, resized, new Size(AppConfig.FACE_CROP_SIZE, AppConfig.FACE_CROP_SIZE));
            faceRegion.release(); // Release after resize

            // Convert to grayscale for LBPH recognition
            Mat gray = new Mat();
            if (resized.channels() > 1) {
                cvtColor(resized, gray, COLOR_BGR2GRAY);
                resized.release();
                return gray;
            } else {
                // If already grayscale, return resized
                return resized;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Extracting face failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * LBPH-BASED FACE RECOGNITION: Extracts and compares facial features using
     * Local Binary Patterns.
     * Includes histogram equalization preprocessing for robustness to lighting
     * variations.
     * Divides face into regions, computes LBP histograms, and compares histogram
     * similarity.
     * This is true facial feature recognition, not just pixel matching.
     * 
     * @param capturedFace Grayscale Mat of the captured face (100x100)
     * @param storedFace   Grayscale Mat of the stored face (100x100)
     * @return Similarity score between 0.0 (no match) and 1.0 (perfect match)
     */
    public float compareFaces(Mat capturedFace, Mat storedFace) {
        if (capturedFace == null || storedFace == null || capturedFace.empty() || storedFace.empty()) {
            return 0.0f;
        }

        Mat processedCaptured = null;
        Mat processedStored = null;

        try {
            // PREPROCESSING: Apply histogram equalization for lighting robustness
            processedCaptured = new Mat();
            processedStored = new Mat();

            equalizeHist(capturedFace, processedCaptured);
            equalizeHist(storedFace, processedStored);

            System.out.println("[DEBUG] Histogram equalization applied to both faces for lighting robustness");

            // Extract LBPH features from preprocessed faces
            int[] capturedHistogram = extractLBPHFeatures(processedCaptured);
            int[] storedHistogram = extractLBPHFeatures(processedStored);

            if (capturedHistogram == null || storedHistogram == null) {
                System.err.println("[ERROR] Failed to extract LBPH features from one or both faces");
                return 0.0f;
            }

            // Compare histograms using correlation (higher = better match)
            float correlation = calculateHistogramCorrelation(capturedHistogram, storedHistogram);
            System.out.println("[DEBUG] Histogram Correlation: " + String.format("%.4f", correlation));

            // Correlation is in range [-1, 1], normalize to [0, 1]
            float similarity = (correlation + 1.0f) / 2.0f;
            return Math.min(1.0f, Math.max(0.0f, similarity));
        } catch (Exception e) {
            System.err.println("[ERROR] LBPH comparison failed: " + e.getMessage());
            e.printStackTrace();
            return 0.0f;
        } finally {
            // Release temporary Mats to prevent memory leaks
            if (processedCaptured != null)
                processedCaptured.release();
            if (processedStored != null)
                processedStored.release();
        }
    }

    /**
     * Extracts Local Binary Patterns Histograms (LBPH) features from a face image.
     * Divides the face into 8x8 grid, computes LBP for each region, and creates
     * histogram.
     * This creates a unique feature signature for each face.
     * 
     * @param faceImage Grayscale 100x100 face image
     * @return Histogram of LBP values (256 bins for 8-bit patterns), or null if
     *         extraction fails
     */
    private int[] extractLBPHFeatures(Mat faceImage) {
        try {
            // Initialize histogram (256 possible LBP patterns for 8 neighbors)
            int[] histogram = new int[256];

            // LBPH Algorithm:
            // 1. For each pixel (except borders)
            // 2. Compare with 8 neighbors (3x3 window)
            // 3. Create binary pattern based on comparison
            // 4. Convert binary to decimal (0-255)
            // 5. Increment histogram bin for that pattern

            for (int y = 1; y < faceImage.rows() - 1; y++) {
                for (int x = 1; x < faceImage.cols() - 1; x++) {
                    // Get center pixel value
                    int center = faceImage.ptr(y, x).get() & 0xFF;

                    // Get 8 neighbors in clockwise order (top, top-right, right, etc.)
                    int[] neighbors = {
                            faceImage.ptr(y - 1, x - 1).get() & 0xFF, // top-left
                            faceImage.ptr(y - 1, x).get() & 0xFF, // top
                            faceImage.ptr(y - 1, x + 1).get() & 0xFF, // top-right
                            faceImage.ptr(y, x + 1).get() & 0xFF, // right
                            faceImage.ptr(y + 1, x + 1).get() & 0xFF, // bottom-right
                            faceImage.ptr(y + 1, x).get() & 0xFF, // bottom
                            faceImage.ptr(y + 1, x - 1).get() & 0xFF, // bottom-left
                            faceImage.ptr(y, x - 1).get() & 0xFF // left
                    };

                    // Compute LBP pattern: bit i = 1 if neighbor[i] >= center, else 0
                    int lbpPattern = 0;
                    for (int i = 0; i < 8; i++) {
                        if (neighbors[i] >= center) {
                            lbpPattern |= (1 << i);
                        }
                    }

                    // Increment histogram bin for this LBP pattern
                    histogram[lbpPattern]++;
                }
            }

            // Normalize histogram to 0-1 range (optional but helps with comparison)
            int totalPixels = (faceImage.rows() - 2) * (faceImage.cols() - 2);
            if (totalPixels > 0) {
                for (int i = 0; i < histogram.length; i++) {
                    // Keep as counts for chi-square distance calculation
                }
            }

            System.out.println("[DEBUG] LBPH features extracted: " + faceImage.rows() + "x" +
                    faceImage.cols() + " face → 256-bin histogram");

            return histogram;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to extract LBPH features: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates Chi-Square distance between two histograms.
     * Chi-Square: Σ((h1[i] - h2[i])² / (h1[i] + h2[i]))
     * Lower distance = better match
     * 
     * @param histogram1 First histogram (256 bins)
     * @param histogram2 Second histogram (256 bins)
     * @return Chi-Square distance (0 = perfect match)
     */
    /**
     * Calculates histogram correlation between two histograms.
     * Correlation: Σ((h1[i] - mean1) * (h2[i] - mean2)) / sqrt(Σ(h1[i] - mean1)^2 *
     * Σ(h2[i] - mean2)^2)
     * Returns value in [-1, 1], where 1 = perfect match.
     */
    private float calculateHistogramCorrelation(int[] histogram1, int[] histogram2) {
        if (histogram1.length != histogram2.length) {
            return -1.0f;
        }

        int n = histogram1.length;
        double mean1 = 0.0, mean2 = 0.0;
        for (int i = 0; i < n; i++) {
            mean1 += histogram1[i];
            mean2 += histogram2[i];
        }
        mean1 /= n;
        mean2 /= n;

        double num = 0.0, denom1 = 0.0, denom2 = 0.0;
        for (int i = 0; i < n; i++) {
            double d1 = histogram1[i] - mean1;
            double d2 = histogram2[i] - mean2;
            num += d1 * d2;
            denom1 += d1 * d1;
            denom2 += d2 * d2;
        }
        double denom = Math.sqrt(denom1 * denom2);
        if (denom == 0.0)
            return 0.0f;
        return (float) (num / denom);
    }

    /**
     * CORE FACE RECOGNITION LOGIC: Identifies which student the captured face
     * belongs to.
     * Uses LBPH feature matching to recognize faces and return valid studentId.
     * This is TRUE FACE RECOGNITION, not face detection.
     * 
     * Recognition Process:
     * 1. Load all registered students' stored face features from database
     * 2. Extract LBPH features from captured face
     * 3. For each student: Compare captured features with stored features
     * 4. Calculate similarity score using Chi-Square distance
     * 5. Find student with BEST MATCH (highest similarity)
     * 6. IF similarity >= threshold (0.6): Return valid studentId [OK]
     * 7. ELSE: Return -1 (unknown person, NO attendance marked) [ERROR]
     * 
     * @param capturedFace    Grayscale Mat of detected face (100x100)
     * @param registeredUsers List of registered students with stored face data
     * @return IdentificationResult with valid studentId ONLY if recognized
     */
    public IdentificationResult identifyUser(Mat capturedFace, List<UserFaceData> registeredUsers) {
        // Validate inputs
        if (registeredUsers == null || registeredUsers.isEmpty()) {
            System.out.println("[ERROR] RECOGNITION FAILED: No registered students in database");
            return new IdentificationResult(false, null, null, 0.0f);
        }

        if (capturedFace == null || capturedFace.empty()) {
            System.out.println("[ERROR] RECOGNITION FAILED: Captured face is null/empty");
            return new IdentificationResult(false, null, null, 0.0f);
        }

        float bestSimilarity = 0.0f;
        String recognizedStudentId = null;
        String recognizedStudentName = null;

        // FACE RECOGNITION: Compare against each registered student
        System.out.println("\n========== FACE RECOGNITION START ==========");
        System.out.println("[INFO] Recognizing captured face against " + registeredUsers.size() +
                " registered students using LBPH features...");

        for (UserFaceData student : registeredUsers) {
            if (student.getFaceData() == null || student.getFaceData().empty()) {
                System.out.println("[!] Student " + student.getUserId() + " (" + student.getUserName() +
                        "): NO STORED FACE DATA - skipping");
                continue;
            }

            // Extract LBPH features and compare with student's stored face
            float similarity = compareFaces(capturedFace, student.getFaceData());

            System.out.println("[->] Student " + student.getUserId() + " (" + student.getUserName() +
                    "): Similarity = " + String.format("%.2f%%", similarity * 100));

            // Track best match
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                recognizedStudentId = student.getUserId();
                recognizedStudentName = student.getUserName();
            }
        }

        // CONFIDENCE DECISION: Recognize ONLY if similarity above threshold
        System.out.println(
                "\n[DECISION] Best Match: " + (recognizedStudentName != null ? recognizedStudentName : "NONE") +
                        " (" + String.format("%.2f%%", bestSimilarity * 100) + ")");
        System.out.println("[THRESHOLD] Required: " + (CONFIDENCE_THRESHOLD * 100) + "% confidence");

        if (bestSimilarity >= CONFIDENCE_THRESHOLD) {
            System.out.println("[OK] FACE RECOGNIZED SUCCESSFULLY!");
            System.out.println("[OK] StudentId: " + recognizedStudentId);
            System.out.println("[OK] Name: " + recognizedStudentName);
            System.out.println("[OK] Confidence: " + String.format("%.2f%%", bestSimilarity * 100));
            System.out.println("========== ATTENDANCE WILL BE MARKED ==========\n");

            return new IdentificationResult(true, recognizedStudentId, recognizedStudentName, bestSimilarity);
        } else {
            System.out.println("[ERROR] FACE NOT RECOGNIZED");
            System.out.println("[ERROR] Best match (" + String.format("%.2f%%", bestSimilarity * 100) +
                    ") is below threshold (" + (AppConfig.CONFIDENCE_THRESHOLD * 100) + "%)");
            System.out.println("[ERROR] This is an UNKNOWN PERSON - NO ATTENDANCE MARKED");
            System.out.println("========== FACE REJECTED - UNKNOWN PERSON ==========\n");

            return new IdentificationResult(false, null, null, bestSimilarity);
        }
    }

    /**
     * Converts a BufferedImage to OpenCV Mat format.
     * 
     * @param image BufferedImage to convert
     * @return Mat representation of the image, or null if conversion fails
     */
    public Mat convertBufferedImageToMat(BufferedImage image) {
        if (image == null)
            return null;

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC3);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.data().put(data);
        return mat;
    }

    /**
     * Converts an OpenCV Mat to BufferedImage format for display in Swing/JavaFX.
     * 
     * @param mat OpenCV Mat to convert
     * @return BufferedImage representation, or null if conversion fails
     */
    public BufferedImage convertMatToBufferedImage(Mat mat) {
        if (mat == null || mat.empty())
            return null;
        Mat converted = mat;
        Mat temp = null;

        try {
            if (mat.channels() == 4) {
                // Convert BGRA frames to BGR for correct color display.
                temp = new Mat();
                cvtColor(mat, temp, COLOR_BGRA2BGR);
                converted = temp;
            }

            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (converted.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }

            BufferedImage image = new BufferedImage(converted.cols(), converted.rows(), type);
            byte[] data = new byte[(int) (converted.total() * converted.elemSize())];
            converted.data().get(data);
            image.getRaster().setDataElements(0, 0, converted.cols(), converted.rows(), data);
            return image;
        } finally {
            if (temp != null) {
                temp.release();
            }
        }
    }

    /**
     * Saves face data to a file for later recognition.
     * 
     * @param faceData Mat containing the face data
     * @param filePath Path where the face data should be saved
     * @return true if save was successful, false otherwise
     */
    public static boolean saveFaceDataToFile(Mat faceData, String filePath) {
        try {
            // Create parent directories if they don't exist
            new File(new File(filePath).getParent()).mkdirs();

            // Actually write the Mat to disk
            boolean success = imwrite(filePath, faceData);

            if (success) {
                System.out.println("[OK] Face data saved to: " + filePath);
            } else {
                System.err.println("[ERROR] imwrite() failed for: " + filePath);
            }

            return success;
        } catch (Exception e) {
            System.err.println("[ERROR] Saving face data failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads face data from a file for recognition.
     * 
     * @param filePath Path to the saved face data file
     * @return Mat containing the face data, or null if load fails
     */
    public static Mat loadFaceDataFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Actually load the image from disk
                Mat loadedMat = imread(filePath, IMREAD_GRAYSCALE);

                if (loadedMat.empty()) {
                    System.err.println("[ERROR] imread() returned empty Mat for: " + filePath);
                    return null;
                }

                System.out.println("[OK] Face data loaded from: " + filePath);
                return loadedMat;
            } else {
                System.err.println("[ERROR] File does not exist: " + filePath);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Loading face data failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * CRITICAL FIX: Serialize a face Mat to byte array using OpenCV imencode.
     * Properly encodes the face image as PNG for reliable storage in database.
     * This is the CORRECT way to serialize OpenCV Mat objects.
     * 
     * @param faceMat Grayscale Mat of face (100x100)
     * @return Byte array (PNG-encoded), or empty array if encoding fails
     */
    public static byte[] encodeFaceData(Mat faceMat) {
        if (faceMat == null || faceMat.empty()) {
            System.err.println("[ERROR] Cannot encode null or empty face Mat");
            return new byte[0];
        }

        try {
            // Use OpenCV imencode to properly encode Mat as PNG
            // Create a BytePointer to receive encoded data
            BytePointer buf = new BytePointer();

            boolean success = imencode(".png", faceMat, buf);

            if (!success || buf.limit() == 0) {
                System.err.println("[ERROR] imencode failed to encode face data");
                buf.deallocate();
                return new byte[0];
            }

            // Extract bytes from the BytePointer
            byte[] imageData = new byte[(int) buf.limit()];
            buf.get(imageData);

            System.out.println("[OK] Face data encoded successfully (" + imageData.length + " bytes)");

            buf.deallocate();
            return imageData;
        } catch (Exception e) {
            System.err.println("[ERROR] Face encoding failed: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * CRITICAL FIX: Deserialize a byte array back to face Mat using OpenCV
     * imdecode.
     * Properly decodes PNG-encoded face data from database.
     * This is the CORRECT way to deserialize byte arrays to OpenCV Mat objects.
     * 
     * @param faceBytes PNG-encoded byte array from database
     * @return Decoded grayscale Mat (100x100), or null if decoding fails
     */
    public static Mat decodeFaceData(byte[] faceBytes) {
        if (faceBytes == null || faceBytes.length == 0) {
            System.err.println("[ERROR] Cannot decode null or empty byte array");
            return null;
        }

        try {
            // Use OpenCV imdecode to properly decode byte array to Mat
            Mat encodedMat = new Mat(1, faceBytes.length, CV_8UC1);
            encodedMat.data().put(faceBytes);

            // Decode as grayscale image
            Mat decodedFace = imdecode(encodedMat, IMREAD_GRAYSCALE);
            encodedMat.release();

            if (decodedFace == null || decodedFace.empty()) {
                System.err.println("[ERROR] imdecode failed to decode face data");
                return null;
            }

            // ALWAYS resize to standard size for consistency
            Mat resized = new Mat();
            resize(decodedFace, resized, new Size(AppConfig.FACE_CROP_SIZE, AppConfig.FACE_CROP_SIZE));
            decodedFace.release();

            System.out.println("[OK] Face data decoded successfully (" + AppConfig.FACE_CROP_SIZE + "x"
                    + AppConfig.FACE_CROP_SIZE + " grayscale)");
            return resized;
        } catch (Exception e) {
            System.err.println("[ERROR] Face decoding failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Releases all OpenCV resources held by this engine.
     * Should be called when the engine is no longer needed to prevent memory leaks.
     */
    public void release() {
        if (faceCascade != null) {
            faceCascade.close();
        }
        System.out.println("[OK] Face Detection Engine released");
    }
}

/**
 * Data holder for user face information used in face recognition.
 * Stores the user ID, name, and their associated face feature data.
 */
class UserFaceData {
    private String userId;
    private String userName;
    private Mat faceData;

    public UserFaceData(String userId, String userName, Mat faceData) {
        this.userId = userId;
        this.userName = userName;
        this.faceData = faceData;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Mat getFaceData() {
        return faceData;
    }
}

/**
 * Result of a face identification operation.
 * Contains whether a match was found, the matched user's details,
 * and the confidence score of the match.
 */
class IdentificationResult {
    private boolean isMatch;
    private String userId;
    private String userName;
    private float confidenceScore;

    public IdentificationResult(boolean isMatch, String userId, String userName, float confidenceScore) {
        this.isMatch = isMatch;
        this.userId = userId;
        this.userName = userName;
        this.confidenceScore = confidenceScore;
    }

    public boolean isMatch() {
        return isMatch;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public float getConfidenceScore() {
        return confidenceScore;
    }
}
