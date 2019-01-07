package com.eatsleeppong.ubipong.repository;

import com.eatsleeppong.ubipong.entity.Player;
import com.eatsleeppong.ubipong.entity.PlayerRatingAdjustment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByUserName(@Param("userName") String userName);
}
