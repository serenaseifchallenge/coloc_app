package com.coloc.back.service;

import com.coloc.back.dto.VoteDTO;
import com.coloc.back.entity.Note;
import com.coloc.back.entity.NoteType;
import com.coloc.back.entity.PollOption;
import com.coloc.back.entity.PollVote;
import com.coloc.back.entity.Roommate;
import com.coloc.back.repository.NoteRepository;
import com.coloc.back.repository.PollOptionRepository;
import com.coloc.back.repository.PollVoteRepository;
import com.coloc.back.repository.RoommateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PollVoteService {

    private final NoteRepository noteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final RoommateRepository roommateRepository;

    public PollVoteService(
            NoteRepository noteRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            RoommateRepository roommateRepository) {

        this.noteRepository = noteRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.roommateRepository = roommateRepository;
    }

    @Transactional
    public void vote(Long noteId, VoteDTO voteDTO) {

        if (voteDTO == null || voteDTO.getRoommateId() == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du colocataire est obligatoire.");
        }

        if (voteDTO.getOptionIds() == null) {
            throw new IllegalArgumentException(
                    "La liste des options est obligatoire.");
        }

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note introuvable."));

        if (note.getType() != NoteType.SONDAGE) {
            throw new IllegalArgumentException(
                    "Il est impossible de voter sur une annonce.");
        }

        Roommate roommate = roommateRepository
                .findById(voteDTO.getRoommateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Colocataire introuvable."));

        if (roommate.getSharedHouse() == null
                || !roommate.getSharedHouse().getId()
                        .equals(note.getSharedHouse().getId())) {
            throw new IllegalArgumentException(
                    "Ce colocataire n'appartient pas à cette colocation.");
        }

        List<Long> optionIds = voteDTO.getOptionIds();

        // Évite de recevoir plusieurs fois le même identifiant.
        Set<Long> uniqueOptionIds = new HashSet<>(optionIds);

        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new IllegalArgumentException(
                    "Une option ne peut pas être sélectionnée plusieurs fois.");
        }

        // Un sondage à choix unique ne peut recevoir qu'une option.
        if (!note.isMultipleAnswers() && optionIds.size() > 1) {
            throw new IllegalArgumentException(
                    "Ce sondage autorise une seule réponse.");
        }

        List<PollOption> options =
                pollOptionRepository.findAllById(uniqueOptionIds);

        if (options.size() != uniqueOptionIds.size()) {
            throw new IllegalArgumentException(
                    "Une ou plusieurs options sont introuvables.");
        }

        // Toutes les options doivent appartenir au sondage concerné
        boolean invalidOption = options.stream()
                .anyMatch(option ->
                        !option.getNote().getId().equals(noteId));

        if (invalidOption) {
            throw new IllegalArgumentException(
                    "Une option n'appartient pas à ce sondage.");
        }

        // Supprime uniquement les anciens votes de ce colocataire pour ce sondage sans toucher à ses autres votes
        List<PollVote> previousVotes =
                pollVoteRepository.findByRoommate_Id(roommate.getId());

        List<PollVote> votesToDelete = previousVotes.stream()
                .filter(vote ->
                        vote.getOption().getNote().getId().equals(noteId))
                .toList();

        pollVoteRepository.deleteAll(votesToDelete);

        // Une liste vide permet de retirer son vote.
        for (PollOption option : options) {
            PollVote newVote = new PollVote();
            newVote.setOption(option);
            newVote.setRoommate(roommate);

            pollVoteRepository.save(newVote);
        }
    }
}