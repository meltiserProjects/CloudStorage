package ru.grigorev.server.db.service;

import ru.grigorev.server.db.dao.DAO;

/**
 * @author Dmitriy Grigorev
 */
public interface DatabaseInteraction extends AutoCloseable {
    void initialize();

    DAO getDAO();
}
