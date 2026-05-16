package com.khalid.freyr.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {

    @Query("""
            select count(assignment)
            from TaskAssignment assignment
            join AgentProposal proposal on proposal.id = assignment.proposalId
            where assignment.agronomistId = :agronomistId
              and proposal.scheduleDate = :scheduleDate
              and assignment.status = com.khalid.freyr.assignment.TaskAssignmentStatus.ACTIVE
            """)
    long countActiveAssignmentsForAgronomistOnScheduleDate(
            @Param("agronomistId") UUID agronomistId,
            @Param("scheduleDate") LocalDate scheduleDate
    );
}
