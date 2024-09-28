package com.ami.notes.controllers;

import com.ami.notes.model.Note;
import com.ami.notes.services.NoteService;
import com.sun.security.auth.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NoteController {

    @Autowired
    private NoteService noteService;
    
    private static final Logger logger= LoggerFactory.getLogger(NoteController.class);

    @PostMapping("/note")
    public Note createNote(@RequestBody String content,
                           @AuthenticationPrincipal UserDetails userDetails){
        String username=userDetails.getUsername();
        System.out.println("User Details : "+username);
        return noteService.createNoteForUser(username,content);
    }

    @GetMapping("/notes")
    public List<Note> getUserNotes(@AuthenticationPrincipal UserDetails userDetails){
        logger.debug("getUserNotes - NoteController");
        String username=userDetails.getUsername();
        System.out.println("User Details : "+username);
        return noteService.getNotesForUser(username);
    }

    @PutMapping("/notes/{noteId}")
    public Note updateNote(@PathVariable Long noteId,
                           @RequestBody String content,
                           @AuthenticationPrincipal UserDetails userDetails){
        String username=userDetails.getUsername();
        return noteService.updateNoteForUser(noteId,content,username);
    }

    @DeleteMapping("/notes/{noteId}")
    public void deleteNote(@PathVariable Long noteId,
                           @AuthenticationPrincipal UserDetails userDetails){
        String username=userDetails.getUsername();
        noteService.deleteNoteForUser(noteId,username);
    }
}
