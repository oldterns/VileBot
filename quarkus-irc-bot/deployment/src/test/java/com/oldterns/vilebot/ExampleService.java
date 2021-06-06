package com.oldterns.vilebot;

import com.oldterns.vilebot.annotations.Delimiter;
import com.oldterns.vilebot.annotations.NoHelp;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExampleService {

    static ExampleService INSTANCE;
    ExampleService mock;

    public ExampleService() {
        INSTANCE = this;
    }

    public void setMock(ExampleService mock) {
        this.mock = mock;
    }

    @OnChannelMessage("!noargs")
    public void noArgs() {
        mock.noArgs();
    }

    // Test classes lose their parameter names :(
    @OnChannelMessage("!string @arg0")
    public void stringArg(String arg0) {
        mock.stringArg(arg0);
    }

    @OnChannelMessage("!int @arg0")
    public void intArg(Integer arg0) {
        mock.intArg(arg0);
    }

    @OnChannelMessage("!maybe ?@arg0")
    public void optionalIntArg(Optional<Integer> arg0) {
        mock.optionalIntArg(arg0);
    }

    @OnChannelMessage("!list @arg0")
    public void listArg(@Delimiter(",") List<Integer> arg0) {
        mock.listArg(arg0);
    }

    @OnChannelMessage("!client")
    public void clientArg(Client client) {
        mock.clientArg(client);
    }

    @OnChannelMessage("!user")
    public void userArg(User user) {
        mock.userArg(user);
    }

    @OnChannelMessage("!event")
    public void eventArg(ChannelMessageEvent event) {
        mock.eventArg(event);
    }

    @OnChannelMessage("!nick @arg0")
    public void nickArg(Nick arg0) {
        mock.nickArg(arg0);
    }

    @OnChannelMessage("!multiarg @arg0 @arg1")
    public void multiArg(Nick arg0, Integer arg1) {
        mock.multiArg(arg0, arg1);
    }

    @OnChannelMessage("!returnSomething")
    public String returnSomething() {
        mock.returnSomething();
        return "something";
    }

    @OnChannelMessage("!returnNewline")
    public String returnNewline() {
        mock.returnNewline();
        return "Message\nwith\nnewlines.";
    }

    @NoHelp
    @OnChannelMessage("!nohelp")
    public void noHelp() {
        mock.noHelp();
    }
}
