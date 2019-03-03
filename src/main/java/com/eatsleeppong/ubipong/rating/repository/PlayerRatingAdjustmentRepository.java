package com.eatsleeppong.ubipong.rating.repository;

import com.eatsleeppong.ubipong.rating.entity.PlayerRatingAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface PlayerRatingAdjustmentRepository extends JpaRepository<PlayerRatingAdjustment, Integer> {
    Page<PlayerRatingAdjustment> findByPlayerId(@Param("playerId") Integer playerId, Pageable pageable);
    Page<PlayerRatingAdjustment> findByTournamentId(@Param("tournamentId") Integer tournamentId, Pageable pageable);
}

