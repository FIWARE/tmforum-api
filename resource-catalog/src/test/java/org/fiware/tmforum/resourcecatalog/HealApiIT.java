package org.fiware.tmforum.resourcecatalog;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.resourcecatalog.api.HealApi;
import org.fiware.resourcecatalog.api.HealApiTestClient;
import org.fiware.resourcecatalog.api.HealApiTestSpec;
import org.fiware.resourcecatalog.model.HealCreateVO;
import org.fiware.resourcecatalog.model.HealCreateVOTestExample;
import org.fiware.resourcecatalog.model.HealVO;
import org.fiware.resourcecatalog.model.HealVOTestExample;
import org.fiware.resourcecatalog.model.TaskStateTypeVO;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.resourcecatalog"})
public class HealApiIT extends AbstractApiIT implements HealApiTestSpec {

    public final HealApiTestClient healApiTestClient;

    private String message;
    private HealCreateVO healCreateVO;
    private HealVO expectedHealVO;

    @ParameterizedTest
    @MethodSource("provideValidHeals")
    public void createHeal201(String message, HealCreateVO healCreateVO, HealVO expectedHealVO) throws Exception {
        this.message = message;
        this.healCreateVO = healCreateVO;
        this.expectedHealVO = expectedHealVO;
        createHeal201();
    }


    @Override
    public void createHeal201() throws Exception {

        HttpResponse<HealVO> healVOHttpResponse = callAndCatch(() -> healApiTestClient.createHeal(healCreateVO));
        assertEquals(HttpStatus.CREATED, healVOHttpResponse.getStatus(), message);
        String healId = healVOHttpResponse.body().getId();

        expectedHealVO.id(healId).href(URI.create(healId));

        assertEquals(expectedHealVO, healVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidHeals() {
        List<Arguments> testEntries = new ArrayList<>();

        HealCreateVO healCreateVO = HealCreateVOTestExample.build().healPolicy(null).resourceFunction(null);
        HealVO expectedHealVO = HealVOTestExample.build().additionalParms(null);
        testEntries.add(Arguments.of("An empty heal should have been created.", healCreateVO, expectedHealVO));

        HealCreateVO actionCreateVO = HealCreateVOTestExample.build().healAction("make-it-healthy").healPolicy(null).resourceFunction(null);
        HealVO expectedActionVO = HealVOTestExample.build().healAction("make-it-healthy").additionalParms(null);
        testEntries.add(Arguments.of("A heal with an action should have been created.", actionCreateVO, expectedActionVO));

        HealCreateVO causeCreateVO = HealCreateVOTestExample.build().cause("its-unhealthy").healPolicy(null).resourceFunction(null);
        HealVO expectedCauseVO = HealVOTestExample.build().cause("its-unhealthy").additionalParms(null);
        testEntries.add(Arguments.of("A heal with a cause should have been created.", causeCreateVO, expectedCauseVO));

        HealCreateVO degreeCreateVO = HealCreateVOTestExample.build().degreeOfHealing("not-yet-healthy-again").healPolicy(null).resourceFunction(null);
        HealVO expectedDegreeVO = HealVOTestExample.build().degreeOfHealing("not-yet-healthy-again").additionalParms(null);
        testEntries.add(Arguments.of("A heal with a degree of healing should have been created.", degreeCreateVO, expectedDegreeVO));

        HealCreateVO nameCreateVO = HealCreateVOTestExample.build().name("my-name").healPolicy(null).resourceFunction(null);
        HealVO expectedNameVO = HealVOTestExample.build().name("my-name").additionalParms(null);
        testEntries.add(Arguments.of("A heal with a name should have been created.", nameCreateVO, expectedNameVO));

        HealCreateVO stateCreateVO = HealCreateVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).healPolicy(null).resourceFunction(null);
        HealVO expectedStateVO = HealVOTestExample.build().state(TaskStateTypeVO.INPROGRESS).additionalParms(null);
        testEntries.add(Arguments.of("A heal with a state should have been created.", stateCreateVO, expectedStateVO));

        HealCreateVO startTimeCreateVO = HealCreateVOTestExample.build().startTime("10-10-2022").healPolicy(null).resourceFunction(null);
        HealVO expectedStartTimeVO = HealVOTestExample.build().startTime("10-10-2022").additionalParms(null);
        testEntries.add(Arguments.of("A heal with a start time should have been created.", startTimeCreateVO, expectedStartTimeVO));
        
        return testEntries.stream();
    }

    @Override
    public void createHeal400() throws Exception {

    }

    @Override
    public void createHeal401() throws Exception {

    }

    @Override
    public void createHeal403() throws Exception {

    }

    @Override
    public void createHeal405() throws Exception {

    }

    @Override
    public void createHeal409() throws Exception {

    }

    @Override
    public void createHeal500() throws Exception {

    }

    @Override
    public void listHeal200() throws Exception {

    }

    @Override
    public void listHeal400() throws Exception {

    }

    @Override
    public void listHeal401() throws Exception {

    }

    @Override
    public void listHeal403() throws Exception {

    }

    @Override
    public void listHeal404() throws Exception {

    }

    @Override
    public void listHeal405() throws Exception {

    }

    @Override
    public void listHeal409() throws Exception {

    }

    @Override
    public void listHeal500() throws Exception {

    }

    @Override
    public void retrieveHeal200() throws Exception {

    }

    @Override
    public void retrieveHeal400() throws Exception {

    }

    @Override
    public void retrieveHeal401() throws Exception {

    }

    @Override
    public void retrieveHeal403() throws Exception {

    }

    @Override
    public void retrieveHeal404() throws Exception {

    }

    @Override
    public void retrieveHeal405() throws Exception {

    }

    @Override
    public void retrieveHeal409() throws Exception {

    }

    @Override
    public void retrieveHeal500() throws Exception {

    }
}
