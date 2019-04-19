package com.eatsleeppong.ubipong.rating.manager;

import com.eatsleeppong.ubipong.rating.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TestPlayerManager {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private PlayerManager playerManager;

    private Integer spongeBobId = 1;
    private Integer patrickId = 2;
    private Integer squidwardId = 3;

    private String spongeBobUserName = "spongebob";
    private String patrickUserName = "patrick";
    private String squidwardUserName = "squidward";

    private Player spongeBob;
    private Player patrick;
    private Player squidward;

    @Before
    public void setup() throws ParseException {
        spongeBob = new Player();
        spongeBob.setUserName(spongeBobUserName);

        patrick = new Player();
        patrick.setUserName(patrickUserName);

        squidward = new Player();
        squidward.setUserName(squidwardUserName);
    }

    @Test
    public void addPlayer() {
        final Player bob = playerManager.addPlayer(spongeBob);

        assertThat(bob.getPlayerId(), notNullValue());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void userNameMustBeUnique() {
        final String sameUserName = "same_user_name";
        final Player a = new Player();
        a.setUserName(sameUserName);

        final Player b = new Player();
        b.setUserName(sameUserName);

        playerManager.addPlayer(a);
        playerManager.addPlayer(b);

        playerManager.getPlayer(sameUserName);
    }

    @Test
    public void getPlayerByUserName() {
        playerManager.addPlayer(spongeBob);

        final Optional<Player> spongebob = playerManager.getPlayer(spongeBobUserName);

        assertTrue(spongebob.isPresent());
        assertThat(spongebob.get().getUserName(), is(spongeBobUserName));
        assertThat(spongebob.map(Player::getUserName).orElse(null), is(spongeBobUserName));
    }

    @Test
    public void getPlayerById() {
        final Player spongebob1 = playerManager.addPlayer(spongeBob);

        final Optional<Player> spongebob2 = playerManager.getPlayerById(spongebob1.getPlayerId());

        assertThat(spongebob2.map(Player::getUserName).orElse(null), is(spongeBobUserName));
    }

    @Test
    public void getPlayerByInvalidUserName() {
        final Optional<Player> spongebob = playerManager.getPlayer(spongeBobUserName);

        assertFalse(spongebob.isPresent());
    }

    @Test
    public void findPlayerByUserNameStartingWith001() {
        playerManager.addPlayer(spongeBob);
        playerManager.addPlayer(squidward);

        final List<Player> playerList = playerManager.findPlayerByUserNameStartingWith("s");

        assertThat(playerList, hasSize(2));
    }

    @Test
    public void findPlayerByUserNameStartingWith002() {
        playerManager.addPlayer(spongeBob);
        playerManager.addPlayer(squidward);

        final List<Player> playerList = playerManager.findPlayerByUserNameStartingWith("sp");

        assertThat(playerList, hasSize(1));
        assertThat(playerList.get(0).getUserName(), is(spongeBobUserName));
    }
}
