package com.coloc.back.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
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

    public List<String> getTaskNames() {
        return DEFAULT_TASK_NAMES;
    }
}