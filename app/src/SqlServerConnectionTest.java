import java.sql.Connection;
import java.sql.DriverManager;

public class SqlServerConnectionTest {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=FaceAttendanceDB;trustServerCertificate=true;";
        String user = "sa";
        String password = "Monu@2026!";
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("[OK] Connection successful!");
            conn.close();
        } catch (Exception e) {
            System.err.println("[ERROR] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
