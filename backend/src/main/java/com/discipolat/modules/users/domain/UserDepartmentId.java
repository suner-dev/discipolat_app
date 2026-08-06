package com.discipolat.modules.users.domain;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Clé composite pour UserDepartment (user_id + department_id).
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartmentId implements Serializable {
    private UUID userId;
    private UUID departmentId;
}
