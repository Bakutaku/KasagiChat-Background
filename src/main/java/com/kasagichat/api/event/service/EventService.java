package com.kasagichat.api.event.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kasagichat.api.event.controller.dto.request.CreateEventRequest;
import com.kasagichat.api.event.controller.dto.response.EventParticipantResponse;
import com.kasagichat.api.event.controller.dto.response.EventResponse;
import com.kasagichat.api.event.controller.dto.response.InvitationResponse;
import com.kasagichat.api.event.exception.EventException;
import com.kasagichat.api.event.model.Event;
import com.kasagichat.api.event.model.EventParticipant;
import com.kasagichat.api.event.model.enums.EventPhase;
import com.kasagichat.api.event.repository.EventParticipantRepository;
import com.kasagichat.api.event.repository.EventRepository;
import com.kasagichat.api.npc.model.Npc;
import com.kasagichat.api.npc.repository.NpcRepository;
import com.kasagichat.api.security.model.Users;
import com.kasagichat.api.security.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * イベントの作成・招待・参加を扱う。
 *
 * <p>参加者以外からの参照は、存在を秘匿するためすべて EVENT_NOT_FOUND で返す。
 * 開催フェーズは列で持たず、参照のたびに日時から算出する。</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final UserService userService;
    private final NpcRepository npcRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    /** 作成者も自動で参加するため、作成にも分身の誕生を求める。 */
    @Transactional
    public EventResponse create(Long userId, CreateEventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw EventException.invalidEventPeriod();
        }
        requireBornNpc(userId);
        Users creator = userService.getCurrentUser(userId);
        Instant now = Instant.now();
        Event event = eventRepository.saveAndFlush(Event.builder()
            .creator(creator)
            .title(request.title())
            .description(request.description())
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .venueTemplate(request.venueTemplate())
            .inviteCode(inviteCodeGenerator.generate())
            .build());
        eventParticipantRepository.saveAndFlush(EventParticipant.builder()
            .event(event).user(creator).joinedAt(now).build());
        return toResponse(event, userId, true, now);
    }

    public List<EventResponse> list(Long userId) {
        List<Event> events = eventRepository.findVisibleTo(userId);
        if (events.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        Map<Long, Long> counts = participantCounts(events);
        Map<Long, String> creatorNames = creatorNames(events);
        Set<Long> joinedEventIds = new HashSet<>(eventParticipantRepository.findActiveEventIds(userId));
        return events.stream()
            .map(event -> toResponse(
                event,
                userId,
                creatorNames.get(event.getCreator().getId()),
                counts.getOrDefault(event.getId(), 0L),
                joinedEventIds.contains(event.getId()),
                now))
            .toList();
    }

    public EventResponse get(Long userId, UUID eventId) {
        Instant now = Instant.now();
        Event event = visibleEvent(userId, eventId);
        return toResponse(event, userId, isJoined(event, userId), now);
    }

    public List<EventParticipantResponse> participants(Long userId, UUID eventId) {
        Event event = visibleEvent(userId, eventId);
        return eventParticipantRepository.findVenueParticipants(event.getId());
    }

    public InvitationResponse invitation(Long userId, String inviteCode) {
        Event event = eventByInviteCode(inviteCode);
        Instant now = Instant.now();
        return new InvitationResponse(
            event.getPublicId(),
            event.getTitle(),
            event.getDescription(),
            event.getStartsAt(),
            event.getEndsAt(),
            event.getVenueTemplate(),
            phaseOf(event, now),
            creatorName(event),
            eventParticipantRepository.countByEventIdAndLeftAtIsNull(event.getId()),
            isJoined(event, userId)
        );
    }

    /** 参加済みでも成功として扱う。会場へそのまま進めるよう、イベントの詳細を返す。 */
    @Transactional
    public EventResponse join(Long userId, String inviteCode) {
        Event event = eventByInviteCode(inviteCode);
        Instant now = Instant.now();
        if (phaseOf(event, now) == EventPhase.ENDED) {
            throw EventException.eventEnded();
        }
        requireBornNpc(userId);
        Optional<EventParticipant> existing =
            eventParticipantRepository.findByEventIdAndUserId(event.getId(), userId);
        if (existing.isEmpty()) {
            Users user = userService.getCurrentUser(userId);
            try {
                eventParticipantRepository.saveAndFlush(EventParticipant.builder()
                    .event(event).user(user).joinedAt(now).build());
            } catch (DataIntegrityViolationException exception) {
                // 同時に参加した場合は一意制約で弾かれる。すでに参加できているので成功として扱う。
            }
        } else if (existing.get().getLeftAt() != null) {
            EventParticipant participant = existing.get();
            // 再参加でも参加日時は更新しない。会場の座席は参加日時順のため、並びが動かないようにする。
            participant.setLeftAt(null);
            eventParticipantRepository.saveAndFlush(participant);
        }
        // TODO: タグ一致マッチングを実行し、出会いカードを生成する(次のブランチ)。
        // TODO: 実績カウンター EVENT_JOINED を加算し FIRST_EVENT を判定する(実績基盤の実装後)。
        return toResponse(event, userId, true, now);
    }

    @Transactional
    public void leave(Long userId, UUID eventId) {
        Event event = eventRepository.findByPublicId(eventId).orElseThrow(EventException::eventNotFound);
        EventParticipant participant = eventParticipantRepository
            .findByEventIdAndUserId(event.getId(), userId)
            .filter(record -> record.getLeftAt() == null)
            .orElseThrow(EventException::eventNotFound);
        participant.setLeftAt(Instant.now());
        eventParticipantRepository.saveAndFlush(participant);
    }

    /** 作り間違いの救済のみを許す。誰かが参加した後は終了(アーカイブ)しかできない。 */
    @Transactional
    public void delete(Long userId, UUID eventId) {
        Event event = creatorEvent(userId, eventId);
        if (eventParticipantRepository.existsByEventIdAndUserIdNot(event.getId(), userId)) {
            throw EventException.eventHasParticipants();
        }
        // 参加記録がイベントを参照しているため、先に消す。
        eventParticipantRepository.deleteByEventId(event.getId());
        eventRepository.delete(event);
    }

    @Transactional
    public EventResponse archive(Long userId, UUID eventId) {
        Event event = creatorEvent(userId, eventId);
        Instant now = Instant.now();
        if (event.getArchivedAt() == null) {
            event.setArchivedAt(now);
            eventRepository.saveAndFlush(event);
        }
        return toResponse(event, userId, isJoined(event, userId), now);
    }

    private Event visibleEvent(Long userId, UUID eventId) {
        Event event = eventRepository.findByPublicId(eventId).orElseThrow(EventException::eventNotFound);
        if (!isCreator(event, userId) && !isJoined(event, userId)) {
            throw EventException.eventNotFound();
        }
        return event;
    }

    private Event creatorEvent(Long userId, UUID eventId) {
        Event event = eventRepository.findByPublicId(eventId).orElseThrow(EventException::eventNotFound);
        if (!isCreator(event, userId)) {
            throw EventException.eventNotFound();
        }
        return event;
    }

    /** 掲示やQRからの入力で大文字小文字が揺れるため、照合前に正規化する。 */
    private Event eventByInviteCode(String inviteCode) {
        String normalized = inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT);
        return eventRepository.findByInviteCode(normalized).orElseThrow(EventException::eventNotFound);
    }

    private void requireBornNpc(Long userId) {
        if (!npcRepository.existsByUserIdAndBornAtIsNotNull(userId)) {
            throw EventException.npcNotBorn();
        }
    }

    private boolean isCreator(Event event, Long userId) {
        return Objects.equals(event.getCreator().getId(), userId);
    }

    private boolean isJoined(Event event, Long userId) {
        return eventParticipantRepository.findByEventIdAndUserId(event.getId(), userId)
            .filter(participant -> participant.getLeftAt() == null)
            .isPresent();
    }

    private EventPhase phaseOf(Event event, Instant now) {
        return EventPhase.of(event.getStartsAt(), event.getEndsAt(), event.getArchivedAt(), now);
    }

    private Map<Long, Long> participantCounts(List<Event> events) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : eventParticipantRepository.countActiveByEventIds(
                events.stream().map(Event::getId).toList())) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Map<Long, String> creatorNames(List<Event> events) {
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : npcRepository.findNamesByUserIds(
                events.stream().map(event -> event.getCreator().getId()).distinct().toList())) {
            names.put((Long) row[0], (String) row[1]);
        }
        return names;
    }

    /** 本名を出さないため、作成者の表示にも分身の名前を使う。 */
    private String creatorName(Event event) {
        return npcRepository.findByUserId(event.getCreator().getId()).map(Npc::getName).orElse(null);
    }

    private EventResponse toResponse(Event event, Long userId, boolean joined, Instant now) {
        return toResponse(
            event,
            userId,
            creatorName(event),
            eventParticipantRepository.countByEventIdAndLeftAtIsNull(event.getId()),
            joined,
            now);
    }

    private EventResponse toResponse(
        Event event, Long userId, String creatorName, long participantCount, boolean joined, Instant now
    ) {
        boolean owner = isCreator(event, userId);
        return new EventResponse(
            event.getPublicId(),
            event.getTitle(),
            event.getDescription(),
            event.getStartsAt(),
            event.getEndsAt(),
            event.getVenueTemplate(),
            phaseOf(event, now),
            creatorName,
            participantCount,
            owner,
            joined,
            // 誰を招くかは主催者が決める。参加者へは招待コードを渡さない。
            owner ? event.getInviteCode() : null
        );
    }
}
