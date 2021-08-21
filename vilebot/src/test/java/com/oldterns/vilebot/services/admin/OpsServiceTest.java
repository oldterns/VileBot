package com.oldterns.vilebot.services.admin;

import java.util.List;

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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OpsServiceTest
{

    @Inject
    OpsService opsService;

    @InjectMock
    SessionService sessionService;

    @InjectMock
    GroupDB groupDB;

    @Test
    public void testNonAdminCannotAddOps()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.isAdmin( null ) ).thenReturn( false );
        assertThat( opsService.autoOpNicks( user, List.of( Nick.valueOf( "alice" ) ) ) ).isNull();

        verify( groupDB ).isAdmin( null );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanAddOps()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.addOp( "alice" ) ).thenReturn( true );
        assertThat( opsService.autoOpNicks( user,
                                            List.of( Nick.valueOf( "alice" ) ) ) ).isEqualTo( "Added alice to operator group\n" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).addOp( "alice" );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanAddOpsAlreadyOp()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.addOp( "alice" ) ).thenReturn( false );
        assertThat( opsService.autoOpNicks( user,
                                            List.of( Nick.valueOf( "alice" ) ) ) ).isEqualTo( "alice was/were already in the operator group\n" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).addOp( "alice" );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanAddOpsMulti()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.addOp( "alice" ) ).thenReturn( false );
        when( groupDB.addOp( "bob" ) ).thenReturn( true );
        assertThat( opsService.autoOpNicks( user, List.of( Nick.valueOf( "alice" ),
                                                           Nick.valueOf( "bob" ) ) ) ).isEqualTo( "Added bob to operator group\nalice was/were already in the operator group\n" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).addOp( "alice" );
        verify( groupDB ).addOp( "bob" );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testNonAdminCannotRemoveOps()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.isAdmin( null ) ).thenReturn( false );
        assertThat( opsService.removeAutoOpNicks( user, List.of( Nick.valueOf( "alice" ) ) ) ).isNull();

        verify( groupDB ).isAdmin( null );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanRemoveOps()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.remOp( "alice" ) ).thenReturn( true );
        assertThat( opsService.removeAutoOpNicks( user,
                                                  List.of( Nick.valueOf( "alice" ) ) ) ).isEqualTo( "Removed alice from operator group\n" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).remOp( "alice" );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanRemoveOpsOpDoesNotExist()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.remOp( "alice" ) ).thenReturn( false );
        assertThat( opsService.removeAutoOpNicks( user,
                                                  List.of( Nick.valueOf( "alice" ) ) ) ).isEqualTo( "alice was/were not in the operator group" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).remOp( "alice" );
        verifyNoMoreInteractions( groupDB );
    }

    @Test
    public void testAdminCanRemoveOpsMulti()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );
        when( groupDB.remOp( "alice" ) ).thenReturn( false );
        when( groupDB.remOp( "bob" ) ).thenReturn( true );
        assertThat( opsService.removeAutoOpNicks( user,
                                                  List.of( Nick.valueOf( "alice" ),
                                                           Nick.valueOf( "bob" ) ) ) ).isEqualTo( "Removed bob from operator group\nalice was/were not in the operator group" );

        verify( groupDB ).isAdmin( "bob" );
        verify( groupDB ).remOp( "alice" );
        verify( groupDB ).remOp( "bob" );
        verifyNoMoreInteractions( groupDB );
    }

}
