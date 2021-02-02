package server.db;

import server.AuthService;

import java.sql.*;

public class DBAuth implements AuthService {
    private PreparedStatement queryNickname;
    private PreparedStatement register;
    private PreparedStatement nicknameChange;

    public DBAuth(DBConnection connection) {
        try {
            Connection c = connection.getConnection();
            register = c.prepareStatement("INSERT INTO accounts (login, password, nickname) VALUES (?, ?, ?);");
            queryNickname = c.prepareStatement("SELECT nickname FROM accounts WHERE login = ? AND password = ?;");
            nicknameChange = c.prepareStatement("UPDATE accounts SET nickname = ? WHERE login = ? AND password = ?;");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            queryNickname.setString(1, login);
            queryNickname.setString(2, password);
            ResultSet rs = queryNickname.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            } else {
                return null;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            register.setString(1, login);
            register.setString(2, password);
            register.setString(3, nickname);
            int a = register.executeUpdate();
            return a > 0;
        } catch (SQLException throwables) {
            return false;
        }
    }

    @Override
    public boolean changeNickname(String login, String password, String newNickname) {
        try {
            nicknameChange.setString(1, newNickname);
            nicknameChange.setString(2, login);
            nicknameChange.setString(3, password);
            int a = nicknameChange.executeUpdate();
            return a > 0;
        } catch (SQLException throwables) {
            return false;
        }
    }
}
