
@team #API -> DB
Feature: Add new team API and DB validation

  @db     #    API        ->      DB
  Scenario: Post new team and verify in database
    Given User logged in to Bookit api as teacher role
    When Users sends POST request to "/api/teams/team" with following info:
      | campus-location | VA |
      | batch-number | 26 |
      | team-name | Wooden Spoon01231712 |
    Then status code should be 201
    And Database should persist same team info
    And User deletes previously created team

 @teams @db
  Scenario Outline: 2 Point Team info verification. API and Database
    Given User logged in to Bookit api as teacher role
    And User sends GET request to "/api/teams/{id}" with "<team_id>"
    Then status code should be 200
    And Team name should be "<team_name>" in response
    And Database query should have same "<team_id>" and "<team_name>"
    Examples:
      | team_id | team_name |
      |  11267 | BugBusters |
      |  11269 | PhantomPain |
      |  11275 | Scarface |
      |  11281 | TheyBite |
      |  11287 | LoafandCamouflage |
      |  11293 | TeaForTwo |
      |  11299 | Galaxina |
      |  11305 | GangsofWasseypur |
      |  11311 | Breathless |
      |  11317 | Fountain |
      |  11323 | Volga |
      |  11329 | BadRonald |
      |  11335 | RiseofCatherinetheGreat |
      |  11341 | Borderland |
      |  11347 | Stardom |
      |  11353 | CatBallou |
      |  11359 | BadBoys |
      |  11365 | FearIsland |
      |  11371 | AnniversaryParty |
      |  11377 | Jumanji |
      |  11383 | Untraceable |
      |  11389 | BlingRing |
      |  11395 | OctoberSky |
      |  11401 | Angel |
      |  11407 | StoryofUs |
      |  11413 | LifeItself |
      |  11419 | Stay |
      |  11425 | Motorama |
      |  11431 | BlitheSpirit |
      |  11437 | Samurai |
      |  11443 | HighSierra |
      |  11449 | CurbDance |
      |  11455 | Super |
      |  11461 | Marjoe |
      |  11467 | Kicking |
      |  11473 | HeartyPaws |
      |  11479 | CoolasIce |
      |  11485 | MadeinAmerica |
      |  11491 | RoadHard |
      |  11497 | AsGoodasItGets |
      |  11503 | QuestforFire |
      |  11509 | teachers |
#
#    # String sql = "SELECT id, name FROM team WHERE id = "+ teamId ;
#    #
#    #        Map<String, Object> dbTeamInfo = DBUtils.getRowMap(sql);