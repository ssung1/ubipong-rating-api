package com.eatsleeppong.ubipong.rating.controller;

import com.eatsleeppong.ubipong.rating.model.RatingAdjustmentResponseLineItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class TestRatingController {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testInvalidCsvFormatResponseMissingLine1() throws Exception {
        mockMvc.perform(
                post("/rest/v0/rating/rating-adjustment")
                .contentType("text/csv")
                .content(""))

            .andDo(print())

            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testInvalidCsvFormatResponseMissingTournamentName() throws Exception {
        mockMvc.perform(
            post("/rest/v0/rating/rating-adjustment")
                .contentType("text/csv")
                .content("stuff stuff"))

            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("message").value(is("Missing tournament name on line 1: stuff stuff")));
    }

    @Test
    public void testInvalidCsvFormatResponseMissingLine2() throws Exception {
        mockMvc.perform(
            post("/rest/v0/rating/rating-adjustment")
                .contentType("text/csv")
                .content("tournamentName, test tournament"))

            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("message").value(is("Missing line 2")));
    }

    @Test
    public void testInvalidCsvFormatResponseMissingTournamentDate() throws Exception {
        mockMvc.perform(
            post("/rest/v0/rating/rating-adjustment")
                .contentType("text/csv")
                .content(
                        "tournamentName, test tournament\n" +
                        "date\n"
                ))

            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("message").value(is("Missing tournament date on line 2: date")));
    }

    @Test
    public void testInvalidCsvFormatResponseMissingPlayerRatingHeader() throws Exception {
        mockMvc.perform(
            post("/rest/v0/rating/rating-adjustment")
                .contentType("text/csv")
                .content(
                        "tournamentName, test tournament\n" +
                        "date, 2019-01-13T17:59:00-0500\n"
                ))

            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("message").value(is("Missing line 3")));
    }

    @Test
    public void testPostRatingAdjustmentDefaultNoAutoAddPlayerMissingPlayer() throws Exception {
        mockMvc.perform(
                post("/rest/v0/rating/rating-adjustment")
                        .contentType("text/csv")
                        .content(
                                "tournamentName, test tournament\n" +
                                "date, 2019-01-13T17:59:00-0500\n" +
                                "player, rating\n" +
                                "invalid-player, 1234\n"
                        ))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("ratingAdjustmentResponseList[0].processed").value(is(false)))
                .andExpect(jsonPath("ratingAdjustmentResponseList[0].rejectReason")
                        .value(is(RatingAdjustmentResponseLineItem.REJECT_REASON_INVALID_PLAYER)));
    }
}
