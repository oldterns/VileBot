package com.oldterns.vilebot.services;

import java.util.List;

import javax.inject.Inject;

import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KaomojiServiceTest
{

    @Inject
    KaomojiService kaomojiService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testKaomoji()
    {
        when( randomProvider.getRandomElement( eq( List.of( ":)", ":D" ) ) ) ).thenReturn( ":)" ).thenReturn( ":D" );

        assertThat( kaomojiService.getKaomoji( "happy" ) ).isEqualTo( ":)" );
        assertThat( kaomojiService.getKaomoji( "happy" ) ).isEqualTo( ":D" );
    }

    @Test
    public void testKaomojiMappingToMultipleCategories()
    {
        when( randomProvider.getRandomElement( List.of( "?" ) ) ).thenReturn( "?" );

        assertThat( kaomojiService.getKaomoji( "confused" ) ).isEqualTo( "?" );
        assertThat( kaomojiService.getKaomoji( "mystery" ) ).isEqualTo( "?" );
    }

    @Test
    public void testSpecialKaomoji()
    {
        when( randomProvider.getRandomElement( List.of( "?" ) ) ).thenReturn( "?" );

        assertThat( kaomojiService.getKaomoji( "wat" ) ).isEqualTo( "?" );
        assertThat( kaomojiService.getKaomoji( "nsfw" ) ).isEqualTo( "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)" );
        assertThat( kaomojiService.getKaomoji( "wtf" ) ).isEqualTo( "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)" );
        assertThat( kaomojiService.getKaomoji( "vilebot" ) ).isEqualTo( "( ͡° ͜ʖ ͡° )" );
    }

    @Test
    public void testMissingKaomoji()
    {
        assertThat( kaomojiService.getKaomoji( "404" ) ).isEqualTo( "щ(ಥдಥщ)" );
    }

}
