package com.oldterns.vilebot.util;

import java.time.Duration;

import com.oldterns.irc.bot.Nick;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LimitServiceTest
{

    @Test
    public void testLimit()
    {
        LimitServiceImpl limitService = new LimitServiceImpl();
        TimeService timeService = mock( TimeService.class );
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "jucook" );
        limitService.timeService = timeService;
        limitService.setLimit( 10, Duration.ofMinutes( 5 ) );

        for ( int i = 0; i < 10; i++ )
        {
            assertThat( limitService.isAtLimit( user ) ).isFalse();
            assertThat( limitService.isAtLimit( Nick.valueOf( "jucook" ) ) ).isFalse();
            assertThat( limitService.isAtLimit( "jucook" ) ).isFalse();
            assertThatCode( () -> limitService.addUse( "jucook" ) ).doesNotThrowAnyException();
        }
        assertThat( limitService.isAtLimit( user ) ).isTrue();
        assertThat( limitService.isAtLimit( Nick.valueOf( "jucook" ) ) ).isTrue();
        assertThat( limitService.isAtLimit( "jucook" ) ).isTrue();
        assertThatCode( () -> limitService.addUse( "jucook" ) ).hasMessage( "jucook has the maximum uses" );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService, times( 10 ) ).onTimeout( eq( Duration.ofMinutes( 5 ) ), argumentCaptor.capture() );

        argumentCaptor.getValue().run();
        assertThat( limitService.isAtLimit( user ) ).isFalse();
        assertThat( limitService.isAtLimit( Nick.valueOf( "jucook" ) ) ).isFalse();
        assertThat( limitService.isAtLimit( "jucook" ) ).isFalse();
        assertThatCode( () -> limitService.addUse( "jucook" ) ).doesNotThrowAnyException();
        verify( timeService, times( 11 ) ).onTimeout( eq( Duration.ofMinutes( 5 ) ), any() );
        assertThat( limitService.isAtLimit( user ) ).isTrue();
        assertThat( limitService.isAtLimit( Nick.valueOf( "jucook" ) ) ).isTrue();
        assertThat( limitService.isAtLimit( "jucook" ) ).isTrue();
        assertThatCode( () -> limitService.addUse( "jucook" ) ).hasMessage( "jucook has the maximum uses" );
    }
}
