package com.example.memo.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.EmbeddingChunk;

public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunk, Long> {
	// ⭐ 파일명으로 존재하는지 확인하는 메서드 추가
    boolean existsByFileName(String fileName);
}
