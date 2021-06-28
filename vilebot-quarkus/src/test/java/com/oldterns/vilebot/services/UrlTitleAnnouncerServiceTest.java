package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class UrlTitleAnnouncerServiceTest
{

    @Inject
    UrlTitleAnnouncerService urlTitleAnnouncerService;

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @Test
    public void testGetUrlTitle()
        throws IOException
    {
        urlStreamHandler.mockHttpConnection( "https://example.com/",
                                             UrlTitleAnnouncerServiceTest.class.getResourceAsStream( "example-webpage.html" ) );
        assertThat( urlTitleAnnouncerService.getUrlTitle( "https://example.com/" ) ).isEqualTo( "'Example Webpage'" );
    }
}
