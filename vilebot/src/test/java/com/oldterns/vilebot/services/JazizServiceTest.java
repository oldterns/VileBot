package com.oldterns.vilebot.services;

import java.util.List;

import javax.inject.Inject;

import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import com.oldterns.vilebot.util.URLFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class JazizServiceTest
{

    @Inject
    JazizService jazizService;

    @InjectMock
    RandomProvider randomProvider;

    @Inject
    TestUrlStreamHandler urlMocker;

    @ConfigProperty( name = "vilebot.jaziz.thesaurus-key" )
    String API_KEY;

    private void setupURL( String word )
    {
        urlMocker.mockConnection( JazizService.API_URL + API_KEY + "/" + word + JazizService.API_FORMAT,
                                  JazizServiceTest.class.getResourceAsStream( "translations-" + word + ".json" ) );
    }

    @Test
    public void testJaziz()
    {
        setupURL( "hello" );
        setupURL( "cold" );
        setupURL( "myself" );
        setupURL( "running" );
        setupURL( "fast" );
        setupURL( "tower" );

        when( randomProvider.getRandomElement( List.of( "hey", "hola",
                                                        "hi" ) ) ).thenReturn( "hey" ).thenReturn( "hola" );

        when( randomProvider.getRandomElement( List.of( "building" ) ) ).thenReturn( "building" );
        when( randomProvider.getRandomElement( List.of( "chilly" ) ) ).thenReturn( "chilly" );
        when( randomProvider.getRandomElement( List.of( "me" ) ) ).thenReturn( "me" );
        when( randomProvider.getRandomElement( List.of( "speedily" ) ) ).thenReturn( "speedily" );
        when( randomProvider.getRandomElement( List.of( "sprinting" ) ) ).thenReturn( "sprinting" );

        assertThat( jazizService.jazizify( "hello tower, are you cold? myself is fast running ok. See you later." ) ).isEqualTo( "hey building, are you chilly? me is speedily sprinting ok. See you later." );
    }

}