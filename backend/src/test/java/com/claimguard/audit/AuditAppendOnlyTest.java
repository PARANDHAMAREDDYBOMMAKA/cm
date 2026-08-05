package com.claimguard.audit;

import com.claimguard.DotenvInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ContextConfiguration(initializers = DotenvInitializer.class)
@Transactional
class AuditAppendOnlyTest {

    @Autowired
    private JdbcTemplate jdbc;

    private Long anySeq() {
        return jdbc.queryForObject("select min(seq) from audit_event", Long.class);
    }

    @Test
    void anExistingEntryCannotBeUpdated() {
        Long seq = anySeq();
        assumeTrue(seq != null, "needs at least one audit entry");

        assertThatThrownBy(() -> jdbc.update(
                "update audit_event set summary = 'tampered' where seq = ?", seq))
                .hasMessageContaining("append-only");
    }

    @Test
    void anExistingEntryCannotBeDeleted() {
        Long seq = anySeq();
        assumeTrue(seq != null, "needs at least one audit entry");

        assertThatThrownBy(() -> jdbc.update("delete from audit_event where seq = ?", seq))
                .hasMessageContaining("append-only");
    }
}
