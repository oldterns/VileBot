package com.oldterns.irc.bot.test;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.services.ClientCreatorImpl;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.tuples.Functions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.internal.verification.VerificationModeFactory.only;

public class ServiceImplementationTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("vilebot.default.channel", "#channel")
            .overrideConfigKey("vilebot.default.nick", "Bot")
            .overrideConfigKey("vilebot.irc.server", "localhost")
            .overrideConfigKey("quarkus.arc.selected-alternatives", "com.oldterns.irc.bot.test.MockClientCreator")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                                                .addClasses(ExampleService.class,
                                                            MockClientCreator.class)
                                                .deleteClass(ClientCreatorImpl.class)
            );

    Functions.Function3<String, String, String, ChannelMessageEvent> fireMessage;
    BiFunction<String, String, PrivateMessageEvent> firePrivateMessage;

    @BeforeEach
    public void setup() {
        fireMessage = (nick, channelName, message) -> {
            User user = Mockito.mock(User.class);
            Mockito.when(user.getNick()).thenReturn(nick);
            Mockito.when(user.getClient()).thenReturn(MockClientCreator.client);

            Channel channel = Mockito.mock(Channel.class);
            Mockito.when(channel.getName()).thenReturn(channelName);
            Mockito.when(channel.getMessagingName()).thenReturn(channelName);
            Mockito.when(channel.getClient()).thenReturn(MockClientCreator.client);

            ServerMessage serverMessage = Mockito.mock(ServerMessage.class);

            ChannelMessageEvent event = new ChannelMessageEvent(MockClientCreator.client, serverMessage, user, channel, message);
            MockClientCreator.eventListeners.forEach(listener -> listener.accept(event));
            return event;
        };

        firePrivateMessage = (nick, message) -> {
            User user = Mockito.mock(User.class);
            Mockito.when(user.getNick()).thenReturn(nick);
            Mockito.when(user.getClient()).thenReturn(MockClientCreator.client);

            ServerMessage serverMessage = Mockito.mock(ServerMessage.class);

            PrivateMessageEvent event = new PrivateMessageEvent(MockClientCreator.client, serverMessage, user, nick, message);
            MockClientCreator.eventListeners.forEach(listener -> listener.accept(event));
            return event;
        };
    }

    @Test
    public void testHelpChannel() {
        ChannelMessageEvent event = fireMessage.apply("user", "#channel", "!help");
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(event.getActor(), Mockito.atLeastOnce()).sendMessage(captor.capture());

        String message = String.join("\n", captor.getAllValues());
        assertThat(message).contains("Available Commands: \n");
        assertThat(message).contains("Help: { !help }");
        assertThat(message).contains("Example: ");
        assertThat(message).contains("{ !noargs }");
        assertThat(message).contains("{ !string <arg0> }");
        assertThat(message).contains("{ !int <arg0:number> }");
        assertThat(message).contains("{ !maybe ?[<arg0:number>] }");
        assertThat(message).contains("{ !list <arg0:number>[,<arg02>...] }");
        assertThat(message).contains("{ !client }");
        assertThat(message).contains("{ !user }");
        assertThat(message).contains("{ !channel }");
        assertThat(message).contains("{ !event }");
        assertThat(message).contains("{ !nick <arg0:nick> }");
        assertThat(message).contains("{ !multiarg <arg0:nick> <arg1:number> }");
        assertThat(message).contains("{ !returnSomething }");
        assertThat(message).contains("{ !returnNewline }");

        assertThat(message).doesNotContain("{ !nohelp }");
    }

    @Test
    public void testHelpPrivate() {
        PrivateMessageEvent event = firePrivateMessage.apply("user", "!help");
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(event.getActor(), Mockito.atLeastOnce()).sendMessage(captor.capture());

        String message = String.join("\n", captor.getAllValues());
        assertThat(message).contains("Available Commands: \n");
        assertThat(message).contains("Help: { !help }");
        assertThat(message).contains("Example: ");
        assertThat(message).contains("{ !noargs }");
        assertThat(message).contains("{ !string <arg0> }");
        assertThat(message).contains("{ !int <arg0:number> }");
        assertThat(message).contains("{ !maybe ?[<arg0:number>] }");
        assertThat(message).contains("{ !list <arg0:number>[,<arg02>...] }");
        assertThat(message).contains("{ !client }");
        assertThat(message).contains("{ !user }");
        assertThat(message).contains("{ !event }");
        assertThat(message).contains("{ !nick <arg0:nick> }");
        assertThat(message).contains("{ !multiarg <arg0:nick> <arg1:number> }");
        assertThat(message).contains("{ !returnSomething }");
        assertThat(message).contains("{ !returnNewline }");

        assertThat(message).doesNotContain("{ !nohelp }");
    }

    @Test
    public void testCommandListeners() {
        ExampleService exampleService = Mockito.mock(ExampleService.class);
        ExampleService.INSTANCE.setMock(exampleService);
        fireMessage.apply("user", "#channel", "Do not trigger anything");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#notrigger", "!noargs");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!noargs");
        Mockito.verify(exampleService, only()).noArgs();
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!noargs 10");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!string hi all!");
        Mockito.verify(exampleService, only()).stringArg("hi all!");
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!int 20");
        Mockito.verify(exampleService, only()).intArg(20);
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!int hello");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!maybe");
        Mockito.verify(exampleService, only()).optionalIntArg(Optional.empty());
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!maybe 10");
        Mockito.verify(exampleService, only()).optionalIntArg(Optional.of(10));
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!maybe hello");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!list 1,2,3");
        Mockito.verify(exampleService, only()).listArg(List.of(1,2,3));
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!list 1,hello,3");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!client");
        Mockito.verify(exampleService, only()).clientArg(MockClientCreator.client);
        Mockito.reset(exampleService);

        ChannelMessageEvent event = fireMessage.apply("user", "#channel", "!user");
        Mockito.verify(exampleService, only()).userArg(event.getActor());
        Mockito.reset(exampleService);

        event = fireMessage.apply("user", "#channel", "!event");
        Mockito.verify(exampleService, only()).eventArg(event);
        Mockito.reset(exampleService);

        event = fireMessage.apply("user", "#channel", "!channel");
        Mockito.verify(exampleService, only()).channelArg(event.getChannel());
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!nick myNick");
        Mockito.verify(exampleService, only()).nickArg(Nick.valueOf("myNick"));
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!nick myNick anotherNick");
        Mockito.verifyNoInteractions(exampleService);

        fireMessage.apply("user", "#channel", "!multiarg myNick 10");
        Mockito.verify(exampleService, only()).multiArg(Nick.valueOf("myNick"), 10);
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!multiarg myNick");
        Mockito.verifyNoInteractions(exampleService);

        event = fireMessage.apply("user", "#channel", "!returnSomething");
        Mockito.verify(exampleService, only()).returnSomething();
        Mockito.verify(event.getChannel()).sendMessage("something");
        Mockito.reset(exampleService);

        event = fireMessage.apply("user", "#channel", "!returnNewline");
        Mockito.verify(exampleService, only()).returnNewline();
        Mockito.verify(event.getChannel()).sendMessage("Message");
        Mockito.verify(event.getChannel()).sendMessage("with");
        Mockito.verify(event.getChannel()).sendMessage("newlines.");
        Mockito.reset(exampleService);

        fireMessage.apply("user", "#channel", "!nohelp");
        Mockito.verify(exampleService, only()).noHelp();
        Mockito.reset(exampleService);
    }
}
