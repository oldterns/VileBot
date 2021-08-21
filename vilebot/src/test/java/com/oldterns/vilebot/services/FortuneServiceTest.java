package com.oldterns.vilebot.services;

import java.util.List;

import javax.inject.Inject;

import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class FortuneServiceTest
{

    @Inject
    FortuneService fortuneService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testFortune()
    {
        when( randomProvider.getRandomElement( List.of( "Today it's up to you to create the peacefulness you long for",
                                                        "A friend asks only for your time not your money",
                                                        "If you refuse to accept anything but the best, you very often get it" ) ) ).thenReturn( "Today it's up to you to create the peacefulness you long for" ).thenReturn( "If you refuse to accept anything but the best, you very often get it" );
        assertThat( fortuneService.onFortune() ).isEqualTo( "Today it's up to you to create the peacefulness you long for" );
        assertThat( fortuneService.onFortune() ).isEqualTo( "If you refuse to accept anything but the best, you very often get it" );
    }

    @Test
    public void testDirtyFortune()
    {
        assertThat( fortuneService.onDirtyFortune() ).isEqualTo( "oooo you dirty" );
    }
}
