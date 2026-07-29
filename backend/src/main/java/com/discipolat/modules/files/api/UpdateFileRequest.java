package com.discipolat.modules.files.api;

public record UpdateFileRequest(
        String nom,
        String description,
        String categorie
) {}
