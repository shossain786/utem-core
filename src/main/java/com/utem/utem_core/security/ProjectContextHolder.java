package com.utem.utem_core.security;

import com.utem.utem_core.entity.Project;

/**
 * ThreadLocal holder that carries the resolved {@link Project} from the
 * {@link ApiKeyAuthFilter} through to the service layer.
 */
public final class ProjectContextHolder {

    private static final ThreadLocal<Project> CONTEXT = new ThreadLocal<>();

    private ProjectContextHolder() {}

    public static void set(Project project) {
        CONTEXT.set(project);
    }

    public static Project get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
