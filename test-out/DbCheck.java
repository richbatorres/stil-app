import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String db = args.length > 0 ? args[0] : "stil.db";
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
        for (String t : new String[]{"artikl","dobavljac","racun","stavka_racuna","nabava","povrat_robe"}) {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + t);
            System.out.println(t + ": " + rs.getInt(1));
        }
        c.close();
    }
}
