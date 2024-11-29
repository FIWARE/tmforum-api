package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CategoryApiTestClient;
import org.fiware.productcatalog.api.CategoryApiTestSpec;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.product.Category;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"})
public class CategoryApiIT extends AbstractApiIT implements CategoryApiTestSpec {

    public final CategoryApiTestClient categoryApiTestClient;

    private String message;
    private CategoryCreateVO categoryCreateVO;
    private CategoryUpdateVO categoryUpdateVO;
    private CategoryVO expectedCategory;

    private Clock clock = mock(Clock.class);

    public CategoryApiIT(CategoryApiTestClient categoryApiTestClient, EntitiesApiClient entitiesApiClient,
                         ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.categoryApiTestClient = categoryApiTestClient;
    }

    @MockBean(Clock.class)
    public Clock clock() {
        return clock;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @ParameterizedTest
    @MethodSource("provideValidCategories")
    public void createCategory201(String message, CategoryCreateVO categoryCreateVO, CategoryVO expectedCategory)
            throws Exception {
        this.message = message;
        this.categoryCreateVO = categoryCreateVO;
        this.expectedCategory = expectedCategory;
        createCategory201();
    }

    @Override
    public void createCategory201() throws Exception {

        HttpResponse<CategoryVO> categoryVOHttpResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.CREATED, categoryVOHttpResponse.getStatus(), message);
        String categoryId = categoryVOHttpResponse.body().getId();
        expectedCategory.setId(categoryId);
        expectedCategory.setHref(categoryId);
        expectedCategory.setParentId(null);

        assertEquals(expectedCategory, categoryVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidCategories() {
        List<Arguments> testEntries = new ArrayList<>();

        CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build();
        categoryCreateVO.parentId(null);
        CategoryVO expectedCategory = CategoryVOTestExample.build();
        testEntries.add(
                Arguments.of("An empty category should have been created.", categoryCreateVO, expectedCategory));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCategories")
    public void createCategory400(String message, CategoryCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.categoryCreateVO = invalidCreateVO;
        createCategory400();
    }

    @Override
    public void createCategory400() throws Exception {
        HttpResponse<CategoryVO> creationResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidCategories() {
        List<Arguments> testEntries = new ArrayList<>();

        CategoryCreateVO invalidProductOfferingCreate = CategoryCreateVOTestExample.build();

        // no valid id
        ProductOfferingRefVO invalidProductOffering = ProductOfferingRefVOTestExample.build();
        invalidProductOfferingCreate.setProductOffering(List.of(invalidProductOffering));
        testEntries.add(Arguments.of("A category with invalid product offerings should not be created.",
                invalidProductOfferingCreate));

        CategoryCreateVO nonExistentProductOfferingCreate = CategoryCreateVOTestExample.build();
        // no existent id
        ProductOfferingRefVO nonExistentProductOffering = ProductOfferingRefVOTestExample.build();
        nonExistentProductOffering.setId("urn:ngsi-ld:product-offering:non-existent");
        nonExistentProductOfferingCreate.setProductOffering(List.of(nonExistentProductOffering));
        testEntries.add(Arguments.of("A category with non-existent product offerings should not be created.",
                nonExistentProductOfferingCreate));

        CategoryCreateVO invalidCategoryCreate = CategoryCreateVOTestExample.build();
        // no valid id
        CategoryRefVO categoryRef = CategoryRefVOTestExample.build();
        invalidCategoryCreate.setSubCategory(List.of(categoryRef));
        testEntries.add(
                Arguments.of("A category with invalid sub-categories should not be created.", invalidCategoryCreate));

        CategoryCreateVO nonExistentCategoryCreate = CategoryCreateVOTestExample.build();
        // no existent id
        CategoryRefVO nonExistentCategoryRef = CategoryRefVOTestExample.build();
        nonExistentCategoryRef.setId("urn:ngsi-ld:category:non-existent");
        nonExistentCategoryCreate.subCategory(List.of(nonExistentCategoryRef));
        testEntries.add(Arguments.of("A category with non-existent sub-categories should not be created.",
                nonExistentCategoryCreate));

        CategoryCreateVO invalidParentCreate = CategoryCreateVOTestExample.build();
        // no valid id
        invalidParentCreate.setParentId("invalid");
        testEntries.add(Arguments.of("A category with an invalid parent should not be created.", invalidParentCreate));

        CategoryCreateVO nonExistentParentCreate = CategoryCreateVOTestExample.build();
        nonExistentParentCreate.setParentId("urn:ngsi-ld:category:non-existent");
        testEntries.add(
                Arguments.of("A category with non-existent parent should not be created.", nonExistentParentCreate));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createCategory401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createCategory403() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createCategory405() throws Exception {

    }

    @Disabled("Category doesn't have 'implicit' entities and id is generated by the implementation, no conflict possible.")
    @Test
    @Override
    public void createCategory409() throws Exception {

    }

    @Override
    public void createCategory500() throws Exception {

    }

    @Test
    @Override
    public void deleteCategory204() throws Exception {
        //first create
        CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build();
        categoryCreateVO.setParentId(null);
        HttpResponse<CategoryVO> createResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

        String catalogId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> categoryApiTestClient.deleteCategory(null, catalogId)).getStatus(),
                "The category should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> categoryApiTestClient.retrieveCategory(null, catalogId, null)).status(),
                "The category should not exist anymore.");

    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteCategory400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCategory401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCategory403() throws Exception {

    }

    @Test
    @Override
    public void deleteCategory404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> categoryApiTestClient.deleteCategory(null, "urn:ngsi-ld:category:no-catalog"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such catalog should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> categoryApiTestClient.deleteCategory(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such catalog should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteCategory405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteCategory409() throws Exception {

    }

    @Override
    public void deleteCategory500() throws Exception {

    }

    @Test
    @Override
    public void listCategory200() throws Exception {
        List<CategoryVO> expectedCategorys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build().parentId(null);
            String id = categoryApiTestClient.createCategory(null, categoryCreateVO).body().getId();
            CategoryVO categoryVO = CategoryVOTestExample.build();
            categoryVO
                    .id(id)
                    .href(id)
                    .parentId(null);
            expectedCategorys.add(categoryVO);
        }

        HttpResponse<List<CategoryVO>> categoryResponse = callAndCatch(
                () -> categoryApiTestClient.listCategory(null, null, null, null));

        assertEquals(HttpStatus.OK, categoryResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedCategorys.size(), categoryResponse.getBody().get().size(),
                "All categorys should have been returned.");
        List<CategoryVO> retrievedCategorys = categoryResponse.getBody().get();

        Map<String, CategoryVO> retrievedMap = retrievedCategorys.stream()
                .collect(Collectors.toMap(category -> category.getId(), category -> category));

        expectedCategorys.stream()
                .forEach(expectedCategory -> assertTrue(retrievedMap.containsKey(expectedCategory.getId()),
                        String.format("All created categorys should be returned - Missing: %s.", expectedCategory,
                                retrievedCategorys)));
        expectedCategorys.stream().forEach(
                expectedCategory -> assertEquals(expectedCategory, retrievedMap.get(expectedCategory.getId()),
                        "The correct categorys should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<CategoryVO>> firstPartResponse = callAndCatch(
                () -> categoryApiTestClient.listCategory(null, null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<CategoryVO>> secondPartResponse = callAndCatch(
                () -> categoryApiTestClient.listCategory(null, null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedCategorys.clear();
        retrievedCategorys.addAll(firstPartResponse.body());
        retrievedCategorys.addAll(secondPartResponse.body());
        expectedCategorys.stream()
                .forEach(expectedCategory -> assertTrue(retrievedMap.containsKey(expectedCategory.getId()),
                        String.format("All created categorys should be returned - Missing: %s.", expectedCategory)));
        expectedCategorys.stream().forEach(
                expectedCategory -> assertEquals(expectedCategory, retrievedMap.get(expectedCategory.getId()),
                        "The correct categorys should be retrieved."));
    }

    @Test
    @Override
    public void listCategory400() throws Exception {
        HttpResponse<List<CategoryVO>> badRequestResponse = callAndCatch(
                () -> categoryApiTestClient.listCategory(null, null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> categoryApiTestClient.listCategory(null, null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listCategory401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listCategory403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answerd with an empty list instead.")
    @Test
    @Override
    public void listCategory404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listCategory405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listCategory409() throws Exception {

    }

    @Override
    public void listCategory500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideCategoryUpdates")
    public void patchCategory200(String message, CategoryUpdateVO categoryUpdateVO, CategoryVO expectedCategory)
            throws Exception {
        this.message = message;
        this.categoryUpdateVO = categoryUpdateVO;
        this.expectedCategory = expectedCategory;
        patchCategory200();
    }

    @Override
    public void patchCategory200() throws Exception {
        //first create
        CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build();
        categoryCreateVO.setParentId(null);
        HttpResponse<CategoryVO> createResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

        String catalogId = createResponse.body().getId();

        // the generated example for parentId is not valid, thus set it to null
        categoryUpdateVO.setParentId(null);

        HttpResponse<CategoryVO> updateResponse = callAndCatch(
                () -> categoryApiTestClient.patchCategory(null, catalogId, categoryUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        CategoryVO updatedCategory = updateResponse.body();
        expectedCategory.setHref(catalogId);
        expectedCategory.setId(catalogId);
        expectedCategory.setParentId(null);

        assertEquals(expectedCategory, updatedCategory, message);
    }

    private static Stream<Arguments> provideCategoryUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        CategoryUpdateVO newDesc = CategoryUpdateVOTestExample.build();
        newDesc.setDescription("New description");
        CategoryVO expectedNewDesc = CategoryVOTestExample.build();
        expectedNewDesc.setDescription("New description");
        testEntries.add(Arguments.of("The description should have been updated.", newDesc, expectedNewDesc));

        CategoryUpdateVO newLifeCycle = CategoryUpdateVOTestExample.build();
        newLifeCycle.setLifecycleStatus("Dead");
        CategoryVO expectedNewLifeCycle = CategoryVOTestExample.build();
        expectedNewLifeCycle.setLifecycleStatus("Dead");
        testEntries.add(
                Arguments.of("The lifecycle state should have been updated.", newLifeCycle, expectedNewLifeCycle));

        CategoryUpdateVO newName = CategoryUpdateVOTestExample.build();
        newName.setName("New name");
        CategoryVO expectedNewName = CategoryVOTestExample.build();
        expectedNewName.setName("New name");
        testEntries.add(Arguments.of("The name should have been updated.", newName, expectedNewName));

        CategoryUpdateVO newVersion = CategoryUpdateVOTestExample.build();
        newVersion.setVersion("1.23.1");
        CategoryVO expectedNewVersion = CategoryVOTestExample.build();
        expectedNewVersion.setVersion("1.23.1");
        testEntries.add(Arguments.of("The version should have been updated.", newVersion, expectedNewVersion));

        CategoryUpdateVO newValidFor = CategoryUpdateVOTestExample.build();
        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build();
        timePeriodVO.setEndDateTime(Instant.now());
        timePeriodVO.setStartDateTime(Instant.now());
        newValidFor.setValidFor(timePeriodVO);
        CategoryVO expectedNewValidFor = CategoryVOTestExample.build();
        expectedNewValidFor.setValidFor(timePeriodVO);
        testEntries.add(Arguments.of("The validFor should have been updated.", newValidFor, expectedNewValidFor));

        CategoryUpdateVO newIsRoot = CategoryUpdateVOTestExample.build();
        newIsRoot.setIsRoot(true);
        CategoryVO expectedNewIsRoot = CategoryVOTestExample.build();
        expectedNewIsRoot.setIsRoot(true);
        testEntries.add(Arguments.of("The isRoot should have been updated.", newIsRoot, expectedNewIsRoot));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchCategory400(String message, CategoryUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.categoryUpdateVO = invalidUpdateVO;
        patchCategory400();
    }

    @Override
    public void patchCategory400() throws Exception {
        //first create
        CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build();
        categoryCreateVO.setParentId(null);
        HttpResponse<CategoryVO> createResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The catalog should have been created first.");

        String catalogId = createResponse.body().getId();

        HttpResponse<CategoryVO> updateResponse = callAndCatch(
                () -> categoryApiTestClient.patchCategory(null, catalogId, categoryUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        CategoryUpdateVO invalidProductOfferingCreate = CategoryUpdateVOTestExample.build();

        // no valid id
        ProductOfferingRefVO invalidProductOffering = ProductOfferingRefVOTestExample.build();
        invalidProductOfferingCreate.setProductOffering(List.of(invalidProductOffering));
        testEntries.add(Arguments.of("A category with invalid product offerings should not be created.",
                invalidProductOfferingCreate));

        CategoryUpdateVO nonExistentProductOfferingCreate = CategoryUpdateVOTestExample.build();
        // no existent id
        ProductOfferingRefVO nonExistentProductOffering = ProductOfferingRefVOTestExample.build();
        nonExistentProductOffering.setId("urn:ngsi-ld:product-offering:non-existent");
        nonExistentProductOfferingCreate.setProductOffering(List.of(nonExistentProductOffering));
        testEntries.add(Arguments.of("A category with non-existent product offerings should not be created.",
                nonExistentProductOfferingCreate));

        CategoryUpdateVO invalidCategoryCreate = CategoryUpdateVOTestExample.build();
        // no valid id
        CategoryRefVO categoryRef = CategoryRefVOTestExample.build();
        invalidCategoryCreate.setSubCategory(List.of(categoryRef));
        testEntries.add(
                Arguments.of("A category with invalid sub-categories should not be created.", invalidCategoryCreate));

        CategoryUpdateVO nonExistentCategoryCreate = CategoryUpdateVOTestExample.build();
        // no existent id
        CategoryRefVO nonExistentCategoryRef = CategoryRefVOTestExample.build();
        nonExistentCategoryRef.setId("urn:ngsi-ld:category:non-existent");
        nonExistentCategoryCreate.subCategory(List.of(nonExistentCategoryRef));
        testEntries.add(Arguments.of("A category with non-existent sub-categories should not be created.",
                nonExistentCategoryCreate));

        CategoryUpdateVO invalidParentCreate = CategoryUpdateVOTestExample.build();
        // no valid id
        invalidParentCreate.setParentId("invalid");
        testEntries.add(Arguments.of("A category with an invalid parent should not be created.", invalidParentCreate));

        CategoryUpdateVO nonExistentParentCreate = CategoryUpdateVOTestExample.build();
        nonExistentParentCreate.setParentId("urn:ngsi-ld:category:non-existent");
        testEntries.add(
                Arguments.of("A category with non-existent parent should not be created.", nonExistentParentCreate));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchCategory401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchCategory403() throws Exception {

    }

    @Test
    @Override
    public void patchCategory404() throws Exception {
        CategoryUpdateVO categoryUpdateVO = CategoryUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> categoryApiTestClient.patchCategory(null, "urn:ngsi-ld:category:not-existent",
                        categoryUpdateVO)).getStatus(),
                "Non existent categories should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchCategory405() throws Exception {

    }

    @Disabled("No implicit creations, cannot happen.")
    @Test
    @Override
    public void patchCategory409() throws Exception {

    }

    @Override
    public void patchCategory500() throws Exception {

    }

    @Test
    @Override
    public void retrieveCategory200() throws Exception {
        Instant currentTimeInstant = Instant.ofEpochSecond(10000);

        when(clock.instant()).thenReturn(currentTimeInstant);

        //first create
        CategoryCreateVO categoryCreateVO = CategoryCreateVOTestExample.build();
        // we dont have a parent
        categoryCreateVO.setParentId(null);
        HttpResponse<CategoryVO> createResponse = callAndCatch(
                () -> categoryApiTestClient.createCategory(null, categoryCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The category should have been created first.");
        String id = createResponse.body().getId();

        CategoryVO expectedCategory = CategoryVOTestExample.build();
        expectedCategory.setId(id);
        expectedCategory.setHref(id);
        expectedCategory.setLastUpdate(currentTimeInstant);
        expectedCategory.setParentId(null);

        //then retrieve
        HttpResponse<CategoryVO> retrievedCategory = callAndCatch(
                () -> categoryApiTestClient.retrieveCategory(null, id, null));
        assertEquals(HttpStatus.OK, retrievedCategory.getStatus(), "The retrieval should be ok.");
        assertEquals(expectedCategory, retrievedCategory.body(), "The correct category should be returned.");
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveCategory400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveCategory401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveCategory403() throws Exception {
    }

    @Test
    @Override
    public void retrieveCategory404() throws Exception {
        HttpResponse<CategoryVO> response = callAndCatch(
                () -> categoryApiTestClient.retrieveCategory(null, "urn:ngsi-ld:category:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such category should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveCategory405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveCategory409() throws Exception {

    }

    @Override
    public void retrieveCategory500() throws Exception {

    }

    @Override
    protected String getEntityType() {
        return Category.TYPE_CATEGORY;
    }
}
