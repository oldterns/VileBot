package com.oldterns.vilebot.services;

import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import javax.inject.Inject;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KarmaRollServiceTest
{

    @Inject
    KarmaRollService karmaRollService;

    @InjectMock
    RandomProvider randomProvider;

    @InjectMock
    KarmaDB karmaDB;

    @Test
    public void testRoll()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "alice has rolled with "
            + KarmaRollService.UPPER_WAGER + " karma points on the line.  Who's up?" );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );
        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "You can't accept your own wager." );
        when( user.getNick() ).thenReturn( "bob|lunch" );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( 20 ) ) ).isEqualTo( "A game is already active; started by alice for "
                                               + KarmaRollService.UPPER_WAGER + " karma. Use !roll to accept." );
        verifyNoMoreInteractions( karmaDB );

        when( randomProvider.getRandomInt( 1, 6 ) ).thenReturn( 4 ).thenReturn( 2 );
        assertThat( karmaRollService.roll( user,
                                           Optional.empty() ) ).isEqualTo( "Results: alice rolled 4, and bob rolled 2. alice takes "
                                               + KarmaRollService.UPPER_WAGER + " from bob!!!\nPlay again?" );
        verify( karmaDB ).modNounKarma( "alice", KarmaRollService.UPPER_WAGER );
        verify( karmaDB ).modNounKarma( "bob", -KarmaRollService.UPPER_WAGER );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "alice has rolled with "
            + KarmaRollService.UPPER_WAGER + " karma points on the line.  Who's up?" );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );

        when( user.getNick() ).thenReturn( "bob|lunch" );
        when( randomProvider.getRandomInt( 1, 6 ) ).thenReturn( 3 ).thenReturn( 5 );
        assertThat( karmaRollService.roll( user,
                                           Optional.empty() ) ).isEqualTo( "Results: alice rolled 3, and bob rolled 5. bob takes "
                                               + KarmaRollService.UPPER_WAGER + " from alice!!!\nPlay again?" );
        verify( karmaDB ).modNounKarma( "alice", -KarmaRollService.UPPER_WAGER );
        verify( karmaDB ).modNounKarma( "bob", KarmaRollService.UPPER_WAGER );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testRollWithWager()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( 50L ) );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( 50 ) ) ).isEqualTo( "alice has rolled with 50 karma points on the line.  Who's up?" );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );

        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "You can't accept your own wager." );
        when( user.getNick() ).thenReturn( "bob|lunch" );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( 20 ) ) ).isEqualTo( "A game is already active; started by alice for 50 karma. Use !roll to accept." );
        verifyNoMoreInteractions( karmaDB );

        when( randomProvider.getRandomInt( 1, 6 ) ).thenReturn( 4 ).thenReturn( 2 );
        assertThat( karmaRollService.roll( user,
                                           Optional.empty() ) ).isEqualTo( "Results: alice rolled 4, and bob rolled 2. alice takes 50 from bob!!!\nPlay again?" );
        verify( karmaDB ).modNounKarma( "alice", 50 );
        verify( karmaDB ).modNounKarma( "bob", -50 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( -1L ) );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( 1 ) ) ).isEqualTo( "alice has rolled with 1 karma points on the line.  Who's up?" );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );

        when( user.getNick() ).thenReturn( "bob|lunch" );
        when( randomProvider.getRandomInt( 1, 6 ) ).thenReturn( 3 ).thenReturn( 5 );
        assertThat( karmaRollService.roll( user,
                                           Optional.empty() ) ).isEqualTo( "Results: alice rolled 3, and bob rolled 5. bob takes 1 from alice!!!\nPlay again?" );
        verify( karmaDB ).modNounKarma( "alice", -1 );
        verify( karmaDB ).modNounKarma( "bob", 1 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( -1L ) );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( 100 ) ) ).isEqualTo( "100 isn't a valid wager. Must be greater than 0. If your wager is larger than "
                                               + KarmaRollService.UPPER_WAGER
                                               + " you must have at least as much karma as your wager." );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( 100L ) );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.roll( user,
                                           Optional.of( -1 ) ) ).isEqualTo( "-1 isn't a valid wager. Must be greater than 0. If your wager is larger than "
                                               + KarmaRollService.UPPER_WAGER
                                               + " you must have at least as much karma as your wager." );
        verify( karmaDB ).getNounKarma( "alice" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testRollCancel()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "alice|wfh" );
        assertThat( karmaRollService.rollCancel( user ) ).isEqualTo( "No roll game is active." );
        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "alice has rolled with "
            + KarmaRollService.UPPER_WAGER + " karma points on the line.  Who's up?" );
        when( user.getNick() ).thenReturn( "bob" );
        assertThat( karmaRollService.rollCancel( user ) ).isEqualTo( "Only alice may cancel this game." );
        when( user.getNick() ).thenReturn( "alice|lunch" );
        assertThat( karmaRollService.rollCancel( user ) ).isEqualTo( "Roll game cancelled." );
        assertThat( karmaRollService.roll( user, Optional.empty() ) ).isEqualTo( "alice has rolled with "
            + KarmaRollService.UPPER_WAGER + " karma points on the line.  Who's up?" );
        // roll cancel to clear state for other tests
        karmaRollService.rollCancel( user );
    }
}
