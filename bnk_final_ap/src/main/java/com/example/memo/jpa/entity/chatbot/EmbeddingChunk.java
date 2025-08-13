package com.example.memo.jpa.entity.chatbot;

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
@Table(name = "EMBEDDING_CHUNK")
public class EmbeddingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHUNK_ID", nullable = false)
    private Long chunkId;

    @Column(name = "CHUNK_SEQ", nullable = false)
    private Long chunkSeq;
    
    @Lob
    @Column(name = "CHUNK_TEXT", nullable = false)
    private String chunkText;

    @Column(name = "VECTOR_ID", nullable = false)
    private String vectorId;
    
    @Column(name = "FILE_NAME", nullable = false) // ⭐ 파일명 컬럼 추가
    private String fileName;

    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
