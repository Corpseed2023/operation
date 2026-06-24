package com.doc.impl;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExpiryDateExtractor {

    private static final Pattern EXPIRY_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(" +
                    "expiry date|" +
                    "date of expiry|" +
                    "expires on|" +
                    "expires|" +
                    "valid upto|" +
                    "valid up to|" +
                    "valid till|" +
                    "valid until|" +
                    "valid through|" +
                    "validity|" +
                    "validity date|" +
                    "license valid till|" +
                    "certificate valid till" +
                    ")"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b" +                       // 31/03/2026
                    "|\\b\\d{1,2}[.]\\d{1,2}[.]\\d{2,4}\\b" +                // 31.03.2026
                    "|\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b" +                // 2026-03-31
                    "|\\b\\d{1,2}\\s*(st|nd|rd|th)?\\s+[A-Za-z]{3,9}\\s*,?\\s*\\d{4}\\b" + // 31 March 2026
                    "|\\b[A-Za-z]{3,9}\\s+\\d{1,2}\\s*,?\\s*\\d{4}\\b" +    // March 31 2026
                    "|\\b\\d{1,2}[/-]\\d{4}\\b"                              // 03/2026
    );

    public ExpiryDateMatch extractExpiryDate(String text) {

        if (text == null || text.isBlank()) {
            return new ExpiryDateMatch(null, null);
        }

        String normalizedText = normalizeText(text);

        /*
         * First priority:
         * Search date near expiry-related keywords.
         */
        Matcher keywordMatcher = EXPIRY_KEYWORD_PATTERN.matcher(normalizedText);

        while (keywordMatcher.find()) {

            int start = Math.max(0, keywordMatcher.start() - 30);
            int end = Math.min(normalizedText.length(), keywordMatcher.end() + 150);

            String nearbyText = normalizedText.substring(start, end);

            Matcher dateMatcher = DATE_PATTERN.matcher(nearbyText);

            while (dateMatcher.find()) {
                String dateText = dateMatcher.group();
                LocalDate date = parseDate(dateText);

                if (date != null) {
                    return new ExpiryDateMatch(date, nearbyText.trim());
                }
            }
        }

        /*
         * Fallback:
         * If no keyword found, pick the latest date from document.
         * But this is less reliable, so matchedText will show fallback.
         */
        List<LocalDate> allDates = extractAllDates(normalizedText);

        if (!allDates.isEmpty()) {
            LocalDate latestDate = allDates.stream()
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            return new ExpiryDateMatch(
                    latestDate,
                    "Fallback date selected. Please verify manually."
            );
        }

        return new ExpiryDateMatch(null, null);
    }

    private String normalizeText(String text) {
        return text
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<LocalDate> extractAllDates(String text) {

        Matcher matcher = DATE_PATTERN.matcher(text);

        return matcher.results()
                .map(matchResult -> parseDate(matchResult.group()))
                .filter(date -> date != null)
                .toList();
    }

    private LocalDate parseDate(String dateText) {

        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        String cleaned = dateText
                .replaceAll("(?i)(st|nd|rd|th)", "")
                .replace(",", "")
                .trim();

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("d.M.uuuu"),
                DateTimeFormatter.ofPattern("dd.MM.uuuu"),
                DateTimeFormatter.ofPattern("uuuu-M-d"),
                DateTimeFormatter.ofPattern("uuuu-MM-dd"),
                twoDigitYearFormatter("d/M/"),
                twoDigitYearFormatter("dd/MM/"),
                twoDigitYearFormatter("d-M-"),
                twoDigitYearFormatter("dd-MM-"),
                DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH)
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        /*
         * Handle month/year only.
         * Example: 03/2026
         * Business assumption: expiry is last day of that month.
         */
        LocalDate monthYearDate = parseMonthYear(cleaned);
        if (monthYearDate != null) {
            return monthYearDate;
        }

        return null;
    }

    private DateTimeFormatter twoDigitYearFormatter(String prefixPattern) {
        return new DateTimeFormatterBuilder()
                .appendPattern(prefixPattern)
                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                .toFormatter(Locale.ENGLISH);
    }

    private LocalDate parseMonthYear(String value) {

        List<DateTimeFormatter> monthYearFormatters = List.of(
                DateTimeFormatter.ofPattern("M/uuuu"),
                DateTimeFormatter.ofPattern("MM/uuuu"),
                DateTimeFormatter.ofPattern("M-uuuu"),
                DateTimeFormatter.ofPattern("MM-uuuu")
        );

        for (DateTimeFormatter formatter : monthYearFormatters) {
            try {
                YearMonth yearMonth = YearMonth.parse(value, formatter);
                return yearMonth.atEndOfMonth();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    public record ExpiryDateMatch(
            LocalDate expiryDate,
            String matchedText
    ) {
    }
}