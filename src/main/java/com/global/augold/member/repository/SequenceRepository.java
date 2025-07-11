package com.global.augold.member.repository;

import com.global.augold.member.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SequenceRepository extends JpaRepository<Customer, Integer> {

    @Query(value = "SELECT GET_NEXT_SEQUENCE(:tableName)", nativeQuery = true)
    String getNextSequence(@Param("tableName") String tableName);
}