package com.ami.notes.services;

import com.ami.notes.exceptions.ResourceNotFoundException;
import com.ami.notes.model.Note;
import com.ami.notes.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteServiceImpl implements NoteService{

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public Note createNoteForUser(String username, String content) {
        Note note=new Note();
        note.setContent(content);
        note.setOwnerUsername(username);
        Note savedNote=noteRepository.save(note);
        auditLogService.logNoteCreation(username,note);
        return savedNote;
    }

    @Override
    public Note updateNoteForUser(Long noteId, String content, String username) {
        Note userNote=noteRepository.findById(noteId)
                .orElseThrow(()->new ResourceNotFoundException("Note","noteId",noteId));
        userNote.setContent(content);
        Note updatedNote=noteRepository.save(userNote);
        auditLogService.logNoteUpdate(username,userNote);
        return updatedNote;
    }

    @Override
    public void deleteNoteForUser(Long noteId, String username) {
        Note note=noteRepository.findById(noteId)
                        .orElseThrow(()->new RuntimeException("Note not found!"));
        auditLogService.logNoteDeletion(username,noteId);
        noteRepository.delete(note);
    }

    @Override
    public List<Note> getNotesForUser(String username) {
        List<Note> noteList=noteRepository.findByOwnerUsername(username);
        return noteList;
    }
}
