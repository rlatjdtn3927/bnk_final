package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FileTaskList;

public interface FileTaskListRepository extends JpaRepository<FileTaskList, Long>{
	List<FileTaskList> findByProdIdIn(List<String> prodIdList);
}
