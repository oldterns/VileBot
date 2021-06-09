package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class TtcServiceTest
{

    @Inject
    TtcService ttcService;

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @Test
    public void testPrintAlerts()
        throws MalformedURLException
    {
        urlStreamHandler.mockConnection( TtcService.TTC_URL,
                                         TtcServiceTest.class.getResourceAsStream( "ttc-example-alerts.html" ) );
        assertThat( ttcService.printAlerts() ).isEqualTo( "Line 1: There is no subway service between Sheppard-Yonge and Bloor-Yonge stations due to ATC signal system testing. Shuttle buses are running.\n"
            + "Line 1: Next Mon. through Thurs. nightly starting at midnight, customers will board northbound trains on southbound platforms at Pioneer Village and Highway 407 stations during single-track operation. Expect slightly longer wait times.\n"
            + "Line 1: Mon. through Thurs., subway service between Finch and Eglinton stations will end nightly at 11pm for tunnel remediation. Shuttle buses will run." );
    }
}
