package com.oldterns.vilebot.services.admin;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AdminManagementServiceTest
{

    @Inject
    AdminManagementService adminManagementService;

    @InjectMock
    PasswordDB passwordDB;

    @InjectMock
    GroupDB groupDB;

    @InjectMock
    SessionService sessionService;

    @Test
    public void testBootstrapAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.noAdmins() ).thenReturn( true );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( false );
        when( passwordDB.setUserPassword( "alice", "password" ) ).thenReturn( true );

        assertThat( adminManagementService.setAdmin( user, Nick.valueOf( "alice" ),
                                                     "password" ) ).isEqualTo( "Added/modified admin alice" );

        verify( passwordDB ).setUserPassword( "alice", "password" );
        verify( groupDB ).addAdmin( "alice" );
    }

    @Test
    public void testChangeAdminPassword()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.noAdmins() ).thenReturn( false );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( passwordDB.setUserPassword( "bob", "password" ) ).thenReturn( false );

        assertThat( adminManagementService.setAdmin( user, Nick.valueOf( "bob" ),
                                                     "password" ) ).isEqualTo( "Added/modified admin bob" );

        verify( passwordDB ).setUserPassword( "bob", "password" );
        verify( groupDB, never() ).addAdmin( "bob" );
    }

    @Test
    public void testNonAdminCannotAddSelfIfThereAreAdmins()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.noAdmins() ).thenReturn( false );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( false );

        assertThat( adminManagementService.setAdmin( user, Nick.valueOf( "bob" ), "password" ) ).isNull();

        verifyNoInteractions( passwordDB );
        verify( groupDB, never() ).addAdmin( "bob" );
    }

    @Test
    public void testRemoveAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.remAdmin( "bob" ) ).thenReturn( true );

        assertThat( adminManagementService.removeAdmin( user,
                                                        Nick.valueOf( "bob" ) ) ).isEqualTo( "Removed admin bob" );

        verify( passwordDB ).removeUserPassword( "bob" );
        verify( groupDB ).remAdmin( "bob" );
    }

    @Test
    public void testRemoveNonAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.remAdmin( "alice" ) ).thenReturn( false );

        assertThat( adminManagementService.removeAdmin( user,
                                                        Nick.valueOf( "alice" ) ) ).isEqualTo( "Removed admin alice" );

        verifyNoInteractions( passwordDB );
        verify( groupDB ).remAdmin( "alice" );
    }

    @Test
    public void testNonAdminCannotRemoveAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( false );

        assertThat( adminManagementService.removeAdmin( user, Nick.valueOf( "alice" ) ) ).isNull();

        verifyNoInteractions( passwordDB );
        verify( groupDB, never() ).remAdmin( "alice" );
    }
}
