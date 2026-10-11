package com.coloc.back.controller;

import com.coloc.back.dto.CreateNoteDTO;
import com.coloc.back.dto.NoteDTO;
import com.coloc.back.service.NoteService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getNotes(
            @RequestParam Long sharedHouseId,
            @RequestParam(required = false) Long currentRoommateId) {

        return ResponseEntity.ok(
                noteService.getNotesBySharedHouse(
                        sharedHouseId, currentRoommateId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDTO> getNoteById(
            @PathVariable Long id,
            @RequestParam(required = false) Long currentRoommateId) {

        return ResponseEntity.ok(
                noteService.getNoteById(id, currentRoommateId));
    }

    @PostMapping
    public ResponseEntity<NoteDTO> createNote(
            @RequestBody CreateNoteDTO dto) {

        NoteDTO createdNote = noteService.createNote(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            @RequestParam Long currentRoommateId) {

        noteService.deleteNote(id, currentRoommateId);

        return ResponseEntity.noContent().build();
    }
}