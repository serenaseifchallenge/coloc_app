package com.coloc.back.controller;

import com.coloc.back.service.TaskNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/task-names")
@RequiredArgsConstructor
public class TaskNameController {

    private final TaskNameService taskNameService;

    @GetMapping
    public List<String> getTaskNames() {
        return taskNameService.getTaskNames();
    }
}