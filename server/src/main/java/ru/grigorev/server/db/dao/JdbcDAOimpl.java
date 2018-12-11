package ru.grigorev.server.db.dao;

import ru.grigorev.server.db.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Dmitriy Grigorev
 */
public class JdbcDAOimpl implements DAO {
    private Connection connection;

    public JdbcDAOimpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insertNewUser(User user) throws SQLException {
        String query = String.format("INSERT INTO \"UsersSchema\".\"Users\"" +
                "(login, password) VALUES ('%s', '%s');", user.getLogin(), user.getPassword());
        Statement statement = connection.createStatement();
        statement.execute(query);
    }

    @Override
    public User getUserByLogin(String login) throws SQLException {
        if (login == null) return null;
        String query = String.format("SELECT * FROM \"UsersSchema\".\"Users\" where login = '%s';", login);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        return convertToUser(resultSet);
    }

    private User convertToUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        if (!resultSet.next()) return null;
        user.setId(resultSet.getInt(1));
        user.setLogin(resultSet.getString(2).trim());
        user.setPassword(resultSet.getString(3).trim());
        return user;
    }
}
