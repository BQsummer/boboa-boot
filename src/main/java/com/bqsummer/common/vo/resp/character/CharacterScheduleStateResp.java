package com.bqsummer.common.vo.resp.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterScheduleStateResp {

    private String characterKey;
    private String timeLocal;
    private String locationText;
    private String activityText;
    private Source source;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String type;
        private Long id;
        private String title;
    }
}
