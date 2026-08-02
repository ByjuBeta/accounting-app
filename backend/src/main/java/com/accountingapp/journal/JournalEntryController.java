package com.accountingapp.journal;

import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryDto;
import com.accountingapp.journal.dto.UpdateJournalEntryRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;
    private final JournalEntryMapper journalEntryMapper;

    @PostMapping
    public ResponseEntity<JournalEntryDto> create(@Valid @RequestBody CreateJournalEntryRequest request) {
        JournalEntry entry = journalEntryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryMapper.toDto(entry));
    }

    @GetMapping("/{id}")
    public JournalEntryDto get(@PathVariable UUID id) {
        return journalEntryMapper.toDto(journalEntryService.getOrThrow(id));
    }

    @GetMapping
    public Page<JournalEntryDto> list(Pageable pageable) {
        return journalEntryService.list(pageable).map(journalEntryMapper::toDto);
    }

    @PutMapping("/{id}")
    public JournalEntryDto update(@PathVariable UUID id, @Valid @RequestBody UpdateJournalEntryRequest request) {
        return journalEntryMapper.toDto(journalEntryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        journalEntryService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/post")
    public JournalEntryDto post(@PathVariable UUID id) {
        return journalEntryMapper.toDto(journalEntryService.post(id));
    }

    @PostMapping("/{id}/void")
    public JournalEntryDto voidEntry(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return journalEntryMapper.toDto(journalEntryService.voidEntry(id, body.get("reason")));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalEntryDto> reverse(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        LocalDate reversalDate = body != null && body.get("reversalDate") != null
                ? LocalDate.parse(body.get("reversalDate")) : null;
        JournalEntry reversal = journalEntryService.reverse(id, reversalDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryMapper.toDto(reversal));
    }
}
