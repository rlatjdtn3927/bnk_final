package com.example.memo.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CHAT_LOG")
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ID", nullable = false)
    private Long chatId;

    @Lob
    @Column(name = "QUESTION_EMBED")
    private String questionEmbed;

    @Lob
    @Column(name = "QUESTION_TEXT", nullable = false)
    private String questionText;

    @Lob
    @Column(name = "ANSWER", nullable = false)
    private String answer;

    @Lob
    @Column(name = "CONTEXT")
    private String context;

    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
