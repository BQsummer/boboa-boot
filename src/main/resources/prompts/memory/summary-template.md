# 对话总结任务

你是一个专业的对话分析师，需要为一段多轮对话生成结构化总结，以便后续对话使用更少的token回顾历史上下文。

## 上次总结（如果存在）
<% if (previousSummary != null) { %>
**话题**: 
<% for (topic : previousSummary.topics) { %>
- ${topic.name}: ${topic.summary}
<% } %>

**关键要点**: 
<% for (point : previousSummary.keyPoints) { %>
- ${point}
<% } %>

**用户情绪**: ${previousSummary.userEmotion}

**上下文延续**: ${previousSummary.contextCarryOver}
<% } else { %>
（这是第一次总结）
<% } %>

## 新消息记录
<% for (msg : messages) { %>
**[${msg.senderType}] ${msg.createdAt}**
${msg.content}

<% } %>

## 总结要求

请根据上述对话，生成一个结构化的JSON总结，包含以下字段：

1. **topics**: 对话涉及的主要话题列表，每个话题包含：
   - name: 话题名称（简短，3-5个字）
   - summary: 话题的简要总结（1-2句话）

2. **keyPoints**: 关键要点列表（字符串数组），提取对话中的重要信息点

3. **userEmotion**: 用户在对话中的整体情绪状态（如：积极、沮丧、中立、焦虑等）

4. **contextCarryOver**: 需要延续到下次对话的上下文信息（如：未解决的问题、待完成的任务等）

## 输出格式

请严格按照以下JSON格式输出（不要包含markdown代码块标记）：

```json
{
  "topics": [
    {
      "name": "话题名称",
      "summary": "话题总结内容"
    }
  ],
  "keyPoints": [
    "关键要点1",
    "关键要点2"
  ],
  "userEmotion": "情绪描述",
  "contextCarryOver": "需要延续的上下文"
}
```

## 注意事项

- 总结要简洁但信息完整
- 关键要点应该是可操作的或有意义的信息
- 情绪识别要准确但不过度解读
- 如果没有需要延续的上下文，contextCarryOver可以为空字符串
- 输出必须是有效的JSON格式
