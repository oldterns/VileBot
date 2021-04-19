package com.oldterns.vilebot.deployment;

import java.util.Collection;
import java.util.HashSet;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class IrcServiceProcessor {

    private static final String FEATURE = "irc-bot";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void createIrcServiceImplementations(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanConsumer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanConsumer) {
        DotName onChannelMessageDotName = DotName.createSimple(OnChannelMessage.class.getName());
        IndexView indexView = combinedIndex.getIndex();
        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedBeanConsumer);

        Collection<ClassInfo> classWithAnnotations = new HashSet<>();

        for (AnnotationInstance annotationInstance : indexView
                .getAnnotations(onChannelMessageDotName)) {
            classWithAnnotations.add(annotationInstance.target().asMethod().declaringClass());
        }

        for (ClassInfo classInfo : classWithAnnotations) {
            try {
                Class<?> ircServiceClass = Class.forName(classInfo.name().toString(), false, Thread.currentThread().getContextClassLoader());
                IrcServiceImplementor implementor = new IrcServiceImplementor(classOutput, ircServiceClass);
                implementor.generateImplementation();

            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to find class (" + classInfo.name() + ").", e);
            }
        }
        IrcListenerImplementor ircListenerImplementor = new IrcListenerImplementor(indexView, classOutput);
        ircListenerImplementor.generateImplementation();
    }
}
