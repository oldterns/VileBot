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
public class InspirationServiceTest
{

    @Inject
    InspirationService inspirationService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testInspiration()
    {
        when( randomProvider.getRandomElement( List.of( 0, 4 ) ) ).thenReturn( 4 ).thenReturn( 0 );
        assertThat( inspirationService.onInspiration() ).isEqualTo( "\"A little fire, Scarecrow?\"" );
        assertThat( inspirationService.onInspiration() ).isEqualTo( "\"... the educated person is not the person who can answer the questions, but\n"
            + "the person who can question the answers.\"\n"
            + "        ― Theodore Schick Jr., in The_Skeptical_Inquirer, March/April, 1997" );
    }

}
