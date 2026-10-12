package com.coloc.back.controller;

import com.coloc.back.dto.NoteDTO;
import com.coloc.back.dto.VoteDTO;
import com.coloc.back.service.PollVoteService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class PollVoteController {

    private final PollVoteService pollVoteService;

    public PollVoteController(PollVoteService pollVoteService) {
        this.pollVoteService = pollVoteService;
    }

    @PutMapping("/{noteId}/vote")
    public ResponseEntity<NoteDTO> vote(
            @PathVariable Long noteId,
            @RequestBody VoteDTO voteDTO) {

        return ResponseEntity.ok(
                pollVoteService.vote(noteId, voteDTO));
    }
}