package com.devsuperior.dscommerce.controllers;

import com.devsuperior.dscommerce.tests.TokenUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

public class ProductControllerRA {
    private String clientUsername, clientPassword, adminUsername, adminPassword;
    private String clientToken, adminToken, invalidToken;
    private Long existingId, nonExistingId, dependentId;
    private String productName;

    private Map<String, Object> postProductInstance;

    @BeforeEach
    void setUp() throws Exception{
        productName = "Macbook";
        existingId = 2L;

        clientUsername = "maria@gmail.com";
        clientPassword = "123456";
        adminUsername = "alex@gmail.com";
        adminPassword = "123456";

        clientToken = TokenUtil.obtainAccessToken(clientUsername, clientPassword);
        adminToken = TokenUtil.obtainAccessToken(adminUsername, adminPassword);
        invalidToken = adminToken + "xpto";

        postProductInstance = new HashMap<>();
        postProductInstance.put("name", "Meu produto");
        postProductInstance.put("description", "Lorem ipsum, dolor sit amet consectetur adipisicing elit. Qui ad, adipisci illum ipsam velit et odit eaque reprehenderit ex maxime delectus dolore labore, quisquam quae tempora natus esse aliquam veniam doloremque quam minima culpa alias maiores commodi. Perferendis enim");
        postProductInstance.put("imgUrl", "https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg");
        postProductInstance.put("price", 50.0);

        List<Map<String, Object>> categories = new ArrayList<>();

        Map<String, Object> category1 = new HashMap<>();
        category1.put("id", 2);

        Map<String, Object> category2 = new HashMap<>();
        category2.put("id", 3);

        categories.add(category1);
        categories.add(category2);

        postProductInstance.put("categories", categories);

        baseURI = "http://localhost:8080";
    }

    @Test
    public void findByIdShouldReturnProductWhenIdExists(){
        given()
                .get("/products/{id}", existingId)
                .then()
                .statusCode(200)
                .body("id", is(2))
                .body("name", equalTo("Smart TV"))
                .body("imgUrl", equalTo("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/2-big.jpg"))
                .body("price", is(2190.0F))
                .body("categories.id", hasItems(2,3))
                .body("categories.name", hasItems("Eletrônicos", "Computadores"));
    }

    @Test
    public void findAllShouldReturnPagedProductsWhenProductNameIsEmpty() {
        given()
                .get("/products?page=0")
                .then()
                .statusCode(200)
                .body("content.name", hasItems("Macbook Pro","PC Gamer Tera"));
    }

    @Test
    public void findAllShouldReturnPagedProductsWhenProductNameIsNotEmpty(){
        given()
                .get("/products?name={productName}", productName)
                .then()
                .statusCode(200)
                .body("content.id[0]", is(3))
                .body("content.name[0]", equalTo("Macbook Pro"))
                .body("content.price[0]", is(1250.0F))
                .body("content.imgUrl[0]", equalTo("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/3-big.jpg"));
    }

    @Test
    public void findAllShouldReturnPagedProductsWithPriceGreaterThan2000(){
        given()
                .get("/products?size=25")
                .then()
                .statusCode(200)
                .body("content.findAll { it.price > 2000 }.name", hasItems("Smart TV", "PC Gamer Weed"));

    }

    @Test
    public void insertShouldReturnProductCreatedWhenAdminLogged(){
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .body("name", equalTo("Meu produto"))
                .body("price", is(50.0F))
                .body("imgUrl", equalTo("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg"))
                .body("categories.id", hasItems(2,3));
    }

    @Test
    public void insertShouldReturnUnprocessableWhenAdminLoggedAndInvalidName(){
        postProductInstance.put("name", "ab");
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(422)
                .body("errors.message[0]", equalTo("Nome precisar ter de 3 a 80 caracteres"));
    }

    @Test
    public void insertShouldReturnUnprocessableWhenAdminLoggedAndInvalidDescription(){
        postProductInstance.put("description", "Loren ");
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(422)
                .body("errors.message[0]", equalTo("Descrição precisa ter no mínimo 10 caracteres"));
    }

    @Test
    public void insertShouldReturnUnprocessableWhenAdminLoggedAndPriceIsNegative(){
        postProductInstance.put("price", -50.0);
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(422)
                .body("errors.message[0]", equalTo("O preço deve ser positivo"));
    }

    @Test
    public void insertShouldReturnUnprocessableWhenAdminLoggedAndPriceIsZero(){
        postProductInstance.put("price", 0.0);
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(422)
                .body("errors.message[0]", equalTo("O preço deve ser positivo"));
    }

    @Test
    public void insertShouldReturnUnprocessableWhenAdminLoggedAndProductHasNoCategory(){

        postProductInstance.put("categories", null);
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(422)
                .body("errors.message[0]", equalTo("Deve ter pelo menos uma categoria"));
    }

    @Test
    public void insertShouldReturnForbiddenWhenClientLogged(){
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + clientToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(403);
    }

    @Test
    public void insertShouldReturnUnauthorizedWhenInvalidToken(){
        JSONObject newProduct = new JSONObject(postProductInstance);

        given()
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .body(newProduct)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(401);
    }

    @Test
    public void deleteShouldReturnNoContentWhenIdExistsAndAdminLogged(){
        existingId = 25L;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/products/{id}", existingId)
                .then()
                .statusCode(204);
    }

    @Test
    public void deleteShouldReturnNotFoundWhenIdDoesNotExistAndAdminLogged(){
        nonExistingId = 100L;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/products/{id}", nonExistingId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Recurso não encontrado"))
                .body("status", equalTo(404));
    }

    @Test
    public void deleteShouldReturnBadRequestWhenDependentIdAndAdminLogged(){
        dependentId = 3L;

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/products/{id}", dependentId)
                .then()
                .statusCode(400);
    }

    @Test
    public void deleteShouldReturnForbiddenWhenClientLogged(){
        existingId = 25L;
        given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .delete("/products/{id}", existingId)
                .then()
                .statusCode(403);
    }

    @Test
    public void deleteShouldReturnUnauthorizedWhenInvalidToken(){
        existingId = 25L;
        given()
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .delete("/products/{id}", existingId)
                .then()
                .statusCode(401);
    }
}

