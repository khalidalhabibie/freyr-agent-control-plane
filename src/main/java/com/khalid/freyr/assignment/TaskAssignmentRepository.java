package com.khalid.freyr.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
}
