package com.coloc.back.notes;

import com.coloc.back.dto.CreateNoteDTO;
import com.coloc.back.entity.NoteType;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.repository.PollOptionRepository;
import com.coloc.back.repository.PollVoteRepository;
import com.coloc.back.repository.RoommateRepository;
import com.coloc.back.service.NoteService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteServiceTest {

    private NoteService noteService;
    private NoteRepository noteRepository;
    private PollOptionRepository pollOptionRepository;
    private PollVoteRepository pollVoteRepository;
    private RoommateRepository roommateRepository;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        pollOptionRepository = mock(PollOptionRepository.class);
        pollVoteRepository = mock(PollVoteRepository.class);
        roommateRepository = mock(RoommateRepository.class);

        noteService = new NoteService(
                noteRepository,
                pollOptionRepository,
                pollVoteRepository,
                roommateRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CreateNoteDTO createDTO(NoteType type, String content) {
        CreateNoteDTO dto = new CreateNoteDTO();
        dto.setType(type);
        dto.setContent(content);
        return dto;
    }

    private NoteType nonPollType() {
        return java.util.Arrays.stream(NoteType.values())
                .filter(type -> type != NoteType.SONDAGE)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Il faut au moins un type de note autre que SONDAGE."
                        ));
    }

    @Test
    void createNoteWithNullDTOReturnsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createNoteWithoutTypeReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(null, "Message de test");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createNoteWithBlankContentReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(nonPollType(), "   ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createPollWithoutEndDateReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(NoteType.SONDAGE, "Choisir une date");
        dto.setOptions(List.of("Samedi", "Dimanche"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createPollWithPastEndDateReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(NoteType.SONDAGE, "Choisir une date");
        dto.setOptions(List.of("Samedi", "Dimanche"));
        dto.setPollEndsAt(LocalDateTime.now().minusDays(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createPollWithOnlyOneOptionReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(NoteType.SONDAGE, "Choisir une date");
        dto.setOptions(List.of("Samedi"));
        dto.setPollEndsAt(LocalDateTime.now().plusDays(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createPollWithBlankOptionReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(NoteType.SONDAGE, "Choisir une date");
        dto.setOptions(List.of("Samedi", "  "));
        dto.setPollEndsAt(LocalDateTime.now().plusDays(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createPollWithDuplicateOptionsReturnsBadRequest() {
        CreateNoteDTO dto = createDTO(NoteType.SONDAGE, "Choisir une date");
        dto.setOptions(List.of("Samedi", " Samedi "));
        dto.setPollEndsAt(LocalDateTime.now().plusDays(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getNotesWithoutAuthenticationReturnsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.getNotes()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void createNoteWithInvalidDTODoesNotSaveNote() {
        CreateNoteDTO dto = createDTO(nonPollType(), "");

        assertThrows(
                ResponseStatusException.class,
                () -> noteService.createNote(dto)
        );

        verify(noteRepository, never()).save(any());
    }

    @Test
    void deleteNoteWithoutAuthenticationReturnsUnauthorized() {
        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> noteService.deleteNote(1L)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(noteRepository, never()).delete(any());
    }
}