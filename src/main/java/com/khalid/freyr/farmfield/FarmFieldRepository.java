package com.khalid.freyr.farmfield;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FarmFieldRepository extends JpaRepository<FarmField, UUID> {
}
