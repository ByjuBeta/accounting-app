package com.accountingapp.banking;

import com.accountingapp.banking.BankImportService.FileFormat;
import com.accountingapp.banking.BankImportService.ImportResult;
import com.accountingapp.banking.dto.BankTransactionDto;
import com.accountingapp.journal.JournalEntryMapper;
import com.accountingapp.journal.dto.JournalEntryLineDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/bank-transactions")
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankImportService bankImportService;
    private final BankMatchingService bankMatchingService;
    private final BankTransactionRepository bankTransactionRepository;
    private final BankTransactionMapper bankTransactionMapper;
    private final JournalEntryMapper journalEntryMapper;

    @PostMapping("/import")
    public ImportResult importFeed(
            @RequestParam UUID accountId, @RequestParam FileFormat format, @RequestParam MultipartFile file)
            throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return bankImportService.importFeed(accountId, format, content);
    }

    @GetMapping
    public List<BankTransactionDto> list(
            @RequestParam UUID accountId, @RequestParam(required = false) BankTransactionStatus status) {
        List<BankTransaction> transactions = status != null
                ? bankTransactionRepository.findByAccountIdAndStatusOrderByTransactionDateAsc(accountId, status)
                : bankTransactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);
        return transactions.stream().map(bankTransactionMapper::toDto).toList();
    }

    @GetMapping("/{id}/suggested-matches")
    public List<JournalEntryLineDto> suggestedMatches(@PathVariable UUID id) {
        return bankMatchingService.suggestMatches(id).stream().map(journalEntryMapper::toDto).toList();
    }

    @PostMapping("/auto-match")
    public Map<String, Integer> autoMatch(@RequestParam UUID accountId) {
        return Map.of("matched", bankMatchingService.autoMatch(accountId));
    }

    @PostMapping("/{id}/match")
    public BankTransactionDto match(@PathVariable UUID id, @RequestBody Map<String, UUID> body) {
        return bankTransactionMapper.toDto(bankMatchingService.confirmMatch(id, body.get("journalEntryLineId")));
    }

    @PostMapping("/{id}/unmatch")
    public BankTransactionDto unmatch(@PathVariable UUID id) {
        return bankTransactionMapper.toDto(bankMatchingService.unmatch(id));
    }

    @PostMapping("/{id}/ignore")
    public BankTransactionDto ignore(@PathVariable UUID id) {
        return bankTransactionMapper.toDto(bankMatchingService.ignore(id));
    }
}
