package com.kaankaplan.movieService.dao;

import com.kaankaplan.movieService.entity.Saloon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaloonDao extends JpaRepository<Saloon, Integer> {
}
