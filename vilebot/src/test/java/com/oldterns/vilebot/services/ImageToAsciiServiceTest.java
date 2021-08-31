package com.oldterns.vilebot.services;

import javax.inject.Inject;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ImageToAsciiServiceTest
{

    @Inject
    ImageToAsciiService imageToAsciiService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @Test
    public void testConvert()
    {
        urlMocker.mockConnection( "https://example.com/image.png",
                                  ImageToAsciiServiceTest.class.getResourceAsStream( "test-image.png" ) );
        assertThat( imageToAsciiService.convert( "https://example.com/image.png" ) ).isEqualTo( "     #@@@@@@@@#                                   \n"
            + "  .#@@#&.    8@@&                                 \n"
            + "  @@@           @@*                               \n"
            + " 8@# .8#.   &8o o@@o                              \n"
            + ":@#   8#o    .   &@@                              \n"
            + ":@&              o@@                              \n"
            + " @#  #o      #8  #@@                              \n"
            + "  @@  o@@@@@@8  @@@o                              \n"
            + "  &@@         &@@@*                               \n"
            + "    o@@@#&&8#@@@o                                 \n"
            + "       8@@@@@8:                                   \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  \n"
            + "                                                  " );
    }

    @Test
    public void testFailedConvert()
    {
        assertThat( imageToAsciiService.convert( "https://example.com/404.png" ) ).isEqualTo( "Could not convert image." );
    }
}
