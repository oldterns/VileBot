package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.Colors;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LastMessageSedServiceTest
{
    @Test
    public void testChangeLastMessageByUser()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        LastMessageSedService lastMessageSedService = new LastMessageSedService();
        assertThat( lastMessageSedService.onMessage( user, "oh no a typoo" ) ).isNull();
        assertThat( lastMessageSedService.onMessage( user, "s/typoo/typo" ) ).isEqualTo( "Correction: oh no a "
            + Colors.bold( "typo" ) );
    }

    @Test
    public void testCannotFindOriginalInLastMessageByUser()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        LastMessageSedService lastMessageSedService = new LastMessageSedService();
        assertThat( lastMessageSedService.onMessage( user, "oh no a typo" ) ).isNull();
        assertThat( lastMessageSedService.onMessage( user,
                                                     "s/typoo/typo" ) ).isEqualTo( "Wow. Seriously? Try subbing out a string that actually occurred. Do you even sed, bro?" );
    }

    @Test
    public void testFirstChangeOnly()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        LastMessageSedService lastMessageSedService = new LastMessageSedService();
        assertThat( lastMessageSedService.onMessage( user, "a dog dog" ) ).isNull();
        assertThat( lastMessageSedService.onMessage( user, "s/dog/cat" ) ).isEqualTo( "Correction: a "
            + Colors.bold( "cat" ) + " dog" );
    }

    @Test
    public void testChangeAllIfGlobal()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        LastMessageSedService lastMessageSedService = new LastMessageSedService();
        assertThat( lastMessageSedService.onMessage( user, "a dog dog" ) ).isNull();
        assertThat( lastMessageSedService.onMessage( user, "s/dog/cat/g" ) ).isEqualTo( "Correction: a "
            + Colors.bold( "cat" ) + " " + Colors.bold( "cat" ) );
    }

    @Test
    public void testSedDifferentUser()
    {
        User alice = mock( User.class );
        User bob = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getNick() ).thenReturn( "bob" );
        LastMessageSedService lastMessageSedService = new LastMessageSedService();
        assertThat( lastMessageSedService.onMessage( alice, "a dog dog" ) ).isNull();
        assertThat( lastMessageSedService.onMessage( bob, "s/dog/cat/ alice" ) ).isEqualTo( "alice, ftfy: a "
            + Colors.bold( "cat" ) + " dog" );
    }
}
