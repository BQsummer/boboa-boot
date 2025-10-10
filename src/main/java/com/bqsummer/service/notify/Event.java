package com.bqsummer.service.notify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private String name;
    private List<NotifyUser> users;
    private List<Template> templates;
}
