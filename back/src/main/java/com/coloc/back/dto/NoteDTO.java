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
public class NoteDTO {

    private Long id;
    private NoteType type;
    private String content;
    private LocalDateTime createdAt;

    private Long creatorId;
    private String creatorName;
    private String creatorInitials;

    private Long sharedHouseId;
    private boolean multipleAnswers;

    private List<PollOptionDTO> options = new ArrayList<>();

    private boolean canDelete;
}