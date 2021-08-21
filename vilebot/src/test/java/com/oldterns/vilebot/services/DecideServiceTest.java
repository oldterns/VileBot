package com.oldterns.vilebot.services;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@QuarkusTest
public class DecideServiceTest
{

    @Inject
    DecideService decideService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testDecideOneOption()
    {
        when( randomProvider.getRandomElement( List.of( "a" ) ) ).thenReturn( "a" );
        assertThat( decideService.onDecision( Optional.empty(), Optional.empty(),
                                              List.of( "a" ) ) ).isEqualTo( "If you already know what you want to do, what are you bugging me for? You should a. Go do that now." );
    }

    @Test
    public void testDecideMultipleOptions()
    {
        when( randomProvider.getRandomElement( List.of( "a", "b", "c" ) ) ).thenReturn( "b" ).thenReturn( "a" );
        assertThat( decideService.onDecision( Optional.empty(), Optional.empty(),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think you should b. Go do that now." );
        assertThat( decideService.onDecision( Optional.empty(), Optional.empty(),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think you should a. Go do that now." );
    }

    @Test
    public void testDecideWithNoun()
    {
        when( randomProvider.getRandomElement( List.of( "a", "b", "c" ) ) ).thenReturn( "b" ).thenReturn( "a" );
        assertThat( decideService.onDecision( Optional.of( "[bob]" ), Optional.empty(),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think bob should b. Go do that now." );
        assertThat( decideService.onDecision( Optional.of( "[bob]" ), Optional.empty(),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think bob should a. Go do that now." );
    }

    @Test
    public void testDecideWithPrefix()
    {
        when( randomProvider.getRandomElement( List.of( "a", "b", "c" ) ) ).thenReturn( "b" ).thenReturn( "a" );
        assertThat( decideService.onDecision( Optional.empty(), Optional.of( "{surely do}" ),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think you should surely do b. Go do that now." );
        assertThat( decideService.onDecision( Optional.empty(), Optional.of( "{surely do}" ),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think you should surely do a. Go do that now." );
    }

    @Test
    public void testDecideWithNounAndPrefix()
    {
        when( randomProvider.getRandomElement( List.of( "a", "b", "c" ) ) ).thenReturn( "b" ).thenReturn( "a" );
        assertThat( decideService.onDecision( Optional.of( "[bob]" ), Optional.of( "{surely do}" ),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think bob should surely do b. Go do that now." );
        assertThat( decideService.onDecision( Optional.of( "[bob]" ), Optional.of( "{surely do}" ),
                                              List.of( "a", "b", "c" ) ) ).isEqualTo(
                                                                                      "I think bob should surely do a. Go do that now." );
    }

}
