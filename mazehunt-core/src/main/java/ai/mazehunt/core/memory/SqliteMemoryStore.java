package ai.mazehunt.core.memory;

import ai.mazehunt.api.memory.MemoryItem;
import ai.mazehunt.core.util.Vectors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Persistent memory store backed by SQLite.
 *
 * <p>Design notes for a 192 GB RAM host:
 * <ul>
 *   <li>SQLite page cache is sized aggressively via {@code PRAGMA cache_size}
 *       so hot memory items stay in RAM.</li>
 *   <li>Embeddings live in a BLOB column; {@link #all()} is only used by the
 *       in-memory vector index which pages lazily.</li>
 *   <li>WAL mode keeps readers non-blocking during consolidation.</li>
 * </ul>
 */
public final class SqliteMemoryStore implements AutoCloseable {

    private final Connection conn;

    public SqliteMemoryStore(Path dbPath) {
        try {
            if (dbPath.getParent() != null) Files.createDirectories(dbPath.getParent());
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA cache_size=-524288");   // ~512 MB
                st.execute("PRAGMA mmap_size=17179869184"); // 16 GB mmap
                st.execute("""
                    CREATE TABLE IF NOT EXISTS memory_items (
                      id TEXT PRIMARY KEY,
                      tier TEXT NOT NULL,
                      content TEXT NOT NULL,
                      embedding BLOB,
                      tags TEXT,
                      source TEXT,
                      confidence REAL NOT NULL DEFAULT 1.0,
                      created_at INTEGER NOT NULL,
                      last_accessed INTEGER NOT NULL,
                      access_count INTEGER NOT NULL DEFAULT 0,
                      tokens INTEGER NOT NULL DEFAULT 0
                    )
                    """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_mem_tier ON memory_items(tier)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_mem_created ON memory_items(created_at)");
                st.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts
                    USING fts5(content, content='memory_items', content_rowid='rowid')
                    """);
                st.execute("""
                    CREATE TRIGGER IF NOT EXISTS memory_ai AFTER INSERT ON memory_items BEGIN
                      INSERT INTO memory_fts(rowid, content) VALUES (new.rowid, new.content);
                    END
                    """);
                st.execute("""
                    CREATE TRIGGER IF NOT EXISTS memory_ad AFTER DELETE ON memory_items BEGIN
                      INSERT INTO memory_fts(memory_fts, rowid, content) VALUES('delete', old.rowid, old.content);
                    END
                    """);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open memory store: " + dbPath, e);
        }
    }

    public void upsert(MemoryItem item) {
        String sql = """
            INSERT INTO memory_items(id, tier, content, embedding, tags, source, confidence,
                                     created_at, last_accessed, access_count, tokens)
            VALUES(?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
              tier=excluded.tier,
              content=excluded.content,
              embedding=excluded.embedding,
              tags=excluded.tags,
              source=excluded.source,
              confidence=excluded.confidence,
              last_accessed=excluded.last_accessed,
              access_count=access_count+1,
              tokens=excluded.tokens
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.id());
            ps.setString(2, item.tier().name());
            ps.setString(3, item.content());
            ps.setBytes(4, item.embedding() == null ? null : Vectors.toBytes(item.embedding()));
            ps.setString(5, encodeTags(item.tags()));
            ps.setString(6, item.source());
            ps.setDouble(7, item.confidence());
            ps.setLong(8, item.createdAt().toEpochMilli());
            ps.setLong(9, item.lastAccessedAt().toEpochMilli());
            ps.setInt(10, item.accessCount());
            ps.setInt(11, item.tokens());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<MemoryItem> findById(String id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM memory_items WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    /** Full-text (BM25) prefilter — used before vector re-ranking. */
    public List<MemoryItem> ftsSearch(String query, int limit) {
        String sql = """
            SELECT m.* FROM memory_fts f
            JOIN memory_items m ON m.rowid = f.rowid
            WHERE memory_fts MATCH ?
            ORDER BY rank LIMIT ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sanitizeFts(query));
            ps.setInt(2, limit);
            return collect(ps);
        } catch (SQLException e) {
            // FTS syntax errors on weird queries — fall back to empty
            return List.of();
        }
    }

    public List<MemoryItem> recent(MemoryItem.Tier tier, int limit) {
        String sql = "SELECT * FROM memory_items WHERE tier = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tier.name());
            ps.setInt(2, limit);
            return collect(ps);
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public List<MemoryItem> all() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM memory_items")) {
            List<MemoryItem> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public void delete(String id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM memory_items WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public Map<MemoryItem.Tier, Long> countsByTier() {
        Map<MemoryItem.Tier, Long> out = new EnumMap<>(MemoryItem.Tier.class);
        for (MemoryItem.Tier t : MemoryItem.Tier.values()) out.put(t, 0L);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT tier, COUNT(*) c FROM memory_items GROUP BY tier")) {
            while (rs.next()) {
                out.put(MemoryItem.Tier.valueOf(rs.getString("tier")), rs.getLong("c"));
            }
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return out;
    }

    public void touch(String id) {
        String sql = "UPDATE memory_items SET last_accessed = ?, access_count = access_count + 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override
    public void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }

    // ---------------------- helpers ----------------------

    private static List<MemoryItem> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<MemoryItem> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    private static MemoryItem map(ResultSet rs) throws SQLException {
        byte[] emb = rs.getBytes("embedding");
        return new MemoryItem(
                rs.getString("id"),
                MemoryItem.Tier.valueOf(rs.getString("tier")),
                rs.getString("content"),
                emb == null ? null : Vectors.fromBytes(emb),
                decodeTags(rs.getString("tags")),
                rs.getString("source"),
                rs.getDouble("confidence"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                Instant.ofEpochMilli(rs.getLong("last_accessed")),
                rs.getInt("access_count"),
                rs.getInt("tokens")
        );
    }

    private static String encodeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        tags.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(k.replace(";", "")).append('=').append(v == null ? "" : v.replace(";", ""));
        });
        return sb.toString();
    }

    private static Map<String, String> decodeTags(String s) {
        if (s == null || s.isEmpty()) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (String piece : s.split(";")) {
            int eq = piece.indexOf('=');
            if (eq > 0) out.put(piece.substring(0, eq), piece.substring(eq + 1));
        }
        return out;
    }

    /** Strip characters that would break FTS5 query syntax. */
    private static String sanitizeFts(String q) {
        if (q == null) return "";
        StringBuilder sb = new StringBuilder(q.length());
        for (char c : q.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') sb.append(c);
            else sb.append(' ');
        }
        String cleaned = sb.toString().trim();
        if (cleaned.isEmpty()) return "\"\"";
        // Quote each term to avoid accidental operators.
        String[] terms = cleaned.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String t : terms) {
            if (t.length() < 2) continue;
            if (out.length() > 0) out.append(" OR ");
            out.append('"').append(t).append('"');
        }
        return out.length() == 0 ? "\"\"" : out.toString();
    }
}
