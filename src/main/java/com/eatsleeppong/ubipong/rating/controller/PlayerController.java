package com.eatsleeppong.ubipong.rating.controller;

import com.eatsleeppong.ubipong.rating.entity.Player;
import com.eatsleeppong.ubipong.rating.manager.PlayerManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.Optional;

@RestController
@RequestMapping("/rest/v0/player")
public class PlayerController {
    private PlayerManager playerManager;

    public PlayerController(
            final PlayerManager playerManager
    ) {
        this.playerManager = playerManager;
    }

    @GetMapping(value = "{player}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlayer(@PathVariable final String player) {
        Optional<Player> result = playerManager.getPlayer(player);

        if (result.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(result.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(MessageFormat.format("Could not find player \"{0}\"", player));
        }
    }
}