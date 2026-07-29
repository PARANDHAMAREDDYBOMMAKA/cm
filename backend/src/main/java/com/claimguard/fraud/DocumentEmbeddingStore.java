package com.claimguard.fraud;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

@Repository
public class DocumentEmbeddingStore {

    private final JdbcTemplate jdbc;

    public DocumentEmbeddingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(UUID documentId, UUID claimId, String model, float[] embedding) {
        jdbc.update("""
                insert into document_embedding (document_id, claim_id, model, embedding, created_at)
                values (?, ?, ?, ?::vector, ?)
                on conflict (document_id) do update
                set claim_id = excluded.claim_id,
                    model = excluded.model,
                    embedding = excluded.embedding,
                    created_at = excluded.created_at
                """,
                documentId, claimId, model, literal(embedding), java.sql.Timestamp.from(Instant.now()));
    }

    public List<Match> findSimilar(UUID claimId, float[] embedding, double minSimilarity, int limit) {
        return jdbc.query("""
                select document_id, claim_id, 1 - (embedding <=> ?::vector) as similarity
                from document_embedding
                where claim_id <> ?
                order by embedding <=> ?::vector
                limit ?
                """,
                (rs, row) -> new Match(
                        rs.getObject("document_id", UUID.class),
                        rs.getObject("claim_id", UUID.class),
                        rs.getDouble("similarity")),
                literal(embedding), claimId, literal(embedding), limit)
                .stream()
                .filter(match -> match.similarity() >= minSimilarity)
                .toList();
    }

    private static String literal(float[] embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }

    public record Match(UUID documentId, UUID claimId, double similarity) {
    }
}
