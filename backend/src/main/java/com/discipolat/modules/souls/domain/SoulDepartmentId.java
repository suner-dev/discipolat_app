package com.discipolat.modules.souls.domain;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Clé composite pour SoulDepartment (soul_id + department_id).
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SoulDepartmentId implements Serializable {
    private UUID soulId;
    private UUID departmentId;
}
