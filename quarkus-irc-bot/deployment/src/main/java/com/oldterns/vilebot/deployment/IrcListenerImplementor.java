package com.oldterns.vilebot.deployment;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.services.IRCService;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

public class IrcListenerImplementor {
    private final IndexView indexView;
    private final GeneratedBeanGizmoAdaptor classOutput;

    private static final DotName onChannelMessageDotName = DotName.createSimple(OnChannelMessage.class.getName());

    public IrcListenerImplementor(IndexView indexView, GeneratedBeanGizmoAdaptor classOutput) {
        this.indexView = indexView;
        this.classOutput = classOutput;
    }

    public void generateImplementation() {
        ClassCreator classCreator = ClassCreator.builder()
                .className(IRCService.class.getName() + "$ListenerImplementation")
                .classOutput(classOutput)
                .superClass(IRCService.class)
                .build();

        classCreator.addAnnotation(ApplicationScoped.class);
        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(classCreator.getClassName(), "configure", void.class));

        Map<String, ResultHandle> listeningEndpointMap = getListeningEndpoints(methodCreator);

        listeningEndpointMap.forEach((channel, channelResultHandle) -> {
            ResultHandle fromResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(RouteBuilder.class, "from", RouteDefinition.class, String.class),
                    methodCreator.getThis(), channelResultHandle);
            ResultHandle multicastResultHandle = methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(RouteDefinition.class, "multicast", MulticastDefinition.class),
                    fromResultHandle);

            methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ProcessorDefinition.class, "to", ProcessorDefinition.class, String[].class),
                    multicastResultHandle, getTargetEndpoints(methodCreator, channel));
        });

        methodCreator.returnValue(null);

        classCreator.close();
    }

    private Map<String, ResultHandle> getListeningEndpoints(MethodCreator methodCreator) {
        Set<String> endpointStringValueSet = new HashSet<>();
        for (AnnotationInstance annotationInstance : indexView.getAnnotations(onChannelMessageDotName)) {
            endpointStringValueSet.add(annotationInstance.valueWithDefault(indexView, "channel").asString());
        }

        Map<String, ResultHandle> out = new HashMap<>(endpointStringValueSet.size());
        for (String endpointStringValue : endpointStringValueSet) {
            out.put(endpointStringValue, methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(IRCService.class, "getChannel", String.class, String.class),
                    methodCreator.getThis(), methodCreator.load(endpointStringValue)));
        }
        return out;
    }

    private ResultHandle getTargetEndpoints(MethodCreator methodCreator, String channelName) {
        List<ResultHandle> targetEndpointList = new ArrayList<>();
        for (AnnotationInstance annotationInstance : indexView.getAnnotations(onChannelMessageDotName)) {
            if ((annotationInstance.valueWithDefault(indexView, "channel").asString().equals(channelName))) {
                targetEndpointList.add(methodCreator.load(IrcServiceImplementor.getDirectNameForMethod(annotationInstance.target().asMethod())));
            }
        }
        ResultHandle out = methodCreator.newArray(String.class, targetEndpointList.size());
        for (int i = 0; i < targetEndpointList.size(); i++) {
            methodCreator.writeArrayValue(out, i, targetEndpointList.get(i));
        }
        return out;
    }
}
