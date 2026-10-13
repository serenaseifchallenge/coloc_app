package com.coloc.back.dto;

import com.coloc.back.entity.Roommate;

public record RoommateSummaryResponse(Long id, String name, String surname, String avatarColor, String avatarImage) {

    /** Couleur grise de l'avatar d'un compte supprimé. */
    private static final String DELETED_COLOR = "#9e9e9e";

    /** Pour l'historique : un compte supprimé (null) s'affiche « Ex-coloc ». */
    public static RoommateSummaryResponse fromNullable(Roommate roommate) {
        return roommate == null
                ? new RoommateSummaryResponse(null, "Ex-coloc", "supprimé", DELETED_COLOR, null)
                : from(roommate);
    }

    public static RoommateSummaryResponse from(Roommate roommate) {
        return new RoommateSummaryResponse(roommate.getId(), roommate.getName(), roommate.getSurname(),
                roommate.getAvatarColor(), roommate.getAvatarImage());
    }
}