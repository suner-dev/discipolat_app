package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByActifTrueOrderByTitreAsc();
    List<Course> findByCategorieOrderByTitreAsc(String categorie);
}
