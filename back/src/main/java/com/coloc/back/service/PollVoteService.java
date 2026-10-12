package com.coloc.back.service;

import com.coloc.back.dto.NoteDTO;
import com.coloc.back.dto.VoteDTO;
import com.coloc.back.entity.Note;
import com.coloc.back.entity.NoteType;
import com.coloc.back.entity.PollOption;
import com.coloc.back.entity.PollVote;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PollVoteService {

    private final NoteRepository noteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final RoommateRepository roommateRepository;
    private final NoteService noteService;

    public PollVoteService(
            NoteRepository noteRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            RoommateRepository roommateRepository,
            NoteService noteService) {

        this.noteRepository = noteRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.roommateRepository = roommateRepository;
        this.noteService = noteService;
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

    @Transactional
    public NoteDTO vote(Long noteId, VoteDTO voteDTO) {
        if (noteId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'identifiant du sondage est obligatoire.");
        }

        if (voteDTO == null || voteDTO.getOptionIds() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La liste des options est obligatoire.");
        }

        Roommate roommate = getCurrentRoommate();
        SharedHouse house = roommate.getSharedHouse();

        if (house == null || house.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'appartenez à aucune colocation.");
        }

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sondage introuvable."));

        if (note.getSharedHouse() == null
                || !house.getId().equals(note.getSharedHouse().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sondage introuvable.");
        }

        if (note.getType() != NoteType.SONDAGE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Il est impossible de voter sur une annonce.");
        }

        // Vérifier la clôture avant toute modification des votes.
        LocalDateTime pollEndsAt = note.getPollEndsAt();

        if (pollEndsAt == null || !pollEndsAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce sondage est terminé. Les votes ne sont plus acceptés.");
        }

        List<Long> optionIds = voteDTO.getOptionIds();
        Set<Long> uniqueOptionIds = new HashSet<>(optionIds);

        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une option ne peut pas être sélectionnée plusieurs fois.");
        }

        if (!note.isMultipleAnswers() && uniqueOptionIds.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ce sondage autorise une seule réponse.");
        }

        List<PollOption> options =
                pollOptionRepository.findAllById(uniqueOptionIds);

        if (options.size() != uniqueOptionIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une ou plusieurs options sont introuvables.");
        }

        boolean invalidOption = options.stream().anyMatch(option ->
                option.getNote() == null
                        || !noteId.equals(option.getNote().getId()));

        if (invalidOption) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une option n'appartient pas à ce sondage.");
        }

        // Remplacer les votes existants de l'utilisateur authentifié.
        pollVoteRepository.deleteByRoommate_IdAndOption_Note_Id(
                roommate.getId(), note.getId());

        pollVoteRepository.flush();

        for (PollOption option : options) {
            PollVote newVote = new PollVote();
            newVote.setOption(option);
            newVote.setRoommate(roommate);
            pollVoteRepository.save(newVote);
        }

        pollVoteRepository.flush();

        // Retourner les compteurs et les sélections actualisés.
        return noteService.getNoteById(noteId);
    }
}