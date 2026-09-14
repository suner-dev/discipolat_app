package com.discipolat.modules.dresscode.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DressCodeRuleRepository extends JpaRepository<DressCodeRule, UUID> {

    List<DressCodeRule> findByDressCodeId(UUID dressCodeId);

    void deleteByDressCodeId(UUID dressCodeId);
}
