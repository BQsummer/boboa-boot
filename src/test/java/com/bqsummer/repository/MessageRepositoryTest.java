package com.bqsummer.repository;

import com.bqsummer.common.dto.im.Message;
import com.bqsummer.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageRepositoryTest {

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageRepository messageRepository;

    @Test
    void saveShouldTruncateContentWhenTooLong() {
        Message message = new Message();
        message.setSenderId(1001L);
        message.setReceiverId(2001L);
        message.setType("text");
        message.setStatus("sent");
        message.setIsDeleted(false);
        message.setContent("a".repeat(2050));

        messageRepository.save(message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        Message savedMessage = captor.getValue();
        assertNotNull(savedMessage);
        assertEquals(2048, savedMessage.getContent().codePointCount(0, savedMessage.getContent().length()));
        assertEquals(2048, message.getContent().codePointCount(0, message.getContent().length()));
    }

    @Test
    void saveShouldKeepContentWhenWithinLimit() {
        Message message = new Message();
        message.setSenderId(1001L);
        message.setReceiverId(2001L);
        message.setType("text");
        message.setStatus("sent");
        message.setIsDeleted(false);
        message.setContent("a".repeat(2048));

        messageRepository.save(message);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        Message savedMessage = captor.getValue();
        assertNotNull(savedMessage);
        assertEquals(2048, savedMessage.getContent().codePointCount(0, savedMessage.getContent().length()));
    }
}
