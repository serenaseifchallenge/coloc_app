package com.coloc.back.service;

import com.coloc.back.entity.HiddenTaskName;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.HiddenTaskNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

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
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<String> getTaskNames() {
        Long sharedHouseId = currentUserService.getCurrentSharedHouse().getId();
        Set<String> hiddenNames = hiddenTaskNameRepository.findNamesBySharedHouseId(sharedHouseId);

        return DEFAULT_TASK_NAMES.stream()
                .filter(name -> !hiddenNames.contains(name))
                .toList();
    }

    @Transactional
    public void deleteTaskName(String name) {
        if (!DEFAULT_TASK_NAMES.contains(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce nom ne fait pas partie des suggestions");
        }

        SharedHouse sharedHouse = currentUserService.getCurrentSharedHouse();
        if (hiddenTaskNameRepository.existsBySharedHouseIdAndName(sharedHouse.getId(), name)) {
            return;
        }

        HiddenTaskName hiddenTaskName = new HiddenTaskName();
        hiddenTaskName.setSharedHouse(sharedHouse);
        hiddenTaskName.setName(name);
        hiddenTaskNameRepository.save(hiddenTaskName);
    }
}