package com.kasagichat.api.event.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** フェーズは列で持たず日時から算出するため、境界の扱いを固定する。 */
class EventPhaseTest {
    private final Instant startsAt = Instant.parse("2026-03-01T00:00:00Z");
    private final Instant endsAt = Instant.parse("2026-03-02T00:00:00Z");

    @Test
    void treatsTimeBeforeStartAsUpcoming() {
        assertThat(EventPhase.of(startsAt, endsAt, null, startsAt.minusSeconds(1)))
            .isEqualTo(EventPhase.UPCOMING);
    }

    @Test
    void treatsStartAndEndBoundariesAsOngoing() {
        assertThat(EventPhase.of(startsAt, endsAt, null, startsAt)).isEqualTo(EventPhase.ONGOING);
        assertThat(EventPhase.of(startsAt, endsAt, null, endsAt)).isEqualTo(EventPhase.ONGOING);
    }

    @Test
    void treatsTimeAfterEndAsEnded() {
        assertThat(EventPhase.of(startsAt, endsAt, null, endsAt.plusSeconds(1)))
            .isEqualTo(EventPhase.ENDED);
    }

    @Test
    void treatsArchivedEventAsEndedEvenBeforeStart() {
        Instant archivedAt = Instant.parse("2026-02-01T00:00:00Z");
        assertThat(EventPhase.of(startsAt, endsAt, archivedAt, startsAt.minusSeconds(1)))
            .isEqualTo(EventPhase.ENDED);
    }
}
