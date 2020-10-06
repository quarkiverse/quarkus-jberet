package io.quarkiverse.jberet.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jberet", phase = ConfigPhase.BUILD_TIME)
public class JBeretBuildTimeConfig {

}
