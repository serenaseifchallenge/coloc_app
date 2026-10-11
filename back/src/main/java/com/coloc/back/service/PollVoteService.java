
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

        // 1. Vérifier la requête.
        if (noteId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du sondage est obligatoire.");
        }

        if (voteDTO == null || voteDTO.getRoommateId() == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du colocataire est obligatoire.");
        }

        if (voteDTO.getOptionIds() == null) {
            throw new IllegalArgumentException(
                    "La liste des options est obligatoire.");
        }

        //Charger le sondage
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note introuvable."));

        if (note.getType() != NoteType.SONDAGE) {
            throw new IllegalArgumentException(
                    "Il est impossible de voter sur une annonce.");
        }

        //Vérifier le colocataire
        Roommate roommate = roommateRepository
                .findById(voteDTO.getRoommateId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Colocataire introuvable."));

        if (note.getSharedHouse() == null
                || roommate.getSharedHouse() == null
                || !note.getSharedHouse().getId()
                        .equals(roommate.getSharedHouse().getId())) {
            throw new IllegalArgumentException(
                    "Ce colocataire n'appartient pas à cette colocation.");
        }

        // Éliminer les identifiants dupliqués et vérifier la sélection
        List<Long> optionIds = voteDTO.getOptionIds();
        Set<Long> uniqueOptionIds = new HashSet<>(optionIds);

        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new IllegalArgumentException(
                    "Une option ne peut pas être sélectionnée plusieurs fois.");
        }

        if (!note.isMultipleAnswers() && uniqueOptionIds.size() > 1) {
            throw new IllegalArgumentException(
                    "Ce sondage autorise une seule réponse.");
        }

        //Charger et valider toutes les options avant toute suppression.
        List<PollOption> options =
                pollOptionRepository.findAllById(uniqueOptionIds);

        if (options.size() != uniqueOptionIds.size()) {
            throw new IllegalArgumentException(
                    "Une ou plusieurs options sont introuvables.");
        }

        boolean invalidOption = options.stream()
                .anyMatch(option ->
                        option.getNote() == null
                                || !noteId.equals(option.getNote().getId()));

        if (invalidOption) {
            throw new IllegalArgumentException(
                    "Une option n'appartient pas à ce sondage.");
        }

        // Remplacer les votes existants pour le sondage actuel
        pollVoteRepository.deleteByRoommate_IdAndOption_Note_Id(
                roommate.getId(),
                note.getId()
        );

        // Exécuter les suppressions avant les insertions.
        pollVoteRepository.flush();

        //Enregistrer chaque option choisie.
        for (PollOption option : options) {
            PollVote newVote = new PollVote();
            newVote.setOption(option);
            newVote.setRoommate(roommate);

            pollVoteRepository.save(newVote);
        }

        // Forcer l'exécution des insertions dans la transaction
        pollVoteRepository.flush();
    }
}