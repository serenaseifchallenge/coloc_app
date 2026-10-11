package com.coloc.back.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollOptionDTO {

    private Long id;
    private String label;
    private long voteCount;
    private boolean votedByCurrentUser;
}