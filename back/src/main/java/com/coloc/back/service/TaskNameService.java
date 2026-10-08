package com.coloc.back.service;

import com.coloc.back.dto.CreateTaskNameRequest;
import com.coloc.back.dto.TaskNameResponse;
import com.coloc.back.entity.CustomTaskName;
import com.coloc.back.entity.HiddenTaskName;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.CustomTaskNameRepository;
import com.coloc.back.repository.HiddenTaskNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TaskNameService {

    private static final List<String> DEFAULT_TASK_NAMES = List.of(
            "Faire la vaisselle",
            "Jeter la poubelle",
            "Passer le balai",
            "Passer l'aspirateur",
            "Nettoyer le sol",
            "Nettoyer les vitres",
            "Arroser les plantes",
            "Nettoyer les toilettes",
            "Nettoyer la douche",
            "Nettoyer le frigo",
            "Faire la poussière",
            "Faire la lessive",
            "Faire à manger"
    );

    private final HiddenTaskNameRepository hiddenTaskNameRepository;
    private final CustomTaskNameRepository customTaskNameRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<String> getTaskNames() {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();
        Set<String> hiddenNames = hiddenTaskNameRepository.findNamesBySharedHouseId(sharedHouseId);

        Stream<String> visibleDefaultNames = DEFAULT_TASK_NAMES.stream()
                .filter(name -> !hiddenNames.contains(name));
        Stream<String> customNames = customTaskNameRepository.findNamesBySharedHouseId(sharedHouseId).stream();

        return Stream.concat(visibleDefaultNames, customNames).toList();
    }

    @Transactional
    public TaskNameResponse addTaskName(CreateTaskNameRequest request) {
        SharedHouse sharedHouse = currentUserService.getCurrentSharedHouse();
        String name = request.name().trim();

        Optional<String> defaultName = findDefaultName(name);
        if (defaultName.isPresent()) {
            restoreDefaultName(sharedHouse.getId(), defaultName.get());
            return new TaskNameResponse(defaultName.get());
        }

        if (customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(sharedHouse.getId(), name).isPresent()) {
            throw alreadyExists();
        }

        CustomTaskName customTaskName = new CustomTaskName();
        customTaskName.setSharedHouse(sharedHouse);
        customTaskName.setName(name);
        customTaskNameRepository.save(customTaskName);

        return new TaskNameResponse(name);
    }

    @Transactional
    public void deleteTaskName(String name) {
        SharedHouse sharedHouse = currentUserService.getCurrentSharedHouse();

        Optional<CustomTaskName> customTaskName =
                customTaskNameRepository.findBySharedHouseIdAndNameIgnoreCase(sharedHouse.getId(), name);
        if (customTaskName.isPresent()) {
            customTaskNameRepository.delete(customTaskName.get());
            return;
        }

        if (!DEFAULT_TASK_NAMES.contains(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce nom ne fait pas partie des suggestions");
        }
        if (hiddenTaskNameRepository.existsBySharedHouseIdAndName(sharedHouse.getId(), name)) {
            return;
        }

        HiddenTaskName hiddenTaskName = new HiddenTaskName();
        hiddenTaskName.setSharedHouse(sharedHouse);
        hiddenTaskName.setName(name);
        hiddenTaskNameRepository.save(hiddenTaskName);
    }

    private void restoreDefaultName(Long sharedHouseId, String defaultName) {
        HiddenTaskName hiddenTaskName = hiddenTaskNameRepository.findBySharedHouseIdAndName(sharedHouseId, defaultName)
                .orElseThrow(this::alreadyExists);
        hiddenTaskNameRepository.delete(hiddenTaskName);
    }

    private Optional<String> findDefaultName(String name) {
        return DEFAULT_TASK_NAMES.stream()
                .filter(defaultName -> defaultName.equalsIgnoreCase(name))
                .findFirst();
    }

    private ResponseStatusException alreadyExists() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Cette tâche récurrente existe déjà");
    }
}