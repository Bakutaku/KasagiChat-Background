package com.kasagichat.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kasagichat.api.common.exception.BaseException;
import com.kasagichat.api.event.controller.dto.request.CreateEventRequest;
import com.kasagichat.api.event.controller.dto.response.EventParticipantResponse;
import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.model.enums.VenueTemplate;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

/** DBやSpringコンテキストを起動しない、参加ライフサイクルと可視性の単体テスト。 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock private UserService userService;
    @Mock private NpcRepository npcRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private InviteCodeGenerator inviteCodeGenerator;
    @Mock private CardMatchingService cardMatchingService;

    private EventService service;
    private Users creator;
    private Users guest;
    private Event event;
    private final UUID eventId = UUID.randomUUID();
    private final Instant startsAt = Instant.parse("2026-03-01T00:00:00Z");
    private final Instant endsAt = Instant.parse("2099-03-02T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new EventService(
            userService, npcRepository, eventRepository, eventParticipantRepository, inviteCodeGenerator,
            cardMatchingService);
        creator = Users.builder().id(7L).displayName("主催者").build();
        guest = Users.builder().id(8L).displayName("参加者").build();
        event = Event.builder().id(100L).publicId(eventId).creator(creator).title("交流会")
            .description("説明").startsAt(startsAt).endsAt(endsAt)
            .venueTemplate(VenueTemplate.HALL).inviteCode("ABCD2345").build();
    }

    @Test
    void createsEventWithInviteCodeAndAutoJoinsCreator() {
        when(npcRepository.existsByUserIdAndBornAtIsNotNull(7L)).thenReturn(true);
        when(userService.getCurrentUser(7L)).thenReturn(creator);
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
        when(eventRepository.saveAndFlush(any(Event.class))).thenReturn(event);
        stubCreatorNpc();
        when(eventParticipantRepository.countByEventIdAndLeftAtIsNull(100L)).thenReturn(1L);

        EventResponse response = service.create(7L, new CreateEventRequest(
            "交流会", "説明", startsAt, endsAt, VenueTemplate.HALL));

        ArgumentCaptor<EventParticipant> participant = ArgumentCaptor.forClass(EventParticipant.class);
        verify(eventParticipantRepository).saveAndFlush(participant.capture());
        assertThat(participant.getValue().getUser()).isSameAs(creator);
        assertThat(participant.getValue().getLeftAt()).isNull();
        assertThat(response.inviteCode()).isEqualTo("ABCD2345");
        assertThat(response.owner()).isTrue();
        assertThat(response.joined()).isTrue();
        assertThat(response.phase()).isEqualTo(EventPhase.ONGOING);
        assertThat(response.creatorName()).isEqualTo("分身");
    }

    @Test
    void rejectsCreationWhenPeriodIsReversed() {
        assertThatThrownBy(() -> service.create(7L, new CreateEventRequest(
            "交流会", "説明", endsAt, startsAt, VenueTemplate.HALL)))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("INVALID_EVENT_PERIOD"));
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsJoinBeforeNpcIsBorn() {
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));
        when(npcRepository.existsByUserIdAndBornAtIsNotNull(8L)).thenReturn(false);

        assertThatThrownBy(() -> service.join(8L, "abcd2345"))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("NPC_NOT_BORN"));
        verify(eventParticipantRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsJoinAfterEventEnded() {
        event.setArchivedAt(Instant.parse("2026-03-01T12:00:00Z"));
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.join(8L, "ABCD2345"))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_ENDED"));
        verify(npcRepository, never()).existsByUserIdAndBornAtIsNotNull(any());
    }

    @Test
    void treatsRepeatedJoinAsSuccessWithoutCreatingAnotherRecord() {
        EventParticipant joined = EventParticipant.builder()
            .id(1L).event(event).user(guest).joinedAt(startsAt).build();
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));
        when(npcRepository.existsByUserIdAndBornAtIsNotNull(8L)).thenReturn(true);
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(joined));
        stubCreatorNpc();
        when(eventParticipantRepository.countByEventIdAndLeftAtIsNull(100L)).thenReturn(2L);

        EventResponse response = service.join(8L, "ABCD2345");

        verify(eventParticipantRepository, never()).saveAndFlush(any());
        assertThat(response.joined()).isTrue();
        assertThat(response.owner()).isFalse();
        // 招待コードは主催者だけが扱う。
        assertThat(response.inviteCode()).isNull();
    }

    @Test
    void recalculatesMatchingOnEveryJoinSoCardsArriveRightAway() {
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));
        when(npcRepository.existsByUserIdAndBornAtIsNotNull(8L)).thenReturn(true);
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(
            EventParticipant.builder().id(1L).event(event).user(guest).joinedAt(startsAt).build()));
        stubCreatorNpc();
        when(eventParticipantRepository.countByEventIdAndLeftAtIsNull(100L)).thenReturn(2L);

        service.join(8L, "ABCD2345");

        verify(cardMatchingService).recalculate(100L);
    }

    @Test
    void skipsMatchingWhenJoinIsRejected() {
        event.setArchivedAt(Instant.parse("2026-03-01T12:00:00Z"));
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.join(8L, "ABCD2345")).isInstanceOf(BaseException.class);

        verify(cardMatchingService, never()).recalculate(any());
    }

    @Test
    void rejoinClearsLeftAtButKeepsOriginalJoinedAtSoSeatingStaysStable() {
        EventParticipant left = EventParticipant.builder()
            .id(1L).event(event).user(guest).joinedAt(startsAt)
            .leftAt(Instant.parse("2026-03-01T06:00:00Z")).build();
        when(eventRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(event));
        when(npcRepository.existsByUserIdAndBornAtIsNotNull(8L)).thenReturn(true);
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(left));
        stubCreatorNpc();
        when(eventParticipantRepository.countByEventIdAndLeftAtIsNull(100L)).thenReturn(2L);

        service.join(8L, "ABCD2345");

        verify(eventParticipantRepository).saveAndFlush(left);
        assertThat(left.getLeftAt()).isNull();
        assertThat(left.getJoinedAt()).isEqualTo(startsAt);
    }

    @Test
    void hidesEventFromUsersWhoAreNotParticipants() {
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(8L, eventId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_NOT_FOUND"));
    }

    @Test
    void hidesEventFromUsersWhoAlreadyLeft() {
        EventParticipant left = EventParticipant.builder()
            .id(1L).event(event).user(guest).joinedAt(startsAt)
            .leftAt(Instant.parse("2026-03-01T06:00:00Z")).build();
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(left));

        assertThatThrownBy(() -> service.participants(8L, eventId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_NOT_FOUND"));
    }

    @Test
    void returnsVenueParticipantsInServerDecidedOrder() {
        EventParticipant joined = EventParticipant.builder()
            .id(1L).event(event).user(guest).joinedAt(startsAt).build();
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(joined));
        when(eventParticipantRepository.findVenueParticipants(100L)).thenReturn(List.of(
            new EventParticipantResponse("ユウ", "cheerful-girl"),
            new EventParticipantResponse("ハル", "cool-girl")));

        assertThat(service.participants(8L, eventId))
            .extracting(EventParticipantResponse::name).containsExactly("ユウ", "ハル");
    }

    @Test
    void refusesDeleteOnceAnotherUserHasEverParticipated() {
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.existsByEventIdAndUserIdNot(100L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(7L, eventId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_HAS_PARTICIPANTS"));
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deletesParticipantRecordsBeforeTheEventItself() {
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.existsByEventIdAndUserIdNot(100L, 7L)).thenReturn(false);

        service.delete(7L, eventId);

        var order = org.mockito.Mockito.inOrder(eventParticipantRepository, eventRepository);
        order.verify(eventParticipantRepository).deleteByEventId(100L);
        order.verify(eventRepository).delete(event);
    }

    @Test
    void hidesDeleteAndArchiveFromNonCreators() {
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.delete(8L, eventId))
            .isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EVENT_NOT_FOUND"));
    }

    @Test
    void leaveMarksLeftAtWithoutRemovingTheRecord() {
        EventParticipant joined = EventParticipant.builder()
            .id(1L).event(event).user(guest).joinedAt(startsAt).build();
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 8L)).thenReturn(Optional.of(joined));

        service.leave(8L, eventId);

        verify(eventParticipantRepository).saveAndFlush(joined);
        verify(eventParticipantRepository, never()).delete(any());
        assertThat(joined.getLeftAt()).isNotNull();
    }

    @Test
    void archiveEndsTheEventForTheCreator() {
        when(eventRepository.findByPublicId(eventId)).thenReturn(Optional.of(event));
        when(eventParticipantRepository.findByEventIdAndUserId(100L, 7L)).thenReturn(Optional.of(
            EventParticipant.builder().id(1L).event(event).user(creator).joinedAt(startsAt).build()));
        stubCreatorNpc();
        when(eventParticipantRepository.countByEventIdAndLeftAtIsNull(100L)).thenReturn(1L);

        EventResponse response = service.archive(7L, eventId);

        assertThat(event.getArchivedAt()).isNotNull();
        assertThat(response.phase()).isEqualTo(EventPhase.ENDED);
    }

    private void stubCreatorNpc() {
        when(npcRepository.findByUserId(7L)).thenReturn(Optional.of(
            Npc.builder().id(11L).user(creator).name("分身").presetId("cheerful-girl").build()));
    }
}
