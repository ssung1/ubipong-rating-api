# ubipong-api

## Tournament Organization

- Tournament (eg. Atlanta Giant Round Robin)
- Event (eg. Prelim Group 1)

  *An event is mapped to a "tournamentId" on challonge.com*

  *An event is represented by `List<MatchWrapper>`*

- Round (eg. Round of 16)

  For single elimination, each round is defined by the number of
  players in that round.

  For round robin, the nth round is roughly the nth match of a single
  playerId, but it is not as important to divide round robin into
  rounds.

- Match

## Deploying to Heroku

If Java plugin has not been installed:

    heroku plugins:install java

If application does not yet exist on Heroku:

    heroku create --no-remote

Deploy:

    heroku deploy:jar target/my-app.jar --app sushi


## Uploading Rating Adjustment

Rating adjustment only contains the players and the ratings.  These are not match results.  This is useful if you
have lost the results and only want to update the player ratings.  The format of the payload is text/csv:

    line 1                tournamentId         , {tournament name}
    line 2                date                 , {date, in ISO8601 format}
    line 3                player               , rating
    line 4                {player1_username}   , {player1_rating}
    line 5                {player2_username}   , {player2_rating}
    line 6                {player3_username}   , {player3_rating}
    ...

