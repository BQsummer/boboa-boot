package com.bqsummer.common.vo.resp.character;

import com.bqsummer.common.dto.character.ScheduleRule;
import com.bqsummer.common.dto.character.ScheduleRulePattern;
import com.bqsummer.common.dto.character.ScheduleSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRuleDetailResp {
    private ScheduleRule rule;
    private List<ScheduleRulePattern> patterns;
    private List<ScheduleSlot> slots;
}
