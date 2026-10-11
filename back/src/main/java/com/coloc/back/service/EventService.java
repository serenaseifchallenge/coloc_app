package com.coloc.back.service;

import com.coloc.back.dto.EventRequest;
import com.coloc.back.dto.EventResponse;
import com.coloc.back.entity.Event;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.EventRepository;
import com.coloc.back.repository.RoommateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final RoommateRepository roommateRepository;

    public EventService(
            EventRepository eventRepository,
            RoommateRepository roommateRepository
    ) {
        this.eventRepository = eventRepository;
        this.roommateRepository = roommateRepository;
    }

    private Roommate getCurrentRoommate() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long roommateId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        return roommateRepository.findById(roommateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Compte utilisateur introuvable"
                ));
    }

    private SharedHouse getCurrentSharedHouse(Roommate roommate) {
        SharedHouse sharedHouse = roommate.getSharedHouse();

        if (sharedHouse == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous devez appartenir à une colocation pour accéder au calendrier"
            );
        }

        return sharedHouse;
    }

    public List<EventResponse> getEvents() {
        Roommate roommate = getCurrentRoommate();
        SharedHouse sharedHouse = getCurrentSharedHouse(roommate);

        return eventRepository
                .findBySharedHouseIdOrderByStartDateAscStartTimeAsc(
                        sharedHouse.getId()
                )
                .stream()
                .map(EventResponse::fromEntity)
                .toList();
    }

    public EventResponse getEvent(Long id) {
        Roommate roommate = getCurrentRoommate();
        SharedHouse sharedHouse = getCurrentSharedHouse(roommate);
        Event event = findEventInHouse(id, sharedHouse);

        return EventResponse.fromEntity(event);
    }

    public EventResponse createEvent(EventRequest request) {
        Roommate creator = getCurrentRoommate();
        SharedHouse sharedHouse = getCurrentSharedHouse(creator);

        validateDatesAndTimes(request);

        Event event = new Event();
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setAllDay(request.allDay());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setCreator(creator);
        event.setSharedHouse(sharedHouse);

        Event savedEvent = eventRepository.save(event);

        return EventResponse.fromEntity(savedEvent);
    }

    public EventResponse updateEvent(Long id, EventRequest request) {
        Roommate roommate = getCurrentRoommate();
        SharedHouse sharedHouse = getCurrentSharedHouse(roommate);
        Event event = findEventInHouse(id, sharedHouse);

        validateDatesAndTimes(request);

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setAllDay(request.allDay());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndTime(request.allDay() ? null : request.endTime());

        // Le créateur et la colocation ne sont jamais modifiés par la requête.
        Event savedEvent = eventRepository.save(event);

        return EventResponse.fromEntity(savedEvent);
    }

    public void deleteEvent(Long id) {
        Roommate roommate = getCurrentRoommate();
        SharedHouse sharedHouse = getCurrentSharedHouse(roommate);
        Event event = findEventInHouse(id, sharedHouse);

        eventRepository.delete(event);
    }

    private Event findEventInHouse(Long id, SharedHouse sharedHouse) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement introuvable"
                ));

        if (event.getSharedHouse() == null
                || !event.getSharedHouse().getId().equals(sharedHouse.getId())) {
            // Ne révèle pas l'existence d'un événement d'une autre colocation.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Événement introuvable"
            );
        }

        return event;
    }

    private void validateDatesAndTimes(EventRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La date de fin doit être postérieure ou égale à la date de début"
            );
        }

        if (request.allDay()) {
            return;
        }

        if (request.startTime() == null || request.endTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les heures de début et de fin sont obligatoires pour un événement avec horaires"
            );
        }

        if (request.startDate().equals(request.endDate())
                && !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être postérieure à l'heure de début"
            );
        }
    }
}
