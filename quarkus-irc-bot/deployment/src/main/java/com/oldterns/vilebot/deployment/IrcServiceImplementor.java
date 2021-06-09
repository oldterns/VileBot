package com.oldterns.vilebot.deployment;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Bot;
import com.oldterns.vilebot.annotations.Delimiter;
import com.oldterns.vilebot.annotations.NoHelp;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.annotations.OnPrivateMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.services.HelpService;
import com.oldterns.vilebot.services.IRCService;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.gizmo.*;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Invoke;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Actor;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.helper.PrivateEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IrcServiceImplementor {
    private final Class<?> ircServiceClass;
    private final GeneratedBeanGizmoAdaptor classOutput;
    private ClassCreator classCreator;
    private MethodCreator methodCreator;
    private MethodCreator postConstructMethodCreator;
    private FieldDescriptor ircServiceField;
    private FieldDescriptor helpServiceField;
    private Set<String> channelSet;
    private String helpCommandGroup;

    public IrcServiceImplementor(GeneratedBeanGizmoAdaptor classOutput, Class<?> ircServiceClass) {
        this.ircServiceClass = ircServiceClass;
        this.classOutput = classOutput;
        channelSet = new HashSet<>();
    }

    public String generateImplementation() {
        final String generatedClassName = ircServiceClass.getName() + "$VileBotImplementation";
        if (ircServiceClass.getAnnotation(ApplicationScoped.class) == null) {
            throw new IllegalStateException("IRC Service class (" + ircServiceClass + ") is not @ApplicationScoped. Maybe add @ApplicationScoped?");
        }

        classCreator = ClassCreator.builder()
                .className(ircServiceClass.getName() + "$VileBotImplementation")
                .classOutput(classOutput)
                .superClass(IRCService.class)
                .build();

        classCreator.addAnnotation(ApplicationScoped.class);

        ircServiceField = FieldDescriptor.of(classCreator.getClassName(), "ircService", ircServiceClass);
        helpServiceField = FieldDescriptor.of(classCreator.getClassName(), "helpService", HelpService.class);
        classCreator.getFieldCreator(ircServiceField)
                    .setModifiers(Modifier.PUBLIC)
                    .addAnnotation(Inject.class);

        classCreator.getFieldCreator(helpServiceField)
                .setModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class);


        helpCommandGroup = ircServiceClass.getSimpleName();
        if (helpCommandGroup.endsWith("Service")) {
            helpCommandGroup = helpCommandGroup.substring(0, helpCommandGroup.length() - "Service".length());
        }

        if (ircServiceClass.isAnnotationPresent(Bot.class)) {
            String botName = ircServiceClass.getAnnotation(Bot.class).value();
            MethodCreator botNickMethodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "botNick", String.class));
            botNickMethodCreator.returnValue(botNickMethodCreator.load(botName));
        }

        postConstructMethodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "__postConstruct", void.class));
        postConstructMethodCreator.addAnnotation(PostConstruct.class);
        for (Method method : ircServiceClass.getDeclaredMethods()) {
            if (hasIrcServiceAnnotation(method) && !Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("IRC Service annotation detected on non-public method (" + method.toGenericString() + "). Maybe make the method public?" );
            }
            Optional.ofNullable(method.getAnnotation(OnChannelMessage.class)).ifPresent(annotation -> implementOnChannelMessage(method, annotation, true));
            Optional.ofNullable(method.getAnnotation(OnPrivateMessage.class)).ifPresent(annotation -> implementOnPrivateMessage(method, annotation, true));
            Optional.ofNullable(method.getAnnotation(OnMessage.class)).ifPresent(annotation -> implementOnMessage(method, annotation));
        }

        methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(IRCService.class, "getChannelsToJoin",
                Collection.class));
        ResultHandle outputArray = methodCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class),
                methodCreator.load(channelSet.size()));
        channelSet.forEach(channel -> {
            ResultHandle channelResultHandle = methodCreator.load(channel);
            ResultHandle actualChannel = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel", Collection.class, String.class),
                    methodCreator.getThis(), channelResultHandle);
            methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "addAll", boolean.class, Collection.class),
                    outputArray, actualChannel);
        });
        methodCreator.returnValue(outputArray);

        postConstructMethodCreator.returnValue(null);
        classCreator.close();
        return generatedClassName;
    }

    private boolean hasIrcServiceAnnotation(Method method) {
        return method.isAnnotationPresent(OnChannelMessage.class)
                || method.isAnnotationPresent(OnPrivateMessage.class)
                || method.isAnnotationPresent(OnMessage.class);
    }

    /**
     * Generates code that looks like this:
     *
     * <pre>
     *   @Handler
     *   public void method$Handler(PrivateMessageEvent privateMessage) {
     *       if (!privateMessage.isToClient())) {
     *           return;
     *       }
     *       Pattern pattern = Pattern.compile(...);
     *       Matcher matcher = pattern.matcher(privateMessage.getMessage());
     *       if (!matcher.matches()) {
     *           return;
     *       }
     *       Integer intArg = Integer.parseInt(matcher.group("intArg"));
     *       String stringArg = matcher.group("stringArg");
     *       return service.theMethod(intArg, stringArg);
     *   }
     * </pre>
     * @param method
     * @param onMessage
     */
    private void implementOnMessage(Method method, OnMessage onMessage) {
        implementOnPrivateMessage(method, new OnPrivateMessage() {

            @Override public Class<? extends Annotation> annotationType() {
                return OnPrivateMessage.class;
            }

            @Override public String value() {
                return onMessage.value();
            }
        }, true);

        implementOnChannelMessage(method, new OnChannelMessage() {

            @Override public Class<? extends Annotation> annotationType() {
                return OnChannelMessage.class;
            }

            @Override public String value() {
                return onMessage.value();
            }

            @Override public String channel() {
                return "<<all>>";
            }
        }, false);
    }

    /**
     * Generates code that looks like this:
     *
     * <pre>
     *   @Handler
     *   public void method$Handler(PrivateMessageEvent privateMessage) {
     *       if (!privateMessage.isToClient())) {
     *           return;
     *       }
     *       Pattern pattern = Pattern.compile(...);
     *       Matcher matcher = pattern.matcher(privateMessage.getMessage());
     *       if (!matcher.matches()) {
     *           return;
     *       }
     *       Integer intArg = Integer.parseInt(matcher.group("intArg"));
     *       String stringArg = matcher.group("stringArg");
     *       return service.theMethod(intArg, stringArg);
     *   }
     * </pre>
     * @param method
     * @param onPrivateMessage
     */
    private void implementOnPrivateMessage(Method method, OnPrivateMessage onPrivateMessage, boolean createHelp) {
        methodCreator = classCreator.getMethodCreator(getSafeMethodSignature(method) + "$$OnPrivateMessage", void.class, PrivateMessageEvent.class);
        AnnotationCreator annotationCreator = methodCreator.addAnnotation(Handler.class);
        annotationCreator.addValue("delivery", Invoke.Asynchronously);

        ResultHandle privateMessageEventResultHandle = methodCreator.getMethodParam(0);

        BytecodeCreator matcherBytecodeCreator = methodCreator;
        ResultHandle ircMessageTextResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrivateMessageEvent.class, "getMessage", String.class),
                privateMessageEventResultHandle);

        ImplementedQuery implementedQuery = implementQuery(method, matcherBytecodeCreator, onPrivateMessage.value(), createHelp, ircMessageTextResultHandle,
                (bytecode, parameterType) -> {
                    if (parameterType.isAssignableFrom(PrivateMessageEvent.class)) {
                        return Optional.of(privateMessageEventResultHandle);
                    } else if (parameterType.isAssignableFrom(Client.class)) {
                        return Optional.of(bytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getBot", Client.class),
                                bytecode.getThis()));
                    } else if (parameterType.isAssignableFrom(User.class)) {
                        return Optional.of(bytecode.invokeInterfaceMethod(MethodDescriptor.ofMethod(ActorEvent.class, "getActor", Actor.class),
                                privateMessageEventResultHandle));
                    }
                    return Optional.empty();
                });
        ResultHandle[] methodArgumentResultHandles = implementedQuery.methodArgumentResultHandles;
        BytecodeCreator processorBytecode = implementedQuery.processorBytecode;

        ResultHandle ircServiceResultHandle = processorBytecode.readInstanceField(ircServiceField, methodCreator.getThis());
        sendResponse(method, processorBytecode, ircServiceResultHandle, methodArgumentResultHandles, (responseByteCodeCreator, textResultHandle) -> {
            responseByteCodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrivateMessageEvent.class, "sendReply", void.class, String.class),
                    privateMessageEventResultHandle, textResultHandle);
        });
        processorBytecode.returnValue(null);
    }

    /**
     * Generates code that looks like this:
     *
     * <pre>
     *   @Handler
     *   public void method$Handler(ChannelMessage channelMessage) {
     *       if (!targetChannel.equals(channelMessage.getChannel().getMessagingName())) {
     *           return;
     *       }
     *       Pattern pattern = Pattern.compile(...);
     *       Matcher matcher = pattern.matcher(channelMessage.getMessage());
     *       if (!matcher.matches()) {
     *           return;
     *       }
     *       Integer intArg = Integer.parseInt(matcher.group("intArg"));
     *       String stringArg = matcher.group("stringArg");
     *       return service.theMethod(intArg, stringArg);
     *   }
     * </pre>
     * @param method
     * @param onChannelMessage
     */
    private void implementOnChannelMessage(Method method, OnChannelMessage onChannelMessage, boolean createHelp) {
        methodCreator = classCreator.getMethodCreator(getSafeMethodSignature(method)  + "$$OnChannelMessage", void.class, ChannelMessageEvent.class);
        AnnotationCreator annotationCreator = methodCreator.addAnnotation(Handler.class);
        annotationCreator.addValue("delivery", Invoke.Asynchronously);

        ResultHandle isChannelTarget;
        ResultHandle channelMessageEventResultHandle = methodCreator.getMethodParam(0);
        if (!onChannelMessage.channel().equals("<<all>>")) {
            channelSet.add(onChannelMessage.channel());
        ResultHandle channelResultHandle = methodCreator.load(onChannelMessage.channel());
        ResultHandle actualChannelToCheckResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel", Collection.class, String.class),
                methodCreator.getThis(), channelResultHandle);

        ResultHandle channelInstanceResultHandle = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(ChannelEvent.class,
                "getChannel", Channel.class), channelMessageEventResultHandle);
        ResultHandle channelMessagingResultHandle = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Channel.class,
                "getMessagingName", String.class), channelInstanceResultHandle);
        isChannelTarget = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "contains",
                boolean.class, Object.class), actualChannelToCheckResultHandle, channelMessagingResultHandle);
        } else {
            isChannelTarget = methodCreator.load(true);
        }

        BranchResult channelMatchesBranchResult = methodCreator.ifFalse(isChannelTarget);
        channelMatchesBranchResult.trueBranch().returnValue(null);
        BytecodeCreator matcherBytecodeCreator = channelMatchesBranchResult.falseBranch();
        ResultHandle ircMessageTextResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ChannelMessageEvent.class, "getMessage", String.class),
                channelMessageEventResultHandle);

        ImplementedQuery implementedQuery = implementQuery(method, matcherBytecodeCreator, onChannelMessage.value(), createHelp, ircMessageTextResultHandle,
                (bytecode, parameterType) -> {
                    if (parameterType.isAssignableFrom(ChannelMessageEvent.class)) {
                        return Optional.of(channelMessageEventResultHandle);
                    } else if (parameterType.isAssignableFrom(Client.class)) {
                        return Optional.of(bytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getBot", Client.class),
                                bytecode.getThis()));
                    } else if (parameterType.isAssignableFrom(User.class)) {
                        return Optional.of(bytecode.invokeInterfaceMethod(MethodDescriptor.ofMethod(ActorEvent.class, "getActor", Actor.class),
                                channelMessageEventResultHandle));
                    }
                    return Optional.empty();
                });
        ResultHandle[] methodArgumentResultHandles = implementedQuery.methodArgumentResultHandles;
        BytecodeCreator processorBytecode = implementedQuery.processorBytecode;

        ResultHandle ircServiceResultHandle = processorBytecode.readInstanceField(ircServiceField, methodCreator.getThis());
        sendResponse(method, processorBytecode, ircServiceResultHandle, methodArgumentResultHandles, (responseByteCodeCreator, textResultHandle) -> {
            responseByteCodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ChannelMessageEvent.class, "sendReply", void.class, String.class),
                                                        channelMessageEventResultHandle, textResultHandle);
        });
        processorBytecode.returnValue(null);
    }

    private ImplementedQuery implementQuery(Method method, BytecodeCreator matcherBytecodeCreator, String pattern, boolean createHelp, ResultHandle ircMessageTextResultHandle, BiFunction<BytecodeCreator, Class, Optional<ResultHandle>> additionalMatcherBiFunction) {

        FieldDescriptor patternField = classCreator.getFieldCreator(FieldDescriptor.of(classCreator.getClassName(), getSafeMethodSignature(method) + "$pattern", Pattern.class))
                .getFieldDescriptor();
        postConstructMethodCreator.writeInstanceField(patternField, postConstructMethodCreator.getThis(),
                postConstructMethodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Pattern.class, "compile", Pattern.class, String.class),
                        postConstructMethodCreator.load(getPatternRegex(method, pattern))));


        if (createHelp && !method.isAnnotationPresent(NoHelp.class)) {
            postConstructMethodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(HelpService.class, "addHelpCommand", void.class, String.class, String.class),
                    postConstructMethodCreator.readInstanceField(helpServiceField, postConstructMethodCreator.getThis()),
                    postConstructMethodCreator.load(helpCommandGroup),
                    postConstructMethodCreator.load(getHelpString(method, pattern)));
        }

        ResultHandle patternMatcherResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Pattern.class, "matcher", Matcher.class, CharSequence.class),
                matcherBytecodeCreator.readInstanceField(patternField, matcherBytecodeCreator.getThis()), ircMessageTextResultHandle);
        ResultHandle matchesResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Matcher.class, "matches", boolean.class),
                patternMatcherResultHandle);
        BranchResult branchResult = matcherBytecodeCreator.ifFalse(matchesResultHandle);
        branchResult.trueBranch().returnValue(null);
        BytecodeCreator processorBytecode = branchResult.falseBranch();


        ResultHandle[] methodArgumentResultHandles = new ResultHandle[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            Optional<ResultHandle> maybeResult = additionalMatcherBiFunction.apply(processorBytecode, parameter.getType());
            methodArgumentResultHandles[i] = maybeResult
                    .orElseGet(() -> extractParameterFromMatcher(processorBytecode, parameter, patternMatcherResultHandle));
        }
        return new ImplementedQuery(methodArgumentResultHandles, processorBytecode);
    }

    private void sendResponse(Method method, BytecodeCreator processorBytecode, ResultHandle ircServiceResultHandle, ResultHandle[] methodArgumentResultHandles, BiConsumer<BytecodeCreator, ResultHandle> responseSender) {
        ResultHandle methodResult = processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(method),
                ircServiceResultHandle, methodArgumentResultHandles);
        ResultHandle stringReturnValue = getReturnValue(processorBytecode, method, methodResult);

        if (!method.getReturnType().isAssignableFrom(void.class)) {
            BytecodeCreator replyBytecodeCreator = processorBytecode.ifNotNull(stringReturnValue).trueBranch();
            ResultHandle splitResultHandle = replyBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "split", String[].class,
                    String.class), stringReturnValue, replyBytecodeCreator.load("\n"));
            AssignableResultHandle indexResultHandle = replyBytecodeCreator.createVariable(int.class);
            replyBytecodeCreator.assign(indexResultHandle, replyBytecodeCreator.load(0));
            BytecodeCreator replyLoopBytecodeCreator = replyBytecodeCreator.whileLoop(conditionCreator ->
                    conditionCreator.ifIntegerLessThan(indexResultHandle, conditionCreator.arrayLength(splitResultHandle))
            ).block();
            responseSender.accept(replyLoopBytecodeCreator, replyLoopBytecodeCreator.readArrayValue(splitResultHandle, indexResultHandle));
            replyLoopBytecodeCreator.assign(indexResultHandle, replyLoopBytecodeCreator.increment(indexResultHandle));
        }
    }

    private static final class ImplementedQuery {
        final ResultHandle[] methodArgumentResultHandles;
        final BytecodeCreator processorBytecode;

        public ImplementedQuery(ResultHandle[] methodArgumentResultHandles, BytecodeCreator processorBytecode) {
            this.methodArgumentResultHandles = methodArgumentResultHandles;
            this.processorBytecode = processorBytecode;
        }
    }

    private String getSafeMethodSignature(Method method) {
        return method.getName() + "$$" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getName().replace('.', '$'))
                .collect(Collectors.joining("$$")) + "$$Handler";
    }

    private ResultHandle extractParameterFromMatcher(BytecodeCreator bytecodeCreator, Parameter parameter, ResultHandle matcherResultHandle) {
        ResultHandle parameterNameResultHandle = bytecodeCreator.load(parameter.getName());
        ResultHandle matcherText = bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Matcher.class, "group", String.class, String.class),
                matcherResultHandle, parameterNameResultHandle);

        AssignableResultHandle returnValue = bytecodeCreator.createVariable(parameter.getType());
        BranchResult ifNullBranchResult = bytecodeCreator.ifNull(matcherText);
        bytecodeCreator = ifNullBranchResult.trueBranch();
        if (parameter.getParameterizedType() instanceof ParameterizedType && Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) parameter.getParameterizedType()).getRawType())) {
            bytecodeCreator.assign(returnValue, bytecodeCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class)));
        } else {
            bytecodeCreator.assign(returnValue, bytecodeCreator.loadNull());
        }

        bytecodeCreator = ifNullBranchResult.falseBranch();

        if (parameter.getParameterizedType() instanceof Class) {
            MethodDescriptor valueParser = getValueOfForType(parameter.getType());
            bytecodeCreator.assign(returnValue, bytecodeCreator.invokeStaticMethod(valueParser, matcherText));
        } else if (parameter.getParameterizedType() instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
            if (List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                ResultHandle listDelimiterResultHandle = bytecodeCreator.load(getListDelimiter(parameter));
                ResultHandle itemsArrayResultHandle = bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "split", String[].class, String.class),
                        matcherText, listDelimiterResultHandle);
                Class<?> arrayType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                MethodDescriptor valueParser = getValueOfForType(arrayType);
                ResultHandle arrayLengthResultHandle = bytecodeCreator.arrayLength(itemsArrayResultHandle);
                ResultHandle parsedValueListResultHandle = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class),
                        arrayLengthResultHandle);
                AssignableResultHandle indexResultHandle = bytecodeCreator.createVariable(int.class);
                bytecodeCreator.assign(indexResultHandle, bytecodeCreator.load(0));
                WhileLoop whileLoop = bytecodeCreator.whileLoop(conditionFunction -> conditionFunction.ifIntegerLessThan(indexResultHandle, arrayLengthResultHandle));

                BytecodeCreator whileLoopBlock = whileLoop.block();
                ResultHandle parsedValueResultHandle = whileLoopBlock.invokeStaticMethod(valueParser, whileLoopBlock.readArrayValue(itemsArrayResultHandle, indexResultHandle));

                whileLoopBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                        parsedValueListResultHandle, parsedValueResultHandle);
                whileLoopBlock.assign(indexResultHandle, whileLoopBlock.increment(indexResultHandle));

                bytecodeCreator.assign(returnValue, parsedValueListResultHandle);
            } else if (Optional.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                BranchResult isEmptyBranchResult = bytecodeCreator.ifTrue(bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "isEmpty", boolean.class),
                        matcherText));
                bytecodeCreator = isEmptyBranchResult.trueBranch();
                bytecodeCreator.assign(returnValue, bytecodeCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class)));
                bytecodeCreator = isEmptyBranchResult.falseBranch();
                Class<?> optionalType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                MethodDescriptor valueParser = getValueOfForType(optionalType);
                bytecodeCreator.assign(returnValue, bytecodeCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Optional.class, "of",
                        Optional.class, Object.class), bytecodeCreator.invokeStaticMethod(valueParser, matcherText)));
            }
        } else {
            throw new IllegalStateException("Illegal type (" + parameter.getType() + ") for parameter (" + parameter.getName() + ").");
        }
        return returnValue;
    }

    private MethodDescriptor getValueOfForType(Class<?> type) {
        if (type.isAssignableFrom(int.class) || type.isAssignableFrom(Integer.class)) {
            return MethodDescriptor.ofMethod(Integer.class, "parseInt", int.class, String.class);
        } else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
            return MethodDescriptor.ofMethod(Long.class, "parseLong", long.class, String.class);
        } else if (type.isAssignableFrom(String.class)) {
            // pointless, but makes generated code more readable
            return MethodDescriptor.ofMethod(String.class, "valueOf", String.class, Object.class);
        } else if (type.isAssignableFrom(Nick.class)) {
            return MethodDescriptor.ofMethod(Nick.class, "valueOf", Nick.class, String.class);
        }else if (type.isEnum()) {
            return MethodDescriptor.ofMethod(type, "valueOf", type, String.class);
        } else {
            throw new IllegalArgumentException("Invalid type (" + type + ").");
        }
    }

    private String getPatternRegex(Method method, String parameterPatternString) {
        String splitRegex = "@([a-zA-Z0-9$_]+)";
        // add a space to the beginning to guarantee first token is not
        // a parameter
        String patternString = (" " + parameterPatternString);
        String[] patternStringParts = patternString.split(splitRegex);
        String remaining = patternString;

        StringBuilder patternRegexBuilder = new StringBuilder("^");
        for (String part : patternStringParts) {
            patternRegexBuilder.append(part);
            remaining = remaining.substring(part.length());
            if (!remaining.isEmpty()) {
                int endIndex = 1;
                while (endIndex < remaining.length() && Character.isJavaIdentifierPart(remaining.charAt(endIndex))) {
                    endIndex++;
                }
                String variableName = remaining.substring(1, endIndex);
                remaining = remaining.substring(variableName.length() + 1);

                Parameter methodParameter = Arrays.stream(method.getParameters()).filter(parameter -> parameter.getName().equals(variableName))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Expected a parameter with name (" +
                        variableName + ") on method (" + method.getName()
                        + ") for pattern (" + patternString + "), but no such parameter exists." ));

                patternRegexBuilder.append("(?<" + variableName + ">" +
                        getRegexForType(methodParameter, methodParameter.getParameterizedType())
                        + ")");
            }
        }
        patternRegexBuilder.append("$");
        // remove the additional space we added
        patternRegexBuilder.deleteCharAt(1);
        return patternRegexBuilder.toString();
    }

    private String getHelpString(Method method, String parameterPatternString) {
        String splitRegex = "@([a-zA-Z0-9$_]+)";
        // add a space to the beginning to guarantee first token is not
        // a parameter
        String helpString = (" " + parameterPatternString);
        String[] helpStringParts = helpString.split(splitRegex);
        String remaining = helpString;

        StringBuilder helpBuilder = new StringBuilder();
        for (String part : helpStringParts) {
            helpBuilder.append(part);
            remaining = remaining.substring(part.length());
            if (!remaining.isEmpty()) {
                int endIndex = 1;
                while (endIndex < remaining.length() && Character.isJavaIdentifierPart(remaining.charAt(endIndex))) {
                    endIndex++;
                }
                String variableName = remaining.substring(1, endIndex);
                remaining = remaining.substring(variableName.length() + 1);

                Parameter methodParameter = Arrays.stream(method.getParameters()).filter(parameter -> parameter.getName().equals(variableName))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Expected a parameter with name (" +
                                variableName + ") on method (" + method.getName()
                                + ") for pattern (" + helpString + "), but no such parameter exists." ));

                helpBuilder.append(getHelpForType(methodParameter, methodParameter.getParameterizedType()));
            }
        }

        // remove the additional space we added
        helpBuilder.deleteCharAt(0);
        return "{ " + helpBuilder + " }";
    }

    private String getListDelimiter(Parameter parameter) {
        if (parameter.isAnnotationPresent(Delimiter.class)) {
            return parameter.getAnnotation(Delimiter.class).value();
        } else {
            return "\\s";
        }
    }

    private String getRegexForType(Parameter parameter, Type type) {
        if (type instanceof Class) {
            Class<?> parameterType = (Class<?>) type;
            if (int.class.isAssignableFrom(parameterType) ||
                    Integer.class.isAssignableFrom(parameterType) ||
                    long.class.isAssignableFrom(parameterType) ||
                    Long.class.isAssignableFrom(parameterType)
            ) {
                return "-?\\d+";
            } else if (String.class.isAssignableFrom(parameterType)) {
                if (parameter.isAnnotationPresent(Regex.class)) {
                    return parameter.getAnnotation(Regex.class).value();
                } else {
                    return ".+";
                }
            } else if (Nick.class.isAssignableFrom(parameterType)) {
                return "(?:x|)(?:[a-zA-Z0-9]+)(?:\\p{Punct}+[a-zA-Z0-9]*|)";
            } else if (parameterType.isEnum()) {
                return Arrays.stream(parameterType.getEnumConstants())
                        .map(Object::toString).collect(Collectors.joining("|"));
            } else {
                throw new IllegalArgumentException("Illegal type (" + type + ") on parameter (" + parameter + ")");
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (List.class.isAssignableFrom(rawType)) {
                String innerTypeRegex = "(?:" + getRegexForType(parameter, parameterizedType.getActualTypeArguments()[0]) + ")";
                String collectionDelimiterRegex = getListDelimiter(parameter);
                return innerTypeRegex + "(?:(?:" + collectionDelimiterRegex +")" + innerTypeRegex + ")*";
            } else if (Optional.class.isAssignableFrom(rawType)) {
                return "(?:" + getRegexForType(parameter, parameterizedType.getActualTypeArguments()[0]) + ")?";
            }else {
                throw new IllegalArgumentException("Illegal type (" + type + ") on parameter (" + parameter + ")");
            }
        } else {
            throw new IllegalArgumentException("Invalid type (" + type + ").");
        }
    }

    private String getHelpForType(Parameter parameter, Type type) {
        if (type instanceof Class) {
            Class<?> parameterType = (Class<?>) type;
            if (int.class.isAssignableFrom(parameterType) ||
                    Integer.class.isAssignableFrom(parameterType) ||
                    long.class.isAssignableFrom(parameterType) ||
                    Long.class.isAssignableFrom(parameterType)
            ) {
                return "<" + parameter.getName() + ":number>";
            } else if (String.class.isAssignableFrom(parameterType)) {
                if (parameter.isAnnotationPresent(Regex.class)) {
                    return "<" + parameter.getName() + ":" + parameter.getAnnotation(Regex.class).value() + ">";
                } else {
                    return "<" + parameter.getName() + ">";
                }
            } else if (Nick.class.isAssignableFrom(parameterType)) {
                return "<" + parameter.getName() + ":nick>";
            } else if (parameterType.isEnum()) {
                return "<" + parameter.getName() + ":" + Arrays.stream(parameterType.getEnumConstants())
                        .map(Object::toString).collect(Collectors.joining("|")) + ">";
            } else {
                throw new IllegalArgumentException("Illegal type (" + type + ") on parameter (" + parameter + ")");
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (List.class.isAssignableFrom(rawType)) {
                String innerHelp = getHelpForType(parameter, parameterizedType.getActualTypeArguments()[0]);
                return innerHelp + "[" + getListDelimiter(parameter) + "<" + parameter.getName() + "2>...]";
            } else if (Optional.class.isAssignableFrom(rawType)) {
                return "[" + getHelpForType(parameter, parameterizedType.getActualTypeArguments()[0]) + "]";
            }else {
                throw new IllegalArgumentException("Illegal type (" + type + ") on parameter (" + parameter + ")");
            }
        } else {
            throw new IllegalArgumentException("Invalid type (" + type + ").");
        }
    }

    private ResultHandle getReturnValue(BytecodeCreator bytecodeCreator, Method method, ResultHandle methodReturnValue) {
        if (method.getReturnType().isAssignableFrom(void.class)) {
            return null;
        } else if (method.getReturnType().isAssignableFrom(String.class)) {
            return methodReturnValue;
        } else {
            return bytecodeCreator.invokeStaticMethod(MethodDescriptor.ofMethod(String.class, "valueOf", String.class, Object.class), methodReturnValue);
        }
    }
}
