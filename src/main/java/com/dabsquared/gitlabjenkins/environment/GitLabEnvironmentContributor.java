package com.dabsquared.gitlabjenkins.environment;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.scm.GitLabSCMRevision;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        GitLabWebHookCause cause = (GitLabWebHookCause) r.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            envs.overrideAll(cause.getData().getBuildVariables());
        } else {
            SCMRevisionAction action = r.getAction(SCMRevisionAction.class);
            if (action != null) {
                SCMRevision revision = action.getRevision();
                if (revision instanceof GitLabSCMRevision && ((GitLabSCMRevision) revision).getCauseData() != null) {
                    envs.overrideAll(((GitLabSCMRevision) revision).getCauseData().getBuildVariables());
                }
            }
        }
    }
}
