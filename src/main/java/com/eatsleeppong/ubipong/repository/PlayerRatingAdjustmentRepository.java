package com.eatsleeppong.ubipong.repository;

import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface PlayerRatingAdjustmentRepository extends JpaRepository<PlayerRatingAdjustment, Integer> {
    Page<PlayerRatingAdjustment> findByPlayerId(@Param("playerId") Integer playerId, Pageable pageable);
    Page<PlayerRatingAdjustment> findByTournamentId(@Param("tournamentId") Integer tournamentId, Pageable pageable);
}

