package server.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistorySaver {
    private PreparedStatement saveMessage;
    private PreparedStatement getMessages;

    public HistorySaver(DBConnection connection) {
        try {
            Connection c = connection.getConnection();
            saveMessage = c.prepareStatement("INSERT INTO messages (date, nick, message) VALUES (?, ?, ?);");
            getMessages = c.prepareStatement("SELECT * FROM messages;");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void saveMessage(String nickname, String message) {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            saveMessage.setString(1, dtf.format(now));
            saveMessage.setString(2, nickname);
            saveMessage.setString(3, message);
            saveMessage.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public ResultSet getMessages() throws SQLException {
        return getMessages.executeQuery();
    }
}
