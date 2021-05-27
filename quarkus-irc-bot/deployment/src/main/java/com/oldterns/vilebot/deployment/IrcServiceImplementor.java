package com.oldterns.vilebot.deployment;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Delimiter;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.services.IRCService;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.gizmo.*;
import io.vertx.codegen.annotations.Nullable;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Invoke;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IrcServiceImplementor {
    private final Class<?> ircServiceClass;
    private final GeneratedBeanGizmoAdaptor classOutput;
    private ClassCreator classCreator;
    private MethodCreator methodCreator;
    private FieldDescriptor ircServiceField;
    private Set<String> channelSet;

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
        classCreator.getFieldCreator(ircServiceField)
                    .setModifiers(Modifier.PUBLIC)
                    .addAnnotation(Inject.class);

        for (Method method : ircServiceClass.getDeclaredMethods()) {
            if (hasIrcServiceAnnotation(method) && !Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("IRC Service annotation detected on non-public method (" + method.toGenericString() + "). Maybe make the method public?" );
            }
            Optional.ofNullable(method.getAnnotation(OnChannelMessage.class)).ifPresent(annotation -> implementOnChannelMessage(method, annotation));
        }

        methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(IRCService.class, "getChannelsToJoin",
                Collection.class));
        ResultHandle outputArray = methodCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class),
                methodCreator.load(channelSet.size()));
        channelSet.forEach(channel -> {
            ResultHandle channelResultHandle = methodCreator.load(channel);
            ResultHandle actualChannel = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel", String.class, String.class),
                    methodCreator.getThis(), channelResultHandle);
            methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "add", boolean.class, Object.class),
                    outputArray, actualChannel);
        });
        methodCreator.returnValue(outputArray);

        classCreator.close();
        return generatedClassName;
    }

    private boolean hasIrcServiceAnnotation(Method method) {
        return method.isAnnotationPresent(OnChannelMessage.class);
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
    private void implementOnChannelMessage(Method method, OnChannelMessage onChannelMessage) {
        channelSet.add(onChannelMessage.channel());
        methodCreator = classCreator.getMethodCreator(getSafeMethodSignature(method), void.class, ChannelMessageEvent.class);
        AnnotationCreator annotationCreator = methodCreator.addAnnotation(Handler.class);
        annotationCreator.addValue("delivery", Invoke.Asynchronously);
        ResultHandle channelResultHandle = methodCreator.load(onChannelMessage.channel());
        ResultHandle actualChannelToCheckResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel", String.class, String.class),
                methodCreator.getThis(), channelResultHandle);
        ResultHandle channelMessageEventResultHandle = methodCreator.getMethodParam(0);

        ResultHandle channelInstanceResultHandle = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(ChannelEvent.class,
                "getChannel", Channel.class), channelMessageEventResultHandle);
        ResultHandle channelMessagingResultHandle = methodCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Channel.class,
                "getMessagingName", String.class), channelInstanceResultHandle);
        ResultHandle isChannelTarget = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(String.class, "equals",
                boolean.class, Object.class), actualChannelToCheckResultHandle, channelMessagingResultHandle);

        BranchResult channelMatchesBranchResult = methodCreator.ifFalse(isChannelTarget);
        channelMatchesBranchResult.trueBranch().returnValue(null);
        BytecodeCreator matcherBytecodeCreator = channelMatchesBranchResult.falseBranch();
        ResultHandle patternRegexResultHandle  = matcherBytecodeCreator.load(getPatternRegex(method, onChannelMessage.value()));
        ResultHandle compiledPatternResultHandle = matcherBytecodeCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Pattern.class, "compile", Pattern.class, String.class),
                patternRegexResultHandle);
        ResultHandle ircMessageTextResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ChannelMessageEvent.class, "getMessage", String.class),
                channelMessageEventResultHandle);
        ResultHandle patternMatcherResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Pattern.class, "matcher", Matcher.class, CharSequence.class),
                compiledPatternResultHandle, ircMessageTextResultHandle);
        ResultHandle matchesResultHandle = matcherBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Matcher.class, "matches", boolean.class),
                patternMatcherResultHandle);
        BranchResult branchResult = matcherBytecodeCreator.ifFalse(matchesResultHandle);
        branchResult.trueBranch().returnValue(null);
        BytecodeCreator processorBytecode = branchResult.falseBranch();
        ResultHandle ircServiceResultHandle = processorBytecode.readInstanceField(ircServiceField, methodCreator.getThis());

        ResultHandle[] methodArgumentResultHandles = new ResultHandle[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            if (parameter.getType().isAssignableFrom(ChannelMessageEvent.class)) {
                methodArgumentResultHandles[i] = channelMessageEventResultHandle;
            } else if (parameter.getType().isAssignableFrom(Client.class)) {
                methodArgumentResultHandles[i] = processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getBot", Client.class),
                        processorBytecode.getThis());
            } else {
                methodArgumentResultHandles[i] = extractParameterFromMatcher(processorBytecode, parameter, patternMatcherResultHandle);
            }
        }
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
            replyLoopBytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ChannelMessageEvent.class, "sendReply", void.class, String.class),
                    channelMessageEventResultHandle, replyLoopBytecodeCreator.readArrayValue(splitResultHandle, indexResultHandle));
            replyLoopBytecodeCreator.assign(indexResultHandle, replyLoopBytecodeCreator.increment(indexResultHandle));
        }
        processorBytecode.returnValue(null);
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
        bytecodeCreator.assign(returnValue, bytecodeCreator.loadNull());
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
            }
        } else {
            throw new IllegalStateException("Illegal type (" + parameter.getType() + ") for parameter (" + parameter.getName() + ").");
        }
        return returnValue;
    }

    private MethodDescriptor getValueOfForType(Class<?> type) {
        if (type.isAssignableFrom(int.class)) {
            return MethodDescriptor.ofMethod(Integer.class, "parseInt", int.class, String.class);
        } else if (type.isAssignableFrom(long.class)) {
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

    private String getPatternRegex(Method method, String patternString) {
        String splitRegex = "@([a-zA-Z0-9$_]+)";
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
                        + ")" + ((methodParameter.isAnnotationPresent(Nullable.class))? "?" : ""));
            }
        }
        patternRegexBuilder.append("$");
        return patternRegexBuilder.toString();
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
            } else {
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
