package com.coloc.back.dto;

import com.coloc.back.entity.NoteType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateNoteDTO {

    private NoteType type;
    private String content;
    private boolean multipleAnswers;
    private List<String> options = new ArrayList<>();
    private LocalDateTime pollEndsAt;
}