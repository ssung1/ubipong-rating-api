package com.eatsleeppong.ubipong.rating.repository;

import com.eatsleeppong.ubipong.rating.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    Optional<Tournament> findByName(@Param("name") String name);
}
