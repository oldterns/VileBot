package com.oldterns.vilebot.services.admin;

import java.time.Duration;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.database.PasswordDB;
import com.oldterns.vilebot.util.SessionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AuthServiceTest
{

    @Inject
    AuthService authService;

    @InjectMock
    SessionService sessionService;

    @InjectMock
    GroupDB groupDB;

    @InjectMock
    PasswordDB passwordDB;

    @Test
    public void testCannotLoginAsANonAdmin()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( groupDB.isAdmin( "alice" ) ).thenReturn( false );
        when( passwordDB.isValidPassword( "alice", "password" ) ).thenReturn( false );

        assertThat( authService.login( user, Nick.valueOf( "alice" ),
                                       "password" ) ).isEqualTo( "Authentication failed" );

        verifyNoInteractions( sessionService );
    }

    @Test
    public void testCannotLoginWrongPassword()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( groupDB.isAdmin( "alice" ) ).thenReturn( true );
        when( passwordDB.isValidPassword( "alice", "password" ) ).thenReturn( false );

        assertThat( authService.login( user, Nick.valueOf( "alice" ),
                                       "password" ) ).isEqualTo( "Authentication failed" );

        verifyNoInteractions( sessionService );
    }

    @Test
    public void testLoginCorrectPassword()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( groupDB.isAdmin( "alice" ) ).thenReturn( true );
        when( passwordDB.isValidPassword( "alice", "password" ) ).thenReturn( true );

        assertThat( authService.login( user, Nick.valueOf( "alice" ),
                                       "password" ) ).isEqualTo( "Authentication successful. Session active for 5 minutes." );

        verify( sessionService ).addSession( Nick.valueOf( "bob" ), "alice", Duration.ofMinutes( 5 ) );
    }

}
