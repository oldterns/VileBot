package com.oldterns.vilebot.deployment;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Delimiter;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.services.ChannelMessage;
import com.oldterns.vilebot.services.IRCService;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.gizmo.*;
import io.vertx.codegen.annotations.Nullable;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.irc.IrcMessage;
import org.apache.camel.model.*;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
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
    private ResultHandle ircServiceResultHandle;

    public IrcServiceImplementor(GeneratedBeanGizmoAdaptor classOutput, Class<?> ircServiceClass) {
        this.ircServiceClass = ircServiceClass;
        this.classOutput = classOutput;
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

        FieldDescriptor ircServiceField = FieldDescriptor.of(classCreator.getClassName(), "ircService", ircServiceClass);
        classCreator.getFieldCreator(ircServiceField)
                    .setModifiers(Modifier.PUBLIC)
                    .addAnnotation(Inject.class);

        methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "configure", void.class));
        ircServiceResultHandle = methodCreator.readInstanceField(ircServiceField, methodCreator.getThis());

        for (Method method : ircServiceClass.getDeclaredMethods()) {
            if (hasIrcServiceAnnotation(method) && !Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("IRC Service annotation detected on non-public method (" + method.toGenericString() + "). Maybe make the method public?" );
            }
            Optional.ofNullable(method.getAnnotation(OnChannelMessage.class)).ifPresent(annotation -> implementOnChannelMessage(method, annotation));
        }
        methodCreator.returnValue(null);
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
     *   Pattern pattern = Pattern.compile(...);
     *   from("direct:methodId")
     *       .pipeline()
     *           .filter(messageTextMatches(pattern.asPredicate()))
     *           .process(reply(ircMessage -> {
     *               Matcher matcher = pattern.matcher(ircMessage.getMessage());
     *               Integer intArg = Integer.parseInt(matcher.group("intArg"));
     *               String stringArg = matcher.group("stringArg");
     *               return service.theMethod(intArg, stringArg);
     *           }))
     *           .to(getChannel("#example"));
     * </pre>
     * @param method
     * @param onChannelMessage
     */
    private void implementOnChannelMessage(Method method, OnChannelMessage onChannelMessage) {
        ResultHandle fromChannelResultHandle = fromChannel(method);
        ResultHandle pipeLineResultHandle = pipeLine(fromChannelResultHandle);
        ResultHandle patternRegexResultHandle  = methodCreator.load(getPatternRegex(method, onChannelMessage.value()));
        ResultHandle compiledPatternResultHandle = methodCreator.invokeStaticMethod(MethodDescriptor.ofMethod(Pattern.class, "compile", Pattern.class, String.class),
                patternRegexResultHandle);
        ResultHandle compiledPatternPredicateResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Pattern.class, "asPredicate", java.util.function.Predicate.class),
                compiledPatternResultHandle);
        ResultHandle camelPredicate = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "messageTextMatches", Predicate.class, java.util.function.Predicate.class),
                methodCreator.getThis(), compiledPatternPredicateResultHandle);
        ResultHandle filterResultHandle = filter(pipeLineResultHandle, camelPredicate);

        FunctionCreator processorFunctionCreator = methodCreator.createFunction(getFunctionType(method));
        BytecodeCreator processorBytecode = processorFunctionCreator.getBytecode();

        ResultHandle ircMessageTextResultHandle = processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(ChannelMessage.class, "getMessage", String.class),
                processorBytecode.getMethodParam(0));

        ResultHandle patternMatcherResultHandle = processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(Pattern.class, "matcher", Matcher.class, CharSequence.class),
                compiledPatternResultHandle, ircMessageTextResultHandle);

        processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(Matcher.class, "matches", boolean.class),
                patternMatcherResultHandle);

        ResultHandle[] methodArgumentResultHandles = new ResultHandle[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            if (parameter.getType().isAssignableFrom(ChannelMessage.class)) {
                methodArgumentResultHandles[i] = processorBytecode.getMethodParam(0);
            } else {
                methodArgumentResultHandles[i] = extractParameterFromMatcher(processorBytecode, parameter, patternMatcherResultHandle);
            }
        }
        ResultHandle methodResult = processorBytecode.invokeVirtualMethod(MethodDescriptor.ofMethod(method),
                ircServiceResultHandle, methodArgumentResultHandles);
        processorBytecode.returnValue(getReturnValue(processorBytecode, method, methodResult));

        ResultHandle processorFunctionResultHandle = methodCreator.invokeVirtualMethod(getProcessorForMethod(method),
                methodCreator.getThis(), processorFunctionCreator.getInstance());
        ResultHandle processorResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ProcessorDefinition.class, "process", ProcessorDefinition.class,
                Processor.class), filterResultHandle, processorFunctionResultHandle);

        if (!method.getReturnType().isAssignableFrom(void.class)) {
            toChannel(processorResultHandle, onChannelMessage.channel());
        }
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
            return "\\w";
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

    private MethodDescriptor getProcessorForMethod(Method method) {
        if (method.getReturnType().isAssignableFrom(String.class)) {
            return MethodDescriptor.ofMethod(IRCService.class, "reply", Processor.class, Function.class);
        } else if (method.getReturnType().isAssignableFrom(void.class)) {
            return MethodDescriptor.ofMethod(IRCService.class, "handleMessage", Processor.class, Consumer.class);
        } else {
            throw new IllegalStateException("Illegal return type (" + method.getReturnType() + ") for method (" + method.getName() + ").");
        }
    }

    private Class<?> getFunctionType(Method method) {
        if (method.getReturnType().isAssignableFrom(String.class)) {
            return Function.class;
        } else if (method.getReturnType().isAssignableFrom(void.class)) {
            return Consumer.class;
        } else {
            throw new IllegalStateException("Illegal return type (" + method.getReturnType() + ") for method (" + method.getName() + ").");
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

    public static String getDirectNameForMethod(Method method) {
        return encodeForDirect(method.getDeclaringClass().getName() + "-" + method.getName() + "-" + Arrays.stream(method.getParameters())
                    .map(Parameter::getType).map(Class::getName)
                    .collect(Collectors.joining("-")));
    }

    public static String getDirectNameForMethod(MethodInfo method) {
        return encodeForDirect(method.declaringClass().name().toString() + "-" + method.name() + "-" + method.parameters().stream()
                .map(org.jboss.jandex.Type::name)
                .map(DotName::toString)
                .collect(Collectors.joining("-")));
    }

    private static String encodeForDirect(String name) {
        return "direct:" + name;
    }

    private ResultHandle fromChannel(Method method) {
        return methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(RouteBuilder.class, "from", RouteDefinition.class, String.class),
                methodCreator.getThis(), methodCreator.load(getDirectNameForMethod(method)));
    }

    private ResultHandle toChannel(ResultHandle processorResult, String channel) {
        ResultHandle theChannel;
        if ("<<default>>".equals(channel)) {
            theChannel = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getMainChannel", String.class),
                    methodCreator.getThis());
        } else {
            theChannel = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel",
                    String.class, String.class), methodCreator.getThis(), methodCreator.load(channel));
        }
        return methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ProcessorDefinition.class, "to", ProcessorDefinition.class, String.class),
                processorResult, theChannel);
    }

    private ResultHandle pipeLine(ResultHandle routeDefinitionResultHandle) {
        return methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(RouteDefinition.class, "pipeline", PipelineDefinition.class),
                routeDefinitionResultHandle);
    }

    private ResultHandle filter(ResultHandle pipelineDefinitionResultHandle, ResultHandle predicate) {
        return methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PipelineDefinition.class, "filter", FilterDefinition.class,
                Predicate.class),
                pipelineDefinitionResultHandle, predicate);
    }
}
