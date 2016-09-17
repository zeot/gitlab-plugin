package com.dabsquared.gitlabjenkins.scm;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.lang.StringUtils;

/**
 * @author Robin Müller
 */
public class GitLabSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {

    private final CauseData causeData;
    private final String gitLabConnection;

    public GitLabSCMRevision(SCMHead head, String hash, CauseData causeData, String gitLabConnection) {
        super(head, hash);
        this.causeData = causeData;
        this.gitLabConnection = gitLabConnection;
    }

    public CauseData getCauseData() {
        return causeData;
    }

    public GitLabApi getClient() {
        if (StringUtils.isNotEmpty(gitLabConnection)) {
            GitLabConnectionConfig connectionConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            return connectionConfig != null ? connectionConfig.getClient(gitLabConnection) : null;
        }
        return null;
    }

    public static GitLabApi getClient(Run<?, ?> build) {
        SCMRevisionAction action = build.getAction(SCMRevisionAction.class);
        if (action != null && action.getRevision() instanceof GitLabSCMRevision) {
            return ((GitLabSCMRevision) action.getRevision()).getClient();
        }
        return null;
    }
}
