package com.oldterns.vilebot.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class GetInfoOnServiceTest
{

    @Inject
    GetInfoOnService getInfoOnService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @Test
    public void testGetInfoOn()
    {
        urlMocker.mockConnection( "https://www.google.com/search?q=hot+dog+site%3Awikipedia.org",
                                  GetInfoOnServiceTest.class.getResourceAsStream( "google-results.html" ) );

        urlMocker.mockConnection( "https://en.wikipedia.org/wiki/Hot_dog",
                                  GetInfoOnServiceTest.class.getResourceAsStream( "wikipedia-hot-dog.html" ) );
        assertThat( getInfoOnService.getInfoOn( "hot dog" ) ).isEqualTo( "A hot dog (less commonly spelled hotdog) is a dish consisting of a grilled or steamed sausage served in the slit of a partially sliced bun. The term hot dog can also refer to the sausage itself. The sausage used is a wiener (Vienna sausage) or a frankfurter (Frankfurter Würstchen, also just called frank). The names of these sausages also commonly refer to their assembled dish. Hot dog preparation ..." );
    }

    @Test
    public void testGetFailedInfoOn()
    {
        assertThat( getInfoOnService.getInfoOn( "cat" ) ).isEqualTo( "Look, I don't know." );
    }

}
