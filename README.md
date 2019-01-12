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