package com.eatsleeppong.ubipong.rating.repository;

import com.eatsleeppong.ubipong.rating.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByUserName(@Param("userName") String userName);
}
