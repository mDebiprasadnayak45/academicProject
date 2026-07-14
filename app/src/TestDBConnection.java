import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Debug-only database connectivity test.
 * This class should NOT be used in production flows.
 */

public class TestDBConnection {
    public static void main(String[] args) {
        String url = AppConfig.getSqlServerDatabaseUrl();

        System.out.println("Testing SQL Server connection...");
        System.out.println("URL: " + url);

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("[OK] Driver loaded");

            try (Connection conn = AppConfig.SQLSERVER_INTEGRATED_SECURITY
                    ? DriverManager.getConnection(url)
                    : DriverManager.getConnection(url, AppConfig.SQLSERVER_USER, AppConfig.SQLSERVER_PASSWORD);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT @@VERSION")) {

                System.out.println("[OK] Connected successfully!");

                if (rs.next()) {
                    System.out.println("[OK] SQL Server Version: " + rs.getString(1).substring(0, 50) + "...");
                }
            }

            System.out.println("\n[OK] ALL TESTS PASSED!");

        } catch (Exception e) {
            System.out.println("\n[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
