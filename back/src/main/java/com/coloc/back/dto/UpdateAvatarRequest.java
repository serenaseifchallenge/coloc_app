package com.coloc.back.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nouvel avatar. Les deux champs sont envoyés à chaque fois :
 *  - color : « #rrggbb », ou null pour revenir à la couleur par défaut ;
 *  - image : photo en data URL (déjà redimensionnée par le front), ou null pour revenir aux initiales.
 */
public record UpdateAvatarRequest(
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur invalide")
        String color,

        @Pattern(regexp = "^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$", message = "Image invalide")
        @Size(max = 400_000, message = "Image trop lourde")
        String image
) {}
