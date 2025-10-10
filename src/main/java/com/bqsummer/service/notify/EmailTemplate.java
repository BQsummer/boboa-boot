package com.bqsummer.service.notify;

import com.bqsummer.constant.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailTemplate extends Template {

    private String title;
    private String emailBody;

    @Override
    ChannelType getType() {
        return ChannelType.EMAIL;
    }
}
