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
import com.coloc.back.repository.SharedHouseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final RoommateRepository roommateRepository;
    private final SharedHouseRepository sharedHouseRepository;

    public NoteService(
            NoteRepository noteRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            RoommateRepository roommateRepository,
            SharedHouseRepository sharedHouseRepository) {

        this.noteRepository = noteRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.roommateRepository = roommateRepository;
        this.sharedHouseRepository = sharedHouseRepository;
    }

    //Récupère les notes d'une colocation
    @Transactional(readOnly = true)
    public List<NoteDTO> getNotesBySharedHouse(Long sharedHouseId,
                                               Long currentRoommateId) {

        if (!sharedHouseRepository.existsById(sharedHouseId)) {
            throw new IllegalArgumentException("Colocation introuvable.");
        }

        return noteRepository
                .findBySharedHouse_IdOrderByCreatedAtDesc(sharedHouseId)
                .stream()
                .map(note -> toDTO(note, currentRoommateId))
                .toList();
    }

    //Récupère une note par son identifiant
    @Transactional(readOnly = true)
    public NoteDTO getNoteById(Long noteId, Long currentRoommateId) {

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note introuvable."));

        return toDTO(note, currentRoommateId);
    }

    //Crée une annonce ou un sondage
    @Transactional
    public NoteDTO createNote(CreateNoteDTO dto) {

        validateCreateNote(dto);

        Roommate creator = roommateRepository.findById(dto.getCreatorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Créateur introuvable."));

        SharedHouse sharedHouse = sharedHouseRepository
                .findById(dto.getSharedHouseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Colocation introuvable."));

        if (creator.getSharedHouse() == null
                || !creator.getSharedHouse().getId()
                        .equals(sharedHouse.getId())) {
            throw new IllegalArgumentException(
                    "Le créateur n'appartient pas à cette colocation.");
        }

        Note note = new Note();
        note.setType(dto.getType());
        note.setContent(dto.getContent().trim());
        note.setCreatedAt(LocalDateTime.now());
        note.setCreator(creator);
        note.setSharedHouse(sharedHouse);

        if (dto.getType() == NoteType.SONDAGE) {
            note.setMultipleAnswers(dto.isMultipleAnswers());
        } else {
            note.setMultipleAnswers(false);
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

        return toDTO(savedNote, dto.getCreatorId());
    }

    // Supprime une note si le demandeur en est le créateur
    @Transactional
    public void deleteNote(Long noteId, Long currentRoommateId) {

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note introuvable."));

        if (currentRoommateId == null
                || !note.getCreator().getId().equals(currentRoommateId)) {
            throw new IllegalArgumentException(
                    "Seul le créateur peut supprimer cette note.");
        }

        noteRepository.delete(note);
    }

    //Vérifie les données de création
    private void validateCreateNote(CreateNoteDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Les données de la note sont obligatoires.");
        }

        if (dto.getType() == null) {
            throw new IllegalArgumentException(
                    "Le type de note est obligatoire.");
        }

        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException(
                    "Le contenu de la note est obligatoire.");
        }

        if (dto.getCreatorId() == null || dto.getSharedHouseId() == null) {
            throw new IllegalArgumentException(
                    "Le créateur et la colocation sont obligatoires.");
        }

        if (dto.getType() == NoteType.SONDAGE) {
            if (dto.getOptions() == null || dto.getOptions().size() < 2) {
                throw new IllegalArgumentException(
                        "Un sondage doit avoir au moins deux options.");
            }

            boolean invalidOption = dto.getOptions().stream()
                    .anyMatch(option ->
                            option == null || option.isBlank());

            if (invalidOption) {
                throw new IllegalArgumentException(
                        "Les options ne peuvent pas être vides.");
            }

            if (dto.getOptions().stream()
                    .map(String::trim)
                    .distinct()
                    .count() != dto.getOptions().size()) {
                throw new IllegalArgumentException(
                        "Les options d'un sondage doivent être différentes.");
            }
        }
    }

    // Convertit une entité Note en DTO pour Angular
    private NoteDTO toDTO(Note note, Long currentRoommateId) {

        NoteDTO dto = new NoteDTO();
        dto.setId(note.getId());
        dto.setType(note.getType());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());

        Roommate creator = note.getCreator();

        dto.setCreatorId(creator.getId());
        dto.setCreatorName(creator.getName() + " " + creator.getSurname());
        dto.setCreatorInitials(
                getInitial(creator.getName()) + getInitial(creator.getSurname()));

        dto.setSharedHouseId(note.getSharedHouse().getId());
        dto.setMultipleAnswers(note.isMultipleAnswers());

        dto.setCanDelete(
                currentRoommateId != null
                && creator.getId().equals(currentRoommateId));

        List<PollOptionDTO> optionDTOs = new ArrayList<>();

        if (note.getType() == NoteType.SONDAGE) {
            List<PollOption> options =
                    pollOptionRepository.findByNote_IdOrderByIdAsc(note.getId());

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