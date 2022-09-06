package org.fiware.tmforum.customer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customer.model.*;
import org.fiware.tmforum.customer.rest.CustomerApiController;
import org.junit.jupiter.api.Test;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.customer"})
class CustomerApiIT {

    private final ObjectMapper objectMapper;
    private final CustomerApiController customerApiController;

    @Test
    void test() throws JsonProcessingException, ParseException {
        CustomerCreateVO myFancyCustomerCreate = getMyFancyCustomer();

        // Test create
        HttpResponse<CustomerVO> myFancyCustomerCreateResponse = customerApiController.createCustomer(myFancyCustomerCreate).blockingGet();
        assertEquals(HttpStatus.CREATED, myFancyCustomerCreateResponse.getStatus(), "Customer should have been created");
        CustomerVO myFancyCustomer = myFancyCustomerCreateResponse.body();

        // Test retrieve
        HttpResponse<CustomerVO> customerVOHttpResponse = customerApiController.retrieveCustomer(myFancyCustomer.getId(), null).blockingGet();
        assertEquals(HttpStatus.OK, customerVOHttpResponse.getStatus(), "A customer response is expected.");
        assertTrue(customerVOHttpResponse.getBody().isPresent(), "A customer response is expected.");
        assertEquals(myFancyCustomer, customerVOHttpResponse.getBody().get(), "The full customer should be retrieved");

        // Test delete
        HttpResponse<Object> customerDeleteResponse = customerApiController.deleteCustomer(myFancyCustomer.getId()).blockingGet();
        assertEquals(HttpStatus.NO_CONTENT, customerDeleteResponse.getStatus(), "A NO_CONTENT response is expected");
        
    }

    private CustomerCreateVO getMyFancyCustomer() throws JsonProcessingException {

        CustomerCreateVO customerVO = new CustomerCreateVO();
        customerVO.setName("My Fancy Customer");
        customerVO.setStatus("My fancy status");
        customerVO.setStatusReason("Fancy reason for my fancy status");

        MediumCharacteristicVO mediumCharacteristicVO = new MediumCharacteristicVO();
        mediumCharacteristicVO.setCity("Berlin");
        mediumCharacteristicVO.setContactType("postal address");
        mediumCharacteristicVO.setCountry("Germany");
        mediumCharacteristicVO.setEmailAddress("my-fancy@company.org");
        mediumCharacteristicVO.setPhoneNumber("0123/4567890-0");
        mediumCharacteristicVO.setFaxNumber("0123/4567890-1");
        mediumCharacteristicVO.setPostCode("10719");
        mediumCharacteristicVO.setSocialNetworkId("@fancy");
        mediumCharacteristicVO.setStateOrProvince("Berlin");
        mediumCharacteristicVO.street1("Kurfürstendamm 12");

        ContactMediumVO contactMediumVO = new ContactMediumVO();
        contactMediumVO.setMediumType("postal address");
        contactMediumVO.setPreferred(true);
        contactMediumVO.setCharacteristic(mediumCharacteristicVO);
        contactMediumVO.setValidFor(new TimePeriodVO().startDateTime(Instant.now()).endDateTime(Instant.now().plus(Duration.of(10, ChronoUnit.DAYS))));

        AccountRefVO accountRefVO = new AccountRefVO();
        accountRefVO.setId("urn:ngsi-ld:AccountRef:MyAccountRef001");
        accountRefVO.setName("My AccountRef name");
        accountRefVO.setDescription("My AccountRef description");

        AgreementRefVO agreementRefVO = new AgreementRefVO();
        agreementRefVO.setId("urn:ngsi-ld:AgreementRef:MyAgreementRef001");
        agreementRefVO.setName("My AgreementRef name");

        CharacteristicVO characteristicVO = new CharacteristicVO();
        characteristicVO.setName("My customer characteristic name");
        characteristicVO.setValue("My customer characteristic value");
        characteristicVO.setValueType("String");

        CreditProfileVO creditProfileVO = new CreditProfileVO();
        creditProfileVO.setCreditScore(6);
        creditProfileVO.setCreditRiskRating(4);
        creditProfileVO.setCreditProfileDate(Instant.now());

        PaymentMethodRefVO paymentMethodRefVO = new PaymentMethodRefVO();
        paymentMethodRefVO.setId("urn:ngsi-ld:PaymentMethodRef:MyPaymentMethodRef001");
        paymentMethodRefVO.setName("My PaymentMethodRef name");

        customerVO.setContactMedium(List.of(contactMediumVO));
        customerVO.setAccount(List.of(accountRefVO));
        customerVO.setAgreement(List.of(agreementRefVO));
        customerVO.setCharacteristic(List.of(characteristicVO));
        customerVO.setCreditProfile(List.of(creditProfileVO));
        customerVO.setPaymentMethod(List.of(paymentMethodRefVO));
        customerVO.setValidFor(
                new TimePeriodVO().startDateTime(Instant.now())
                        .endDateTime(Instant.now()
                                .plus(Duration.of(10, ChronoUnit.DAYS))));

        return customerVO;
    }

    class TestCharacteristic {
        public final long valuation;
        public final String unit;

        TestCharacteristic(long valuation, String unit) {
            this.valuation = valuation;
            this.unit = unit;
        }
    }

}