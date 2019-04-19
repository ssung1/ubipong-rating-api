package com.eatsleeppong.ubipong.rating.manager;

import com.eatsleeppong.ubipong.rating.entity.Player;
import com.eatsleeppong.ubipong.rating.repository.PlayerRatingAdjustmentRepository;
import com.eatsleeppong.ubipong.rating.repository.PlayerRepository;
import com.eatsleeppong.ubipong.rating.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PlayerManager {
    private PlayerRepository playerRepository;

    public PlayerManager(
            PlayerRepository playerRepository
    ) {
        this.playerRepository = playerRepository;
    }

    public Player addPlayer(Player player) {
        return playerRepository.save(player);
    }

    public Optional<Player> getPlayerById(Integer id) {
        return playerRepository.findById(id);
    }

    /**
     * @param search can be ID, username, or {firstname lastname}
     * @return
     */
    public Optional<Player> getPlayer(final String search) {
        return playerRepository.findByUserName(search);
    }

    public Integer getPlayerId(final String search) {
        return getPlayer(search).map(Player::getPlayerId).orElse(null);
    }

    public Optional<Player> getOrCreatePlayer(final String search) {
        final Optional<Player> existing = getPlayer(search);
        if (existing.isPresent()) {
            return existing;
        }
        final Player newUser = new Player();
        // the search term becomes username
        newUser.setUserName(search);

        return Optional.of(playerRepository.save(newUser));
    }

    public List<Player> findPlayerByUserNameStartingWith(final String search) {
        return playerRepository.findByUserNameStartingWith(search);
    }
}