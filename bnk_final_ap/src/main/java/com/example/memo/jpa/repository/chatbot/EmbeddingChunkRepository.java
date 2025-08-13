package com.example.memo.jpa.repository.chatbot;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.chatbot.EmbeddingChunk;

@Repository
@Transactional("txManagerOracle")
public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunk, Long> {
	// ⭐ 파일명으로 존재하는지 확인하는 메서드 추가
    boolean existsByFileName(String fileName);
    @Query("SELECT e.vectorId FROM EmbeddingChunk e WHERE e.fileName = :fileName")
    List<String> findVectorIdsByFileName(@Param("fileName") String fileName);
    
    @Query("select c.chunkSeq from EmbeddingChunk c where c.fileName = :fileName")
    List<Long> findChunkSeqsByFileName(@Param("fileName") String fileName);

}
