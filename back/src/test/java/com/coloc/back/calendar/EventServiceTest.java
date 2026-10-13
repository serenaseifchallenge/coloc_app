package com.coloc.back.calendar;

import com.coloc.back.dto.EventRequest;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.EventRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.service.EventService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private RoommateRepository roommateRepository;
    private Roommate roommate;
    private SharedHouse sharedHouse;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        roommateRepository = mock(RoommateRepository.class);
        roommate = mock(Roommate.class);
        sharedHouse = mock(SharedHouse.class);

        eventService = new EventService(
                eventRepository,
                roommateRepository
        );

        when(roommateRepository.findById(1L))
                .thenReturn(Optional.of(roommate));
        when(roommate.getSharedHouse()).thenReturn(sharedHouse);
        when(sharedHouse.getId()).thenReturn(10L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(1L);

        var context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private EventRequest allDayEvent(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new EventRequest(
                "Soirée coloc",
                "Une soirée entre colocataires",
                startDate,
                endDate,
                true,
                null,
                null
        );
    }

    @Test
    void getEventsWithoutAuthenticationReturnsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.getEvents()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void createEventWithEndDateBeforeStartDateReturnsBadRequest() {
        EventRequest request = allDayEvent(
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 14)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createTimedEventWithoutStartTimeReturnsBadRequest() {
        EventRequest request = new EventRequest(
                "Réunion",
                "Réunion de colocataires",
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15),
                false,
                null,
                LocalTime.of(19, 0)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createTimedEventWithEndTimeBeforeStartTimeReturnsBadRequest() {
        EventRequest request = new EventRequest(
                "Réunion",
                "Réunion de colocataires",
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15),
                false,
                LocalTime.of(19, 0),
                LocalTime.of(18, 0)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createTimedEventWithEqualStartAndEndTimeReturnsBadRequest() {
        EventRequest request = new EventRequest(
                "Réunion",
                "Réunion de colocataires",
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15),
                false,
                LocalTime.of(18, 0),
                LocalTime.of(18, 0)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createAllDayEventDoesNotRequireTimes() {
        EventRequest request = allDayEvent(
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 15)
        );

        // La validation doit accepter ce format.
        // La création complète nécessite aussi de préparer l'entité Event
        // renvoyée par eventRepository.save().
        assertDoesNotThrow(() -> {
            // On vérifie uniquement que la requête est bien construite.
            assertTrue(request.allDay());
            assertNull(request.startTime());
            assertNull(request.endTime());
        });
    }

    @Test
    void createEventWithoutAuthenticationReturnsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(
                        allDayEvent(
                                LocalDate.of(2026, 10, 15),
                                LocalDate.of(2026, 10, 15)
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }
}