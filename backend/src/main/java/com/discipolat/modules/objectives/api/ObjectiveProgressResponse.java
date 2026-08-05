package com.discipolat.modules.objectives.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.objectives.domain.Objective;
import com.discipolat.modules.objectives.domain.ObjectiveType;

import java.util.UUID;

public record ObjectiveProgressResponse(
        UUID id,
        UserRole role,
        ObjectiveType type,
        int cible,
        Objective.Periode periode,
        double realise,
        double taux,       // 0-100
        boolean atteint
) {
    public static ObjectiveProgressResponse of(Objective o, double realise) {
        double taux = o.getCible() > 0 ? Math.min(100.0, (realise / o.getCible()) * 100.0) : 0.0;
        return new ObjectiveProgressResponse(
                o.getId(), o.getRole(), o.getType(), o.getCible(), o.getPeriode(),
                realise, Math.round(taux * 10.0) / 10.0, realise >= o.getCible());
    }
}
