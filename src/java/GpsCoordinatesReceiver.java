
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MultipartConfig
@WebServlet(name = "GpsCoordinates", urlPatterns = {"/GpsCoordinates"})
public class GpsCoordinatesReceiver extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Properties properties = new Properties();
            properties.put("connectTimeout", "2000");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sigma?user=root&password=Fsk129azF", properties);

            JsonArray jar = (JsonArray) Json.createReader(request.getPart("data").getInputStream()).readArray();

            for (int i = 0; i < jar.size(); i++) {
                JsonObject ob = (JsonObject) jar.get(i);
                Statement statement = conn.createStatement();
                statement.executeUpdate("INSERT INTO gps_app SET "
                        + "imei = '" + ob.getString("imei" + i) + "', "
                        + "dt = '" + ob.getString("dt" + i) + "', "
                        + "lat = " + ob.getString("lat" + i) + ", "
                        + "lon = " + ob.getString("lon" + i));
            }

            response.getOutputStream().write("ok".getBytes());
        } catch (Exception e) {
            Logger.getLogger(GpsCoordinatesReceiver.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(GpsCoordinatesReceiver.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static JsonValue scapeJsonObj(final JsonValue v) {
        switch (v.getValueType()) {
            case STRING:
                return new JsonString() {

                    private final String s = scape(((JsonString) v).getString());

                    @Override
                    public String getString() {
                        return s;
                    }

                    @Override
                    public CharSequence getChars() {
                        return s;
                    }

                    @Override
                    public JsonValue.ValueType getValueType() {
                        return JsonValue.ValueType.STRING;
                    }
                };
            case OBJECT:
                JsonObjectBuilder job = Json.createObjectBuilder();
                JsonObject o = ((JsonObject) v);
                Iterator<Map.Entry<String, JsonValue>> it = o.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, JsonValue> e = it.next();
                    job.add(e.getKey(), scapeJsonObj(e.getValue()));
                }
                return job.build();
            case ARRAY:
                JsonArray arr = (JsonArray) v;
                JsonArrayBuilder b = Json.createArrayBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    b.add(scapeJsonObj(arr.get(i)));
                }
                return b.build();
            default:
                return v;
        }
    }

    private static String scape(String str) {
        String rta = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\'':
                case '\"':
                    rta += "\\" + c;
                    break;
                case '\\':
                    if (i < str.length() - 1) {
                        char c1 = str.charAt(i + 1);
                        switch (c1) {
                            case '\'':
                            case '\"':
                            case '\\':
                                rta += c;
                                rta += c1;
                                i++;
                                break;
                            default:
                                rta += "\\\\";
                                break;
                        }
                    } else {
                        rta += "\\\\";
                    }
                    break;
                default:
                    rta += c;
                    break;
            }
        }
        return rta;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
