# VileBot

## System Requirements

- Podman or Docker
- Java 11 or higher

## Using VileBot

### Building

Before building VileBot, ensure quarkus-irc-bot is built. To build VileBot, run the following command:
```shell
./mvnw clean install
```

### Running

To run VileBot, run the following command:
```shell
./server-control.sh start
```

### Stopping

To stop VileBot, run the following command:
```shell
./server-control.sh stop
```

### Dev Mode

When developing VileBot, you might want
to try Dev Mode. First, start a test Redis database using the following command:
```shell
./startTestRedisInstance.sh
```
Next, run this command to launch Quarkus Dev Mode:
```shell
./mvnw quarkus:dev
```

## VileBot File Structure

### .env

This file hold API keys and VileBot configuration. This file should *NEVER* be committed to the repository.

### src/main/resources/application.properties

This file is an example configuration for VileBot1. Properties in `.env` will overwrite properties in `application.properties`.

### src/main/resources

Contains all data files that VileBot needs in order to work.

### db

Store the redis database.

### cfg

Store the redis config.

### log

Store VileBot and Redis logs.

### utils

Database migration utils (no migration is currently necessary, but might be useful if a future database migration occurs).

## VileBot Java Package Structure

### database

Store services that directly interact with Redis.

### services

Store services user interact with.

#### services.admin

Store services that require an active admin session to use.

### util

Store various utilities used in various services.

## Testing

Testing is done using JUnit 5, AssertJ and Mockito.

### Mocking URLs

Many VileBot services connect to URLs in order to function. To test these services, you need to mock the responses from the URLs. You can do this easily with the `TestUrlStreamHandler`:

```java
@QuarkusTest
public class MyServiceTest
{

    @Inject
    MyService myService;

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @Test
    public void testService()
        throws MalformedURLException
    {
        urlStreamHandler.mockConnection( "http://example.com",
                                         MyServiceTest.class.getResourceAsStream( "expected-response.html" ) );
        assertThat( myService.execute() ).isEqualTo( "Expected Content" );
    }
}
```

## A brief overview of quarkus-irc-bot

VileBot works via `quarkus-irc-bot`, which is a Quarkus extension contained in this repo. `quarkus-irc-bot` generate handlers for annotated methods. Annotations:

- `@OnMessage(template)`: Generate a handler that is triggered on any message (private/channel). It will be registered on all bots.
- `@OnChannelMessage(template, channel=${vilebot.default.channel})`: Generate a handler that is triggered on a message in a particular channel (defaults to the default channel). It will be registered on all bots in the channel.
- `@OnPrivateMessage(template)`: Generate a handler that is triggered on private message only. It will be registered to all bots.
- `@NoHelp`: Instructs `quarkus-irc-bot` to not add this service to the automatically generated `!help` command.

### Template format

A template is a regex with some additional features:

- A message is automatically trimmed before being parsed by the regex.
- The regex has an implicit starting `^` and ending `$` (i.e. it does not trigger if it occurs in the middle of a message).
- It allows parameters, which are in the format `@javaIdentifier`. It expects a parameter with exactly that name to be present in the annotated method, which will be passed the parsed value.
- Automatically passing optional parameters detailing who sent the message and where it was sent.

For example:

```java
import javax.enterprise.context.ApplicationScoped;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnMessage;

@ApplicationScoped
public class MyService {

    @OnMessage("!call @nick @amount")
    public String theMethod(User user, Nick nick, Integer amount) {
        return Nick.getNick(user).getBaseNick() + " has called " + nick + " for " + amount + " karma!";
    }
}
```

Creates a regex similar to `^!call (?<nick>[A-Za-z0-9]+) (?<amount>[0-9]+)$`. When `bob` sends the message "!call alice 100", `theMethod` is called with `(User(bob), Nick(alice), 100).

### Automatic parameters

- `org.kitteh.irc.client.library.element.User`: The user that sent the message. Has a `sendMessage(String)` method that can be used to private message the user.
- `org.kitteh.irc.client.library.element.Channel`: (`@OnChannelMessage` only) The channel the message was sent at. Can be used to send future replies.
- `org.kitteh.irc.client.library.event.channel.ChannelMessageEvent`:  (`@OnChannelMessage` only) the source event.