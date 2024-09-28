package com.ami.notes.services;

import com.ami.notes.model.AuditLog;
import com.ami.notes.model.Note;

import java.util.List;

public interface AuditLogService {
    void logNoteCreation(String username, Note note);

    void logNoteUpdate(String username, Note note);

    void logNoteDeletion(String username, Long noteId);

    List<AuditLog> getAllAuditLogs();

    List<AuditLog> getAuditLogsByNoteId(Long id);
}
