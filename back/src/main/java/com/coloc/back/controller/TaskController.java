package com.coloc.back.controller;

import com.coloc.back.dto.TaskResponse;
import com.coloc.back.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> getTasks(@RequestParam(defaultValue = "false") boolean done) {
        return taskService.getTasks(done);
    }
}