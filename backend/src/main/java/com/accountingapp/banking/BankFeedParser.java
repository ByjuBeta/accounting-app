package com.accountingapp.banking;

import com.accountingapp.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses raw bank-feed text (CSV or OFX/QFX) into {@link ParsedTransaction}
 * rows. No persistence or dedupe here — that's {@link BankImportService}'s job.
 */
@Component
public class BankFeedParser {

    public record ParsedTransaction(LocalDate transactionDate, BigDecimal amount, String description,
                                     String checkNumber, String externalId) {
    }

    private static final List<DateTimeFormatter> CSV_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    private static final Pattern STMTTRN_PATTERN =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** Expects a header row, then columns: Date, Description, Amount, [CheckNumber]. */
    public List<ParsedTransaction> parseCsv(String content) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> fields = splitCsvLine(line);
            if (fields.size() < 3) {
                continue;
            }
            LocalDate date = parseCsvDate(fields.get(0).trim());
            String description = fields.get(1).trim();
            BigDecimal amount = new BigDecimal(fields.get(2).trim().replace(",", ""));
            String checkNumber = fields.size() > 3 && !fields.get(3).isBlank() ? fields.get(3).trim() : null;
            transactions.add(new ParsedTransaction(date, amount, description, checkNumber, hash(date, amount, description)));
        }
        return transactions;
    }

    public List<ParsedTransaction> parseOfx(String content) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        Matcher blockMatcher = STMTTRN_PATTERN.matcher(content);
        while (blockMatcher.find()) {
            String block = blockMatcher.group(1);
            BigDecimal amount = new BigDecimal(extractTag(block, "TRNAMT"));
            LocalDate date = parseOfxDate(extractTag(block, "DTPOSTED"));
            String description = extractTagOptional(block, "NAME");
            if (description == null) {
                description = extractTagOptional(block, "MEMO");
            }
            String fitId = extractTagOptional(block, "FITID");
            String checkNumber = extractTagOptional(block, "CHECKNUM");
            String externalId = fitId != null ? fitId : hash(date, amount, description != null ? description : "");
            transactions.add(new ParsedTransaction(date, amount, description != null ? description : "", checkNumber, externalId));
        }
        return transactions;
    }

    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private LocalDate parseCsvDate(String value) {
        for (DateTimeFormatter formatter : CSV_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new BusinessRuleException("INVALID_DATE_FORMAT", "Could not parse date '" + value + "'");
    }

    private LocalDate parseOfxDate(String value) {
        // OFX dates are YYYYMMDD, optionally followed by HHMMSS and a timezone offset.
        return LocalDate.parse(value.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String extractTag(String block, String tag) {
        String value = extractTagOptional(block, tag);
        if (value == null) {
            throw new BusinessRuleException("INVALID_OFX_FORMAT", "Missing <" + tag + "> in STMTTRN block");
        }
        return value;
    }

    private String extractTagOptional(String block, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + ">([^<\\r\\n]*)").matcher(block);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String hash(LocalDate date, BigDecimal amount, String description) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((date + "|" + amount.toPlainString() + "|" + description)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
