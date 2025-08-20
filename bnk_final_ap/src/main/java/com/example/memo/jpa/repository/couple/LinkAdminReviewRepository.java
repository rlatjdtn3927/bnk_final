package com.example.memo.jpa.repository.couple;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.memo.jpa.entity.couple.LinkAdminReview;

@Repository
public interface LinkAdminReviewRepository extends JpaRepository<LinkAdminReview, Long>{

}
