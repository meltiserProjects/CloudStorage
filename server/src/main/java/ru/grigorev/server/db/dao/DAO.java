package ru.grigorev.server.db.dao;

import ru.grigorev.server.db.model.User;

import java.sql.SQLException;

/**
 * @author Dmitriy Grigorev
 */
public interface DAO {
    User getUserByLogin(String login) throws SQLException;

    void insertNewUser(User user) throws SQLException;
}
