package com.coloc.back.service;

import com.coloc.back.dto.CreateNoteDTO;
import com.coloc.back.dto.NoteDTO;
import com.coloc.back.dto.PollOptionDTO;
import com.coloc.back.entity.Note;
import com.coloc.back.entity.NoteType;
import com.coloc.back.entity.PollOption;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.repository.PollOptionRepository;
import com.coloc.back.repository.PollVoteRepository;
import com.coloc.back.repository.RoommateRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final RoommateRepository roommateRepository;

    public NoteService(
            NoteRepository noteRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            RoommateRepository roommateRepository) {

        this.noteRepository = noteRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.roommateRepository = roommateRepository;
    }

    // Récupère le colocataire authentifié grâce au JWT.
    private Roommate getCurrentRoommate() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof Long roommateId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié.");
        }

        return roommateRepository.findById(roommateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Colocataire introuvable."));
    }

    // Récupère la colocation de l'utilisateur connecté.
    private SharedHouse getCurrentHouse(Roommate roommate) {
        if (roommate.getSharedHouse() == null
                || roommate.getSharedHouse().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'appartenez à aucune colocation.");
        }

        return roommate.getSharedHouse();
    }

    // Vérifie qu'une note appartient à la colocation de l'utilisateur.
    private Note getNoteInCurrentHouse(Long noteId, SharedHouse house) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Note introuvable."));

        if (note.getSharedHouse() == null
                || !house.getId().equals(note.getSharedHouse().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Note introuvable.");
        }

        return note;
    }

    @Transactional(readOnly = true)
    public List<NoteDTO> getNotes() {
        Roommate roommate = getCurrentRoommate();
        SharedHouse house = getCurrentHouse(roommate);

        return noteRepository
                .findBySharedHouse_IdOrderByCreatedAtDesc(house.getId())
                .stream()
                .map(note -> toDTO(note, roommate.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteDTO getNoteById(Long noteId) {
        Roommate roommate = getCurrentRoommate();
        SharedHouse house = getCurrentHouse(roommate);

        Note note = getNoteInCurrentHouse(noteId, house);
        return toDTO(note, roommate.getId());
    }

    @Transactional
    public NoteDTO createNote(CreateNoteDTO dto) {
        validateCreateNote(dto);

        Roommate creator = getCurrentRoommate();
        SharedHouse house = getCurrentHouse(creator);

        Note note = new Note();
        note.setType(dto.getType());
        note.setContent(dto.getContent().trim());
        note.setCreatedAt(LocalDateTime.now());
        note.setCreator(creator);
        note.setSharedHouse(house);

        if (dto.getType() == NoteType.SONDAGE) {
            note.setMultipleAnswers(dto.isMultipleAnswers());
            note.setPollEndsAt(dto.getPollEndsAt());
        } else {
            note.setMultipleAnswers(false);
            note.setPollEndsAt(null);
        }

        Note savedNote = noteRepository.save(note);

        if (dto.getType() == NoteType.SONDAGE) {
            for (String label : dto.getOptions()) {
                PollOption option = new PollOption();
                option.setLabel(label.trim());
                option.setNote(savedNote);
                pollOptionRepository.save(option);
            }
        }

        return toDTO(savedNote, creator.getId());
    }

    @Transactional
    public void deleteNote(Long noteId) {
        Roommate roommate = getCurrentRoommate();
        SharedHouse house = getCurrentHouse(roommate);
        Note note = getNoteInCurrentHouse(noteId, house);

        if (!note.getCreator().getId().equals(roommate.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seul le créateur peut supprimer cette note.");
        }

        noteRepository.delete(note);
    }

    private void validateCreateNote(CreateNoteDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les données de la note sont obligatoires.");
        }

        if (dto.getType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le type de note est obligatoire.");
        }

        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le contenu de la note est obligatoire.");
        }

        if (dto.getType() == NoteType.SONDAGE) {
            if (dto.getPollEndsAt() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La date de fin du sondage est obligatoire.");
            }

            if (!dto.getPollEndsAt().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La date de fin doit être dans le futur.");
            }

            if (dto.getOptions() == null || dto.getOptions().size() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Un sondage doit avoir au moins deux options.");
            }

            boolean invalidOption = dto.getOptions().stream()
                    .anyMatch(option -> option == null || option.isBlank());

            if (invalidOption) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Les options ne peuvent pas être vides.");
            }

            if (dto.getOptions().stream()
                    .map(String::trim)
                    .distinct()
                    .count() != dto.getOptions().size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Les options d'un sondage doivent être différentes.");
            }
        }
    }

    private NoteDTO toDTO(Note note, Long currentRoommateId) {
        NoteDTO dto = new NoteDTO();

        dto.setId(note.getId());
        dto.setType(note.getType());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());

        // Date de fin : null pour une annonce.
        dto.setPollEndsAt(note.getPollEndsAt());

        Roommate creator = note.getCreator();

        dto.setCreatorId(creator.getId());
        dto.setCreatorName(
                creator.getName() + " " + creator.getSurname());
        dto.setCreatorInitials(
                getInitial(creator.getName())
                        + getInitial(creator.getSurname()));

        dto.setSharedHouseId(note.getSharedHouse().getId());
        dto.setMultipleAnswers(note.isMultipleAnswers());
        dto.setCanDelete(
                currentRoommateId != null
                        && creator.getId().equals(currentRoommateId));

        List<PollOptionDTO> optionDTOs = new ArrayList<>();

        if (note.getType() == NoteType.SONDAGE) {
            List<PollOption> options =
                    pollOptionRepository
                            .findByNote_IdOrderByIdAsc(note.getId());

            for (PollOption option : options) {
                PollOptionDTO optionDTO = new PollOptionDTO();
                optionDTO.setId(option.getId());
                optionDTO.setLabel(option.getLabel());
                optionDTO.setVoteCount(
                        pollVoteRepository.countByOption_Id(option.getId()));

                boolean voted = currentRoommateId != null
                        && pollVoteRepository
                                .existsByOption_IdAndRoommate_Id(
                                        option.getId(), currentRoommateId);

                optionDTO.setVotedByCurrentUser(voted);
                optionDTOs.add(optionDTO);
            }
        }

        dto.setOptions(optionDTOs);
        return dto;
    }

    private String getInitial(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().substring(0, 1).toUpperCase();
    }
}