package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlTitleAnnouncerServiceTest
{
    static TestUrlStreamHandler urlStreamHandler;

    @BeforeAll
    public static void beforeAll()
    {
        urlStreamHandler = TestUrlStreamHandler.setup();
    }

    @Test
    public void testGetUrlTitle()
        throws IOException
    {
        UrlTitleAnnouncerService urlTitleAnnouncerService = new UrlTitleAnnouncerService();
        urlStreamHandler.mockHttpConnection( new URL( "https://example.com/" ),
                                             UrlTitleAnnouncerServiceTest.class.getResourceAsStream( "example-webpage.html" ) );
        assertThat( urlTitleAnnouncerService.getUrlTitle( "https://example.com/" ) ).isEqualTo( "'Example Webpage'" );
    }
}
