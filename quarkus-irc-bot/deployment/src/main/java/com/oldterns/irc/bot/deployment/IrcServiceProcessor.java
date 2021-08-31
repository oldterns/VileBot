package com.oldterns.irc.bot.deployment;

import java.util.Collection;
import java.util.HashSet;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.irc.bot.annotations.OnPrivateMessage;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import net.engio.mbassy.listener.Handler;
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
            BuildProducer<GeneratedBeanBuildItem> generatedBeanConsumer) {
        DotName onChannelMessageDotName = DotName.createSimple(OnChannelMessage.class.getName());
        DotName onPrivateMessageDotName = DotName.createSimple(OnPrivateMessage.class.getName());
        DotName onMessageDotName = DotName.createSimple(OnMessage.class.getName());
        DotName handlerDotName = DotName.createSimple(Handler.class.getName());

        IndexView indexView = combinedIndex.getIndex();
        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedBeanConsumer);

        Collection<ClassInfo> classWithAnnotations = new HashSet<>();

        for (AnnotationInstance annotationInstance : indexView
                .getAnnotations(onChannelMessageDotName)) {
            classWithAnnotations.add(annotationInstance.target().asMethod().declaringClass());
        }

        for (AnnotationInstance annotationInstance : indexView
                .getAnnotations(onPrivateMessageDotName)) {
            classWithAnnotations.add(annotationInstance.target().asMethod().declaringClass());
        }

        for (AnnotationInstance annotationInstance : indexView
                .getAnnotations(onMessageDotName)) {
            classWithAnnotations.add(annotationInstance.target().asMethod().declaringClass());
        }

        for (AnnotationInstance annotationInstance : indexView
                .getAnnotations(handlerDotName)) {
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
    }
}
