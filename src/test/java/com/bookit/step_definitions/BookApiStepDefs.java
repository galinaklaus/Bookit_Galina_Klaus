package com.bookit.step_definitions;

import com.bookit.pages.HuntPage;
import com.bookit.pages.LogInPage;
import com.bookit.pages.MapPage;
import com.bookit.pages.SelfPage;
import com.bookit.utilities.*;
import com.github.javafaker.Faker;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import static org.junit.Assert.*;
import static io.restassured.RestAssured.*;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class BookApiStepDefs {

    public static final Logger LOG = LogManager.getLogger();
    String baseUrl = Environment.BASE_URL;
    String accessToken;
    Response response;
    //this map is used to share data between steps
    Map<String, String> newRecordMap;

    WebDriverWait wait = new WebDriverWait(Driver.getDriver(), 25);
    HuntPage huntPage = new HuntPage();


    @Given("User logged in to Bookit api as teacher role")
    public void user_logged_in_to_Bookit_api_as_teacher_role() {
        String email = Environment.TEACHER_EMAIL;
        String password = Environment.TEACHER_PASSWORD;
        LOG.info("Authorizing teacher user : email = " + email + ", password = " + password);
        LOG.info("Environment base url = " + baseUrl);

        accessToken = BookItApiUtil.getAccessToken(email, password);

        if (accessToken == null || accessToken.isEmpty()) {
            LOG.error("Could not authorize user in authorization server");
            fail("Could not authorize user in authorization server");
        }

    }

    @Given("User sends GET request to {string}")
    public void user_sends_GET_request_to(String endpoint) { ///api/users/me
        response = given().accept(ContentType.JSON)
                .and().header("Authorization",  accessToken)
                .when().get(baseUrl + endpoint);
        response.then().log().all();
    }



    @Then("content type is {string}")
    public void content_type_is(String expContentType) {
        response.then().contentType(expContentType);
        assertEquals("Content type verification failed. expected = " + expContentType +" but actual = " + response.contentType()
                ,expContentType, response.contentType());
    }

    /**
         {
             "id": 11516,
             "firstName": "Barbabas",
             "lastName": "Lyst",
             "role": "teacher"
         }
     */
    @Then("role is {string}")
    public void role_is(String expRole) {
        assertEquals(expRole, response.path("role"));

        JsonPath jsonPath = response.jsonPath();
        assertEquals(expRole, jsonPath.getString("role"));

        //deserialization: json to map or json to pojo
        Map<String, ?> responseMap = response.as(Map.class);
        assertEquals(expRole, responseMap.get("role"));
    }

    @Given("User logged in to Bookit app as teacher role")
    public void user_logged_in_to_Bookit_app_as_teacher_role() {
        //goto login page
        Driver.getDriver().get(Environment.URL);
        LogInPage logInPage = new LogInPage();
        logInPage.login(Environment.TEACHER_EMAIL, Environment.TEACHER_PASSWORD);
        //TODO: add explicit wait for url change
        //assertTrue(Driver.getDriver().getCurrentUrl().endsWith("map"));
    }

    @Given("User is on self page")
    public void user_is_on_self_page() {
        MapPage mapPage = new MapPage();
        mapPage.gotoSelfPage();
    }

    @Then("User should see same info on UI and API")
    public void user_should_see_same_info_on_UI_and_API() {
        SelfPage selfPage = new SelfPage();
        String fullName = selfPage.fullName.getText();
        String role = selfPage.role.getText();

        Map<String, String> uiUserDataMap = new HashMap<>();
        uiUserDataMap.put("role", role);
        String[] name = fullName.split(" "); //[0] = Firstname, [1] = Lastname
        uiUserDataMap.put("firstName", name[0]);
        uiUserDataMap.put("lastName", name[1]);

        System.out.println("uiUserDataMap = " + uiUserDataMap);

        Map<String, ?> responseMap = response.as(Map.class);
        responseMap.remove("id");//delete id to compare with ui
        assertThat(uiUserDataMap, equalTo(responseMap));

    }

    /**
     {
     "entryiId": 14945,
     "entryType": "Team",
     "message": "team Wooden Spoon7987 has been added to the batch 26."
     }
     */

    @When("Users sends POST request to {string} with following info:") ///api/students/student
    public void users_sends_POST_request_to_with_following_info(String endpoint, Map<String, String> dataMap) {
//       TODO Faker faker = new Faker();
//        String randomEmail = faker.internet().emailAddress();
//        if (dataMap.containsKey("email")) {
//            dataMap.replace("email", randomEmail);
//            System.out.println("randomEmail = " + randomEmail);
//        }
        response =given().accept(ContentType.JSON)
                .and().queryParams(dataMap)
                .and().header("Authorization", accessToken)
                .when().post(baseUrl + endpoint);
        response.prettyPrint();
        //store into newRecordMap so that we can use for validation in next step
        this.newRecordMap = dataMap;
    }

    @Then("Database should persist same team info")
    public void database_should_persist_same_team_info() {
        int newTeamID = response.path("entryiId");

        String sql = "SELECT * FROM team WHERE id = " + newTeamID;
        Map<String, Object> dbNewTeamMap = DBUtils.getRowMap(sql);

        System.out.println("sql = " + sql);
        System.out.println("dbNewTeamMap = " + dbNewTeamMap);

        assertThat(dbNewTeamMap.get("id"), equalTo((long)newTeamID));
        assertThat(dbNewTeamMap.get("name"), equalTo(newRecordMap.get("team-name")));
        assertThat(dbNewTeamMap.get("batch_number").toString(), equalTo(newRecordMap.get("batch-number")));
    }

    @Then("User deletes previously created team")
    public void user_deletes_previously_created_team() {
        int teamId = response.path("entryiId");
        given().accept(ContentType.JSON)
                .and().header("Authorization", accessToken)
                .and().pathParam("id", teamId)
                .when().delete(baseUrl + "/api/teams/{id}")
                .then().log().all();
    }

    /**
     Feature: Add new student

     Scenario: Add new student and verify status code 201
     Given User logged in to Bookit api as teacher role
     When Users sends POST request to "/api/students/student" with following info:
     | first-name      | harold              |
     | last-name       | finch               |
     | email           | harolds4Email78945@gmail.com  |
     | password        | abc123              |
     | role            | student-team-leader |
     | campus-location | VA                  |
     | batch-number    | 8                   |
     | team-name       | Nukes               |
     Then status code should be 201
     And Database should contain same student info
     And User should able to login bookit app on ui
     And User deletes previously created student
     */
    @Then("Database should contain same student info")
    public void database_should_contain_same_student_info() {

        int newStudentId = response.path("entryiId");
        String sql = "SELECT * FROM users WHERE id = " + newStudentId;
        Map<String, Object> dbStudentMap = DBUtils.getRowMap(sql);
        System.out.println("dbStudentMap = " + dbStudentMap);

        assertThat(newRecordMap.get("first-name"), equalTo(dbStudentMap.get("firstname")));
        assertThat(newRecordMap.get("last-name"), equalTo(dbStudentMap.get("lastname")));
        assertThat(newRecordMap.get("role"), equalTo(dbStudentMap.get("role")));
        assertThat(newRecordMap.get("email"), equalTo(dbStudentMap.get("email")));
    }

    @Then("User should able to login bookit app on ui")
    public void user_should_able_to_login_bookit_app_on_ui() {
        Driver.getDriver().get(Environment.URL);
        LogInPage logInPage = new LogInPage();
        logInPage.login(newRecordMap.get("email"), newRecordMap.get("password"));
        MapPage mapPage = new MapPage();
        assertThat(mapPage.myLink.isDisplayed(), is(true));
    }

    /**
     {
     "entryiId": 15379,
     "entryType": "Student",
     "message": "user harold finch has been added to database."
     }
     */
    @Then("User deletes previously created student")
    public void user_deletes_previously_created_student() {
        int newStudentId = response.path("entryiId");
        given().accept(ContentType.JSON)
                .and().header("Authorization",accessToken)
                .and().pathParam("id", newStudentId)
                .when().delete(baseUrl + "/api/students/{id}")
                .then().statusCode(204).log().all();
    }
    /**
     Feature: Team module verifications

     Scenario Outline: 2 Point Team info verification. API and Database
     Given User logged in to Bookit api as teacher role
     And User sends GET request to "/api/teams/{id}" with "<team_id>"
     Then status code should be 200
     And Team name should be "<team_name>" in response
     And Database query should have same "<team_id>" and "<team_name>"
     Examples:
     */

    @And("User sends GET request to {string} with {string}")
    public void userSendsGETRequestToWith(String endPoint, String id) {

       response= given().accept(ContentType.JSON)
                .and().pathParam("id",id)
                .and().header("Authorization",accessToken)
                .when().get(baseUrl+endPoint);
       response.prettyPrint();
    }

    @And("Team name should be {string} in response")
    public void teamNameShouldBeInResponse(String expectedName) {

        JsonPath jsonPath = response.then().extract().jsonPath();

        String actualName = jsonPath.getString("name");

        assertThat(actualName, is(expectedName));
    }

    @And("Database query should have same {string} and {string}")
    public void databaseQueryShouldHaveSameAnd(String teamId, String teamName) {

        int id = Integer.parseInt(teamId);

        String sql = "SELECT id, name FROM team WHERE id = " + id;

        Map<String, Object> dbMap = DBUtils.getRowMap(sql);

       System.out.println("dbMap = " + dbMap);

        assertThat(dbMap.get("name"), is(teamName));
        assertThat(dbMap.get("id"), is((long)id));

    }

    /**
     Feature: Json Schema validation

     Scenario: GET request and perform json schema validation of response
     Given User logged in to Bookit api as team lead role
     When User sends GET request to "/api/students/me"
     Then status code should be 200
     And response should match "json-schemas/student-schema.json" schema
     */

    @Given("User logged in to Bookit api as team lead role")
    public void user_logged_in_to_Bookit_api_as_team_lead_role() {

        String email = Environment.LEADER_EMAIL;
        String password = Environment.LEADER_PASSWORD;
        LOG.info("Authorizing teacher user : email = " + email + ", password = " + password);
        LOG.info("Environment base url = " + baseUrl);

        accessToken = BookItApiUtil.getAccessToken(email, password);

        if (accessToken == null || accessToken.isEmpty()) {
            LOG.error("Could not authorize user in authorization server");
            fail("Could not authorize user in authorization server");
        }

    }

    @Then("response should match {string} schema")
    public void response_should_match_schema(String expectedSchema) {

        response.then().body(JsonSchemaValidator.matchesJsonSchema(
                                new File("src/test/resources/json-schemas/student-schema.json")
                        )).and().log().all();

    }

    /**
     Scenario: Team lead should be able to see the available rooms
     Given User logged in to Bookit app as team lead role
     When User goes to room hunt page
     And User searches for room with date:
     |date |September 4, 2022|
     |from |7:00am           |
     |to   |7:30am           |
     Then User should see available rooms
     And User logged in to Bookit api as team lead role
     And User sends GET request to "/api/rooms/available" with:
     | year | 2022 |
     | month | 9 |
     | day | 4 |
     | conference-type | SOLID |
     | cluster-name | light-side |
     | timeline-id | 8 | --?ask Olya
     Then status code should be 200
     And available rooms in response should match UI results
     And available rooms in database should match UI and API results
     */
    @Given("User logged in to Bookit app as team lead role")
    public void user_logged_in_to_Bookit_app_as_team_lead_role() {

        Driver.getDriver().get(Environment.URL);
        LogInPage logInPage = new LogInPage();

        logInPage.login(Environment.LEADER_EMAIL, Environment.LEADER_PASSWORD);
    }

    @When("User goes to room hunt page")
    public void user_goes_to_room_hunt_page() {

        MapPage mapPage=new MapPage();

        mapPage.huntLink.click();
    }

    @When("User searches for room with date:")
    public void user_searches_for_room_with_date(Map <String,String> dateMap) {

      // Actions actions = new Actions(Driver.getDriver());

       // wait.until(ExpectedConditions.elementToBeSelected(huntPage.dateInput));
        huntPage.dateInput.sendKeys(dateMap.get("date"));

        LOG.info("enter the date");

        wait.until(ExpectedConditions.elementToBeClickable(huntPage.fromDropdown));

        huntPage.fromDropdown.click();

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(huntPage.timeSlot(dateMap.get("from")))));
        Driver.getDriver().findElement(By.xpath(huntPage.timeSlot(dateMap.get("from")))).click();
        LOG.info("enter the start time -> from");

       // for (WebElement eachTime: huntPage.timeList) {

//            wait.until(ExpectedConditions.elementToBeClickable(eachTime));
//            if (eachTime.getText().contains(dateMap.get("from"))){
//
//                eachTime.click();
//                break;
//            }
//        }
        wait.until(ExpectedConditions.elementToBeClickable(huntPage.toDropdown));

        huntPage.toDropdown.click();

        BrowserUtils.waitFor(3);

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//span[@class='mat-option-text']")));

                for (WebElement eachTime: huntPage.timeList) {
                   wait.until(ExpectedConditions.elementToBeClickable(eachTime));
                    if (eachTime.getText().contains(dateMap.get("to"))){

                       eachTime.click();
                        break;
                    }
                }
        LOG.info("enter the end time -> to");

            wait.until(ExpectedConditions.elementToBeClickable(huntPage.searchBtn));
            huntPage.searchBtn.click();

        LOG.info("click submit button");
    }

    @Then("User should see available rooms")
    public void user_should_see_available_rooms() {

        System.out.println("huntPage.roomList.size() = " + huntPage.roomList.size());

        assertTrue(huntPage.roomList.size()>0);

    }

    @Then("User sends GET request to {string} with:")
    public void user_sends_GET_request_to_with(String endPoint, Map<String,String>roomParams) {

        System.out.println("base url "+baseUrl+ endPoint+ " access token "+accessToken);

        response =given().accept(ContentType.JSON)
                .and().queryParams(roomParams)
                .and().header("Authorization", accessToken)
                .when().get(baseUrl + endPoint);
        response.prettyPrint();

    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expStatusCode) {

        System.out.println("response.statusCode() = " + response.statusCode());
        assertEquals("Status code verification failed",expStatusCode, response.statusCode());
//        response.then().statusCode(expStatusCode);
    }
    @Then("available rooms in response should match UI results")
    public void available_rooms_in_response_should_match_UI_results() {

        List <String> apiRooms = response.path("name");
        System.out.println("listApiRooms = " + apiRooms);

        List<String> uiRooms  = new ArrayList<>();

        for (WebElement eachRoom: huntPage.roomList) {
            uiRooms.add(eachRoom.getText());
        }
        System.out.println("room names = " + uiRooms);

        assertEquals("Rooms results do not match",apiRooms, uiRooms);

    }

    @Then("available rooms in database should match UI and API results")
    public void available_rooms_in_database_should_match_UI_and_API_results() {

        String query = "Select room.name from room\n" +
                "inner join cluster c on c.id = room.cluster_id\n" +
                "where c.name='light-side';";

        List<Object> dbRooms = DBUtils.getColumnData(query, "name");

        List <String> apiRooms = response.path("name");

        List<String> uiRooms  = new ArrayList<>();

        for (WebElement eachRoom: huntPage.roomList) {
            uiRooms.add(eachRoom.getText());
        }

        assertEquals("Rooms results (ui vs api) do not match",apiRooms, uiRooms);
        assertEquals("Rooms results (db vs api) do not match",apiRooms, dbRooms);

    }




}
