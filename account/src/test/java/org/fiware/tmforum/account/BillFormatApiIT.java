package org.fiware.tmforum.account;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiware.ngsi.api.EntitiesApiClient;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.account.api.BillFormatApiTestSpec;
import org.fiware.account.api.BillFormatApiTestClient;
import org.fiware.tmforum.account.domain.BillFormat;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.account.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@MicronautTest(packages = { "org.fiware.tmforum.account" })
public class BillFormatApiIT extends AbstractApiIT implements BillFormatApiTestSpec {

    public final BillFormatApiTestClient billFormatApiTestClient;

    private String message;
    private BillFormatCreateVO billFormatCreateVO;
    private BillFormatUpdateVO billFormatUpdateVO;
    private BillFormatVO expectedBillFormat;

    public BillFormatApiIT(BillFormatApiTestClient billFormatApiTestClient, EntitiesApiClient entitiesApiClient,
                        ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.billFormatApiTestClient = billFormatApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return BillFormat.TYPE_BILLF;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        when(eventHandler.handleDeleteEvent(any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @ParameterizedTest
    @MethodSource("provideValidBillFormats")
    public void createBillFormat201(String message, BillFormatCreateVO billFormatCreateVO, BillFormatVO expectedBillFormat)
            throws Exception {
        this.message = message;
        this.billFormatCreateVO = billFormatCreateVO;
        this.expectedBillFormat = expectedBillFormat;
        createBillFormat201();
    }
    
    @Override
    public void createBillFormat201() throws Exception {

        HttpResponse<BillFormatVO> billFormatVOHttpResponse = callAndCatch(
                () -> billFormatApiTestClient.createBillFormat(billFormatCreateVO));
        assertEquals(HttpStatus.CREATED, billFormatVOHttpResponse.getStatus(), message);
        String billFormatId = billFormatVOHttpResponse.body().getId();
        expectedBillFormat.setId(billFormatId);
        expectedBillFormat.setHref(billFormatId);

        assertEquals(expectedBillFormat, billFormatVOHttpResponse.body(), message);


    }

    private static Stream<Arguments> provideValidBillFormats() {
        List<Arguments> testEntries = new ArrayList<>();

        BillFormatCreateVO billFormatCreateVO = BillFormatCreateVOTestExample.build();
        BillFormatVO expectedBillFormat = BillFormatVOTestExample.build();
        testEntries.add(Arguments.of("An empty billFormat should have been created.", billFormatCreateVO, expectedBillFormat));

        return testEntries.stream();
    }


    @ParameterizedTest
    @MethodSource("provideInvalidBillFormats")
    public void createBillFormat400(String message, BillFormatCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.billFormatCreateVO = invalidCreateVO;
        createBillFormat400();
    }

    @Override
    public void createBillFormat400() throws Exception {

    }

    private static Stream<Arguments> provideInvalidBillFormats() {
        List<Arguments> testEntries = new ArrayList<>();

        return testEntries.stream();
    }

    @Override
    public void createBillFormat401() throws Exception {

    }

    @Override
    public void createBillFormat403() throws Exception {

    }

    @Override
    public void createBillFormat405() throws Exception {

    }

    @Override
    public void createBillFormat409() throws Exception {

    }

    @Override
    public void createBillFormat500() throws Exception {

    }

    @Override
    public void deleteBillFormat204() throws Exception {

    }

    @Override
    public void deleteBillFormat400() throws Exception {

    }

    @Override
    public void deleteBillFormat401() throws Exception {

    }

    @Override
    public void deleteBillFormat403() throws Exception {

    }

    @Override
    public void deleteBillFormat404() throws Exception {

    }

    @Override
    public void deleteBillFormat405() throws Exception {

    }

    @Override
    public void deleteBillFormat409() throws Exception {

    }

    @Override
    public void deleteBillFormat500() throws Exception {

    }

    @Override
    public void listBillFormat200() throws Exception {

    }

    @Override
    public void listBillFormat400() throws Exception {

    }

    @Override
    public void listBillFormat401() throws Exception {

    }

    @Override
    public void listBillFormat403() throws Exception {

    }

    @Override
    public void listBillFormat404() throws Exception {

    }

    @Override
    public void listBillFormat405() throws Exception {

    }

    @Override
    public void listBillFormat409() throws Exception {

    }

    @Override
    public void listBillFormat500() throws Exception {

    }

    @Override
    public void patchBillFormat200() throws Exception {

    }

    @Override
    public void patchBillFormat400() throws Exception {

    }

    @Override
    public void patchBillFormat401() throws Exception {

    }

    @Override
    public void patchBillFormat403() throws Exception {

    }

    @Override
    public void patchBillFormat404() throws Exception {

    }

    @Override
    public void patchBillFormat405() throws Exception {

    }

    @Override
    public void patchBillFormat409() throws Exception {

    }

    @Override
    public void patchBillFormat500() throws Exception {

    }

    @Override
    public void retrieveBillFormat200() throws Exception {

    }

    @Override
    public void retrieveBillFormat400() throws Exception {

    }

    @Override
    public void retrieveBillFormat401() throws Exception {

    }

    @Override
    public void retrieveBillFormat403() throws Exception {

    }

    @Override
    public void retrieveBillFormat404() throws Exception {

    }

    @Override
    public void retrieveBillFormat405() throws Exception {

    }

    @Override
    public void retrieveBillFormat409() throws Exception {

    }

    @Override
    public void retrieveBillFormat500() throws Exception {

    }

}
