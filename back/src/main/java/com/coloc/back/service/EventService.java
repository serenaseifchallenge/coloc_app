package com.coloc.back.service;

import com.coloc.back.dto.EventRequest;
import com.coloc.back.dto.EventResponse;
import com.coloc.back.entity.Event;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.EventRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.repository.SharedHouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RoommateRepository roommateRepository;
    private final SharedHouseRepository sharedHouseRepository;

    public EventService(
            EventRepository eventRepository,
            RoommateRepository roommateRepository,
            SharedHouseRepository sharedHouseRepository
    ) {
        this.eventRepository = eventRepository;
        this.roommateRepository = roommateRepository;
        this.sharedHouseRepository = sharedHouseRepository;
    }

    public List<EventResponse> getEvents(Long sharedHouseId) {

        return eventRepository
                .findBySharedHouseIdOrderByStartDateAscStartTimeAsc(sharedHouseId)
                .stream()
                .map(EventResponse::fromEntity)
                .toList();
    }

    public EventResponse getEvent(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Événement introuvable"
                        )
                );

        return EventResponse.fromEntity(event);
    }

    public EventResponse createEvent(EventRequest request) {

        validateDatesAndTimes(request);

        Roommate creator = roommateRepository.findById(request.creatorId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Colocataire introuvable"
                        )
                );

        SharedHouse sharedHouse = sharedHouseRepository.findById(request.sharedHouseId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Colocation introuvable"
                        )
                );

        if (creator.getSharedHouse() == null
                || !creator.getSharedHouse().getId().equals(sharedHouse.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le créateur n'appartient pas à cette colocation"
            );
        }

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

        validateDatesAndTimes(request);

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Événement introuvable"
                        )
                );

        SharedHouse sharedHouse = sharedHouseRepository.findById(request.sharedHouseId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Colocation introuvable"
                        )
                );

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setAllDay(request.allDay());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setSharedHouse(sharedHouse);

        Event savedEvent = eventRepository.save(event);

        return EventResponse.fromEntity(savedEvent);
    }

    public void deleteEvent(Long id) {

        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Événement introuvable"
            );
        }

        eventRepository.deleteById(id);
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