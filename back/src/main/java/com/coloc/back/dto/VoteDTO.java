package com.coloc.back.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VoteDTO {

    private List<Long> optionIds = new ArrayList<>();
}