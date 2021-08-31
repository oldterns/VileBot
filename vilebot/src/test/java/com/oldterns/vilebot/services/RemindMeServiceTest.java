package com.oldterns.vilebot.services;

import java.time.Duration;
import java.time.LocalDateTime;

import javax.inject.Inject;

import com.oldterns.vilebot.util.TimeService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class RemindMeServiceTest
{

    @Inject
    RemindMeService remindMeService;

    @InjectMock
    TimeService timeService;

    @Test
    public void testReminder()
    {
        User user = Mockito.mock( User.class );
        Mockito.when( user.getNick() ).thenReturn( "bob" );
        Mockito.when( timeService.getCurrentDateTime() ).thenReturn( LocalDateTime.of( 2020, 12, 1, 0, 0 ) );

        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "s" ) ).isEqualTo( "Created reminder for 2020-12-01T00:00:10" );

        ArgumentCaptor<Runnable> reminderRunner = ArgumentCaptor.forClass( Runnable.class );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofSeconds( 10 ) ), reminderRunner.capture() );
        Mockito.verify( user ).getNick();
        Mockito.verifyNoMoreInteractions( user );
        reminderRunner.getValue().run();
        Mockito.verify( user ).sendMessage( "This is your reminder that you should: refactor VileBot" );
        Mockito.verifyNoMoreInteractions( user );
    }

    @Test
    public void testUnits()
    {
        User user = Mockito.mock( User.class );
        Mockito.when( user.getNick() ).thenReturn( "alice" );
        Mockito.when( timeService.getCurrentDateTime() ).thenReturn( LocalDateTime.of( 2020, 12, 1, 0, 0 ) );

        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "s" ) ).isEqualTo( "Created reminder for 2020-12-01T00:00:10" );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofSeconds( 10 ) ), Mockito.any() );
        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "m" ) ).isEqualTo( "Created reminder for 2020-12-01T00:10" );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofMinutes( 10 ) ), Mockito.any() );
        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "h" ) ).isEqualTo( "Created reminder for 2020-12-01T10:00" );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofHours( 10 ) ), Mockito.any() );
        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "d" ) ).isEqualTo( "Created reminder for 2020-12-11T00:00" );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofDays( 10 ) ), Mockito.any() );

    }

    @Test
    public void testLimit()
    {
        User user = Mockito.mock( User.class );
        Mockito.when( user.getNick() ).thenReturn( "jill" );
        Mockito.when( timeService.getCurrentDateTime() ).thenReturn( LocalDateTime.of( 2020, 12, 1, 0, 0 ) );

        for ( int i = 0; i < 10; i++ )
        {
            assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                                  "s" ) ).isEqualTo( "Created reminder for 2020-12-01T00:00:10" );
        }
        ;
        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "s" ) ).isEqualTo( "There is a limit of 10 reminders, please wait until one reminder ends to set a new one." );
        ArgumentCaptor<Runnable> reminderRunner = ArgumentCaptor.forClass( Runnable.class );
        Mockito.verify( timeService, Mockito.times( 10 ) ).onTimeout( Mockito.eq( Duration.ofSeconds( 10 ) ),
                                                                      reminderRunner.capture() );
        reminderRunner.getValue().run();
        assertThat( remindMeService.remindMe( user, "refactor VileBot", 10,
                                              "s" ) ).isEqualTo( "Created reminder for 2020-12-01T00:00:10" );
        Mockito.verify( timeService, Mockito.times( 11 ) ).onTimeout( Mockito.eq( Duration.ofSeconds( 10 ) ),
                                                                      reminderRunner.capture() );
    }
}
