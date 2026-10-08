package com.coloc.back.controller;

import com.coloc.back.dto.CreateTaskNameRequest;
import com.coloc.back.dto.TaskNameResponse;
import com.coloc.back.service.TaskNameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskNameResponse addTaskName(@Valid @RequestBody CreateTaskNameRequest request) {
        return taskNameService.addTaskName(request);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskName(@PathVariable String name) {
        taskNameService.deleteTaskName(name);
    }
}