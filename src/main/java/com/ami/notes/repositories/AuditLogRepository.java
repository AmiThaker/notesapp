package com.ami.notes.repositories;

import com.ami.notes.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {

    List<AuditLog> findByNoteId(Long noteId);
}
