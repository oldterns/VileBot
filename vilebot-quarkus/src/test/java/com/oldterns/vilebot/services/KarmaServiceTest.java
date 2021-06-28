package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KarmaServiceTest
{

    @Inject
    KarmaService karmaService;

    @InjectMock
    KarmaDB karmaDB;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testTotal()
    {
        when( karmaDB.getTotalKarma() ).thenReturn( 2L );
        assertThat( karmaService.total() ).isEqualTo( "2" );
        when( karmaDB.getTotalKarma() ).thenReturn( -3L );
        assertThat( karmaService.total() ).isEqualTo( "-3" );
    }

    @Test
    public void testTopThree()
    {
        LinkedHashSet<String> topThreeNouns = new LinkedHashSet<>();
        topThreeNouns.add( "user1" );
        topThreeNouns.add( "alice" );
        topThreeNouns.add( "bob" );
        when( karmaDB.getRankNouns( 0L, 2L ) ).thenReturn( topThreeNouns );
        when( karmaDB.getNounRank( "user1" ) ).thenReturn( Optional.of( 0L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( 5L ) );

        when( karmaDB.getNounRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( 2L ) );

        when( karmaDB.getNounRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( -5L ) );

        assertThat( karmaService.getTopThree() ).isEqualTo( "1resu is ranked at #0 with 5 points of karma.\n"
            + "ecila is ranked at #1 with 2 points of karma.\n" + "bob is ranked at #2 with -5 points of karma." );
    }

    @Test
    public void testBottomThree()
    {
        LinkedHashSet<String> bottomThreeNouns = new LinkedHashSet<>();
        bottomThreeNouns.add( "user1" );
        bottomThreeNouns.add( "alice" );
        bottomThreeNouns.add( "bob" );
        when( karmaDB.getRevRankNouns( 0L, 2L ) ).thenReturn( bottomThreeNouns );
        when( karmaDB.getNounRevRank( "user1" ) ).thenReturn( Optional.of( 0L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( -5L ) );

        when( karmaDB.getNounRevRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( karmaDB.getNounKarma( "alice" ) ).thenReturn( Optional.of( -2L ) );

        when( karmaDB.getNounRevRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 5L ) );

        assertThat( karmaService.getBottomThree() ).isEqualTo( "1resu is ranked at (reverse) #0 with -5 points of karma.\n"
            + "ecila is ranked at (reverse) #1 with -2 points of karma.\n"
            + "bob is ranked at (reverse) #2 with 5 points of karma." );
    }

    @Test
    public void testRank()
    {
        when( karmaDB.getNounRank( "user1" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( 5L ) );

        assertThat( karmaService.rank( Nick.valueOf( "user1" ) ) ).isEqualTo( "user1 is ranked at #2 with 5 points of karma." );
        assertThat( karmaService.rank( Nick.valueOf( "user1|lunch" ) ) ).isEqualTo( "user1 is ranked at #2 with 5 points of karma." );
    }

    @Test
    public void testRevRank()
    {
        when( karmaDB.getNounRevRank( "user1" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( 5L ) );

        assertThat( karmaService.revrank( Nick.valueOf( "user1" ) ) ).isEqualTo( "user1 is ranked at (reverse) #2 with 5 points of karma." );
        assertThat( karmaService.revrank( Nick.valueOf( "user1|lunch" ) ) ).isEqualTo( "user1 is ranked at (reverse) #2 with 5 points of karma." );
    }

    @Test
    public void testSelfRank()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user1" );
        when( karmaDB.getNounRank( "user1" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( 5L ) );

        assertThat( karmaService.selfRank( user ) ).isEqualTo( "user1 is ranked at #2 with 5 points of karma." );

        when( user.getNick() ).thenReturn( "user1|lunch" );
        assertThat( karmaService.selfRank( user ) ).isEqualTo( "user1 is ranked at #2 with 5 points of karma." );
    }

    @Test
    public void testSelfRevRank()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user1" );
        when( karmaDB.getNounRevRank( "user1" ) ).thenReturn( Optional.of( 2L ) );
        when( karmaDB.getNounKarma( "user1" ) ).thenReturn( Optional.of( 5L ) );

        assertThat( karmaService.selfRevrank( user ) ).isEqualTo( "user1 is ranked at (reverse) #2 with 5 points of karma." );

        when( user.getNick() ).thenReturn( "user1|lunch" );
        assertThat( karmaService.selfRevrank( user ) ).isEqualTo( "user1 is ranked at (reverse) #2 with 5 points of karma." );
    }

    @Test
    public void testUserPlusPlus()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user1" );
        String out = karmaService.karmaIncOrDec( user, "user2++" );
        assertThat( out ).isNull();
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", 1 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        out = karmaService.karmaIncOrDec( user, "user1++" );
        assertThat( out ).isEqualTo( "I think I'm supposed to insult you now." );
        verifyNoInteractions( karmaDB );

        reset( karmaDB );
        out = karmaService.karmaIncOrDec( user, "user2++ some text user3++ user2++" );
        assertThat( out ).isNull();
        verify( karmaDB, Mockito.times( 2 ) ).modNounKarma( "user2", 1 );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user3", 1 );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testUserMinusMinus()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user1" );
        String out = karmaService.karmaIncOrDec( user, "user2--" );
        assertThat( out ).isNull();
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", -1 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        out = karmaService.karmaIncOrDec( user, "user1--" );
        assertThat( out ).isEqualTo( "I think I'm supposed to insult you now." );
        verifyNoInteractions( karmaDB );

        reset( karmaDB );
        out = karmaService.karmaIncOrDec( user, "user2-- some text user3-- user2--" );
        assertThat( out ).isNull();
        verify( karmaDB, Mockito.times( 2 ) ).modNounKarma( "user2", -1 );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user3", -1 );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testUserPlusMinus()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user1" );
        when( randomProvider.getRandomBoolean() ).thenReturn( false );
        String out = karmaService.karmaIncOrDec( user, "user2+-" );
        assertThat( out ).isEqualTo( "user2 had their karma decreased by 1" );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", -1 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        when( randomProvider.getRandomBoolean() ).thenReturn( true );
        out = karmaService.karmaIncOrDec( user, "user2+-" );
        assertThat( out ).isEqualTo( "user2 had their karma increased by 1" );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", 1 );
        verifyNoMoreInteractions( karmaDB );

        reset( karmaDB );
        out = karmaService.karmaIncOrDec( user, "user1+-" );
        assertThat( out ).isEqualTo( "I think I'm supposed to insult you now." );
        verifyNoInteractions( karmaDB );

        reset( karmaDB );
        when( randomProvider.getRandomBoolean() ).thenReturn( true ).thenReturn( false ).thenReturn( false );
        out = karmaService.karmaIncOrDec( user, "user2+- some text user3+- user2+-" );
        assertThat( out ).isEqualTo( "user2 had their karma increased by 1\n" + "user3 had their karma decreased by 1\n"
            + "user2 had their karma decreased by 1" );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", 1 );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user2", -1 );
        verify( karmaDB, Mockito.times( 1 ) ).modNounKarma( "user3", -1 );
        verifyNoMoreInteractions( karmaDB );
    }
}
