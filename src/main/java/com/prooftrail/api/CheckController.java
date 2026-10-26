package com.prooftrail.api;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CheckController {

    public record CheckRequest(String approvedFacts, String guidelines, String content) {}
    public record Finding(int id, String statement, String status, String reason,
                          String evidence, String suggestion) {}
    public record CheckResponse(int sentenceCount, int findingCount, List<Finding> findings,
                                String note) {}

    private static final List<String> HIGH_RISK_PHRASES = List.of(
        "100% guaranteed", "guaranteed results", "number one", "no. 1", "best in the world",
        "clinically proven", "zero risk", "risk-free", "cures", "cure"
    );

    @PostMapping("/check")
    public CheckResponse check(@RequestBody CheckRequest request) {
        String factsText = safe(request.approvedFacts());
        String guidelinesText = safe(request.guidelines());
        String content = safe(request.content());

        List<String> facts = factsText.lines().map(String::trim)
            .filter(s -> !s.isBlank()).toList();
        List<String> prohibited = guidelinesText.lines().map(String::trim)
            .filter(s -> !s.isBlank()).toList();

        List<String> sentences = Arrays.stream(content.split("(?<=[.!?])\\s+|\\n+"))
            .map(String::trim).filter(s -> !s.isBlank()).toList();

        List<Finding> findings = new ArrayList<>();
        int id = 1;
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase(Locale.ROOT);
            String matchedProhibited = prohibited.stream()
                .filter(term -> lower.contains(term.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(null);

            if (matchedProhibited != null) {
                findings.add(new Finding(id++, sentence, "Needs Human Review",
                    "This draft contains a phrase listed in your brand rules.",
                    "Guideline: avoid phrase \"" + matchedProhibited + "\".",
                    "Remove the flagged phrase or replace it with wording approved by your team."));
                continue;
            }

            String risky = HIGH_RISK_PHRASES.stream()
                .filter(term -> lower.contains(term))
                .findFirst().orElse(null);
            if (risky != null) {
                findings.add(new Finding(id++, sentence, "Unsupported",
                    "This absolute or high-impact claim needs explicit supporting evidence.",
                    "No matching approval was supplied for the phrase \"" + risky + "\".",
                    "Add verifiable evidence to the approved facts or rewrite the claim more narrowly."));
                continue;
            }

            String matchedFact = facts.stream()
                .filter(f -> hasMeaningfulOverlap(sentence, f))
                .max(Comparator.comparingInt(f -> overlapCount(sentence, f)))
                .orElse(null);

            if (matchedFact != null) {
                findings.add(new Finding(id++, sentence, "Supported",
                    "This statement shares key terms with an approved fact. Human verification is still recommended.",
                    "Approved fact: " + matchedFact,
                    "Keep the statement if it accurately represents the approved fact."));
            } else if (containsClaimSignal(lower)) {
                findings.add(new Finding(id++, sentence, "Unsupported",
                    "No sufficiently similar approved fact was found in the information provided.",
                    facts.isEmpty() ? "No approved facts were entered." : "Checked against " + facts.size() + " approved fact(s).",
                    "Verify the claim with a reliable source and add the approved wording, or remove it."));
            } else {
                findings.add(new Finding(id++, sentence, "Needs Human Review",
                    "This sentence could not be confidently matched to an approved fact or rule.",
                    facts.isEmpty() ? "No approved facts were entered." : "No clear matching fact was found.",
                    "Review the wording and add a relevant approved fact or guideline if needed."));
            }
        }

        int flagged = (int) findings.stream().filter(f -> !f.status().equals("Supported")).count();
        return new CheckResponse(sentences.size(), flagged, findings,
            "Rule-based prototype: results are review signals, not proof of truth. A human must approve all content.");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "app", "PROOFTRAIL");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean containsClaimSignal(String text) {
        return Pattern.compile("\\b(is|are|was|were|has|have|offers|provides|contains|includes|made with|up to|more than|less than|fastest|leading|first|only|free|organic|certified|sustainable|secure|safe|improves|reduces|increases|saves|guarantees|delivers)\\b")
            .matcher(text).find() || text.matches(".*\\d+\\s*(%|percent|mg|kg|hours?|days?|years?|x).*");
    }

    private static boolean hasMeaningfulOverlap(String a, String b) {
        return overlapCount(a, b) >= 2;
    }

    private static int overlapCount(String a, String b) {
        Set<String> aw = words(a);
        Set<String> bw = words(b);
        aw.retainAll(bw);
        return aw.size();
    }

    private static Set<String> words(String text) {
        Set<String> stop = Set.of("the","and","for","with","this","that","are","was","were","has","have","from","our","your","you","is","to","of","in","a","an","on","it","by");
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9% ]", " ")
            .split("\\s+")).filter(w -> w.length() > 2 && !stop.contains(w))
            .collect(Collectors.toSet());
    }
}
