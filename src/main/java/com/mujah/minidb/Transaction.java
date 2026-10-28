package com.mujah.minidb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Transaction implements AutoCloseable {

    private final Database database;
    private final List<Runnable> operations = new ArrayList<>();
    private boolean committed = false;
    private boolean rolledBack = false;

    Transaction(Database database) {
        this.database = database;
    }

    public void set(String key, String value) {
        assertActive();
        operations.add(() -> {
            try {
                database.set(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void delete(String key) {
        assertActive();
        operations.add(() -> {
            try {
                database.delete(key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void commit() throws IOException {
        assertActive();

        for (Runnable operation : operations) {
            operation.run();
        }

        committed = true;
    }

    public void rollback() {
        assertActive();
        rolledBack = true;
    }

    private void assertActive() {
        if (committed) {
            throw new IllegalStateException(
                    "Transaction is already committed");
        }

        if (rolledBack) {
            throw new IllegalStateException(
                    "Transaction is already rolled back");
        }
    }

    @Override
    public void close() {
        if (!committed && !rolledBack) {
            rolledBack = true;
        }
    }
}