package com.oldterns.vilebot.services.admin;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AdminPingServiceTest
{

    @Inject
    AdminPingService adminPingService;

    @InjectMock
    SessionService sessionService;

    @InjectMock
    GroupDB groupDB;

    @Test
    public void testNoSessionPing()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        adminPingService.onAdminPing( user );

        verify( user ).sendMessage( "You do not have an active admin session" );
    }

    @Test
    public void testSessionPing()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        adminPingService.onAdminPing( user );

        verify( user ).sendMessage( "You do not have an active admin session" );
    }

}
