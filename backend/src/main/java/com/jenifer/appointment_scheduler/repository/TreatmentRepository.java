package com.jenifer.appointment_scheduler.repository;

import com.jenifer.appointment_scheduler.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    boolean existsByName(String name);
}
