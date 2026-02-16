package com.bqsummer.controller;

import com.bqsummer.common.dto.character.ScheduleRule;
import com.bqsummer.common.dto.character.ScheduleRulePattern;
import com.bqsummer.common.dto.character.ScheduleSlot;
import com.bqsummer.common.dto.character.SpecialEvent;
import com.bqsummer.common.vo.req.character.CreateScheduleRuleReq;
import com.bqsummer.common.vo.req.character.CreateSpecialEventReq;
import com.bqsummer.common.vo.resp.character.CharacterScheduleStateResp;
import com.bqsummer.common.vo.resp.character.ScheduleRuleDetailResp;
import com.bqsummer.service.CharacterScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ai/characters")
@RequiredArgsConstructor
public class CharacterScheduleController {

    private final CharacterScheduleService characterScheduleService;

    @PostMapping("/schedules/rules")
    public ResponseEntity<?> createRule(@RequestBody CreateScheduleRuleReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).body("未授权");
        }
        if (req.getCharacterId() == null && (req.getCharacterKey() == null || req.getCharacterKey().isBlank())) {
            return ResponseEntity.badRequest().body("characterId 或 characterKey 必填");
        }

        ScheduleRule rule = characterScheduleService.createRule(req);
        return ResponseEntity.ok(rule);
    }

    @PostMapping("/schedules/events")
    public ResponseEntity<?> createEvent(@RequestBody CreateSpecialEventReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).body("未授权");
        }
        if (req.getCharacterId() == null && (req.getCharacterKey() == null || req.getCharacterKey().isBlank())) {
            return ResponseEntity.badRequest().body("characterId 或 characterKey 必填");
        }

        SpecialEvent event = characterScheduleService.createSpecialEvent(req);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/schedules/rules")
    public ResponseEntity<List<ScheduleRuleDetailResp>> listRules(@RequestParam(value = "characterId", required = false) Long characterId,
                                                                  @RequestParam(value = "characterKey", required = false) String characterKey) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (characterId == null && (characterKey == null || characterKey.isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        List<ScheduleRule> rules = characterScheduleService.listRules(characterId, characterKey);
        List<ScheduleRulePattern> patterns = characterScheduleService.listRulePatterns(characterId, characterKey);
        List<ScheduleSlot> slots = characterScheduleService.listRuleSlots(characterId, characterKey);
        Map<Long, List<ScheduleRulePattern>> patternMap = patterns.stream().collect(Collectors.groupingBy(ScheduleRulePattern::getRuleId));
        Map<Long, List<ScheduleSlot>> slotMap = slots.stream().collect(Collectors.groupingBy(ScheduleSlot::getRuleId));

        List<ScheduleRuleDetailResp> resp = rules.stream()
                .map(rule -> ScheduleRuleDetailResp.builder()
                        .rule(rule)
                        .patterns(patternMap.getOrDefault(rule.getId(), List.of()))
                        .slots(slotMap.getOrDefault(rule.getId(), List.of()))
                        .build())
                .toList();
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/schedules/rules/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        characterScheduleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/schedules/events")
    public ResponseEntity<List<SpecialEvent>> listEvents(@RequestParam(value = "characterId", required = false) Long characterId,
                                                         @RequestParam(value = "characterKey", required = false) String characterKey) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (characterId == null && (characterKey == null || characterKey.isBlank())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(characterScheduleService.listEvents(characterId, characterKey));
    }

    @DeleteMapping("/schedules/events/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        characterScheduleService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/state")
    public ResponseEntity<CharacterScheduleStateResp> state(@RequestParam(value = "characterId", required = false) Long characterId,
                                                            @RequestParam(value = "characterKey", required = false) String characterKey,
                                                            @RequestParam(value = "t", required = false) String t) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (characterId == null && (characterKey == null || characterKey.isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        OffsetDateTime at;
        try {
            at = t == null || t.isBlank() ? OffsetDateTime.now() : OffsetDateTime.parse(t);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(characterScheduleService.queryState(characterId, characterKey, at));
    }
}
