package com.oldterns.vilebot.services;

import com.oldterns.vilebot.database.UserlistDB;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import javax.inject.Inject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserListServiceTest {

    @Inject
    UserListService userListService;

    @InjectMock
    UserlistDB userlistDB;

    @Test
    public void testListsEnumerate() {
        when(userlistDB.getLists()).thenReturn(new LinkedHashSet<>(List.of("a", "b", "c")));
        assertThat(userListService.listsEnumerate()).isEqualTo("Available lists: a, b, c");
        when(userlistDB.getLists()).thenReturn(null);
        assertThat(userListService.listsEnumerate()).isEqualTo("There are no lists.");
        when(userlistDB.getLists()).thenReturn(Collections.emptySet());
        assertThat(userListService.listsEnumerate()).isEqualTo("There are no lists.");
    }

    @Test
    public void testListQuery() {
        when(userlistDB.getUsersIn("myList")).thenReturn(new LinkedHashSet<>(List.of("a", "b", "c")));
        User user = mock(User.class);
        userListService.listQuery( user, "myList");
        verify(user).sendMessage("The list myList contains: a, b, c");
        verifyNoMoreInteractions(user);

        reset(user);
        when(userlistDB.getUsersIn("myList")).thenReturn(null);
        userListService.listQuery( user, "myList");
        verify(user).sendMessage("The list myList does not exist or is empty.");
        verifyNoMoreInteractions(user);

        reset(user);
        when(userlistDB.getUsersIn("myList")).thenReturn(Collections.emptySet());
        userListService.listQuery( user, "myList");
        verify(user).sendMessage("The list myList does not exist or is empty.");
        verifyNoMoreInteractions(user);
    }

    @Test
    public void testListAdd() {
        List<String> nicks = List.of("a", "b", "c");
        assertThat(userListService.listAdd("myList", nicks))
            .isEqualTo("Added the following names to list myList: a, b, c");
        verify(userlistDB).addUsersTo("myList", nicks);
        verifyNoMoreInteractions(userlistDB);
    }

    @Test
    public void testListRemove() {
        List<String> nicks = List.of("a", "b", "c");
        assertThat(userListService.listRemove("myList", nicks))
                .isEqualTo("Removed the following names from list myList: a, b, c");
        verify(userlistDB).removeUsersFrom("myList", nicks);
        verifyNoMoreInteractions(userlistDB);
    }

    @Test
    public void testListExpansion() {
        when(userlistDB.getUsersIn("myList")).thenReturn(new LinkedHashSet<>(List.of("a", "b", "c")));
        User user = mock(User.class);
        when(user.getNick()).thenReturn("a");
        assertThat(userListService.listExpansion(user, "myList", "hey myList!"))
                .isEqualTo("b, c: hey myList!");

        when(userlistDB.getUsersIn("emptyList")).thenReturn(null);
        assertThat(userListService.listExpansion(user, "emptyList", "hey emptyList!"))
                .isNull();

        when(userlistDB.getUsersIn("emptyList")).thenReturn(Collections.emptySet());
        assertThat(userListService.listExpansion(user, "emptyList", "hey emptyList!"))
                .isNull();
    }
}
