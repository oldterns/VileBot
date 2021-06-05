package com.oldterns.vilebot;

import com.oldterns.vilebot.services.ClientCreator;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.feature.EventManager;
import org.mockito.Mockito;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ApplicationScoped
@Alternative
public class MockClientCreator implements ClientCreator {
    static Client client;
    static List<Consumer<ChannelMessageEvent>> eventListeners = new ArrayList<>();

    @Override
    public Client createClient(String nick) {
        if (client == null) {
            client = Mockito.mock(Client.class);
            EventManager eventManager = Mockito.mock(EventManager.class);
            Mockito.when(client.getName()).thenReturn(nick);
            Mockito.when(client.getEventManager()).thenReturn(eventManager);
            doAnswer(invocationOnMock -> {
                Object object = invocationOnMock.getArgument(0);
                Arrays.stream(object.getClass().getMethods())
                        .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0] == ChannelMessageEvent.class)
                        .forEach(method -> eventListeners.add(event -> {
                            try {
                                method.invoke(object, event);
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        }));

                return null;
            }).when(eventManager).registerEventListener(any());
        }
        return client;
    }
}
