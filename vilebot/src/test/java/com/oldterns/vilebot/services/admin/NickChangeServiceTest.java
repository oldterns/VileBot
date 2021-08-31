package com.oldterns.vilebot.services.admin;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class NickChangeServiceTest
{

    @Inject
    NickChangeService nickChangeService;

    @InjectMock
    SessionService sessionService;

    @InjectMock
    GroupDB groupDB;

    @Test
    public void testNonAdminCannotChangeName()
    {
        Client client = mock( Client.class );
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        assertThat( nickChangeService.changeNick( client, user, Nick.valueOf( "alice" ) ) ).isNull();

        verifyNoInteractions( client );
    }

    @Test
    public void testAdminCanChangeNick()
    {
        Client client = mock( Client.class );
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        assertThat( nickChangeService.changeNick( client, user, Nick.valueOf( "alice" ) ) ).isEqualTo( "Nick changed" );

        verify( client ).setNick( "alice" );
    }

}
