# MiniDB

MiniDB is a persistent key-value database built from scratch in Java.

It combines an append-only data file, a disk-based B+Tree index, a write-ahead log, transactions, snapshots, and TTL expiration.

## Features

| Feature | Description |
|---|---|
| Persistent storage | Values are stored in an append-only data file |
| B+Tree index | Keys are indexed on disk using fixed-size pages |
| Buffer pool | Frequently used pages are cached in memory with LRU eviction |
| Write-ahead logging | Unfinished writes are recovered after a crash |
| Transactions | Group writes with commit and rollback |
| Atomic counters | Thread-safe increment and decrement operations |
| Snapshots | Export and restore the full database state |
| TTL expiration | Keys can expire automatically after a given time |
| Compaction | Remove obsolete data and reclaim disk space |

## Requirements

- Java 17 or newer
- Maven 3.8 or newer

## Getting started

Clone the repository:

```bash
git clone [https://github.com/YOUR_USERNAME/mini-db.git](https://github.com/YOUR_USERNAME/mini-db.git)
cd mini-db
```

Run the tests:

```bash
mvn clean test
```

## Usage

```java
import com.mujah.minidb.Database;

public class Demo {
    public static void main(String[] args) throws Exception {
        try (Database db = new Database("mydb")) {
            db.set("name", "Mujah");
            db.set("city", "Mumbai");

            System.out.println(db.get("name"));

            db.increment("visits");
            db.expire("session", 60);

            System.out.println(db.stats());
        }
    }
}
```

## Transactions

```java
try (Database db = new Database("mydb")) {
    try (Transaction tx = db.begin()) {
        tx.set("name", "Mujah");
        tx.set("city", "Mumbai");
        tx.commit();
    }
}
```

If `commit()` is never called, the transaction rolls back automatically.

## Snapshots

```java
database.exportTo("backup.snapshot");
database.restoreFrom("backup.snapshot");
```

## Project structure

```text
src/main/java/com/mujah/minidb/
├── Database.java
├── DiskManager.java
├── DiskBPlusTree.java
├── BPlusTree.java
├── BPlusTreeNode.java
├── Page.java
├── PageFile.java
├── WalManager.java
├── Transaction.java
├── CommandParser.java
└── Main.java
```

## Roadmap

- Persist TTL expiry times across restarts
- Range scans and cursor-based iteration
- Socket server with a Redis-style command protocol
- Configurable WAL fsync policy
- Concurrent transaction isolation

## License

MIT