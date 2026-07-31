package com.claimguard.fraud;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskBandTest {

    @Test
    void bandsFollowTheDocumentedBoundaries() {
        assertThat(RiskBand.of(0)).isEqualTo(RiskBand.CLEAN);
        assertThat(RiskBand.of(1)).isEqualTo(RiskBand.LOW);
        assertThat(RiskBand.of(24)).isEqualTo(RiskBand.LOW);
        assertThat(RiskBand.of(25)).isEqualTo(RiskBand.MEDIUM);
        assertThat(RiskBand.of(49)).isEqualTo(RiskBand.MEDIUM);
        assertThat(RiskBand.of(50)).isEqualTo(RiskBand.HIGH);
        assertThat(RiskBand.of(74)).isEqualTo(RiskBand.HIGH);
        assertThat(RiskBand.of(75)).isEqualTo(RiskBand.CRITICAL);
        assertThat(RiskBand.of(100)).isEqualTo(RiskBand.CRITICAL);
    }

    @Test
    void onlyHighAndCriticalNeedReview() {
        assertThat(RiskBand.CLEAN.needsReview()).isFalse();
        assertThat(RiskBand.LOW.needsReview()).isFalse();
        assertThat(RiskBand.MEDIUM.needsReview()).isFalse();
        assertThat(RiskBand.HIGH.needsReview()).isTrue();
        assertThat(RiskBand.CRITICAL.needsReview()).isTrue();
    }

    @Test
    void aSingleCriticalSignalIsEnoughToNeedReview() {
        RiskBand band = RiskBand.of(Severity.CRITICAL.weight());

        assertThat(band).isEqualTo(RiskBand.HIGH);
        assertThat(band.needsReview()).isTrue();
    }

    @Test
    void aSingleHighSignalScoresBelowTheReviewBandOnItsOwn() {
        RiskBand band = RiskBand.of(Severity.HIGH.weight());

        assertThat(band).isEqualTo(RiskBand.MEDIUM);
        assertThat(band.needsReview()).isFalse();
    }

    @Test
    void twoHighSignalsReachTheReviewBand() {
        assertThat(RiskBand.of(Severity.HIGH.weight() * 2).needsReview()).isTrue();
    }
}
