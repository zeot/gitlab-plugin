package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.scm.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.eclipse.jgit.transport.URIish;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;
import static com.dabsquared.gitlabjenkins.util.WebHookConverter.convert;

/**
 * @author Robin Müller
 */
public class MergeRequestBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private Item project;
    private MergeRequestHook mergeRequestHook;

    public MergeRequestBuildAction(Item project, String json) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = JsonUtil.read(json, MergeRequestHook.class);
    }

    void processForCompatibility() {
        // url and homepage are introduced in 8.x versions of Gitlab
        final MergeRequestObjectAttributes attributes = this.mergeRequestHook.getObjectAttributes();
        if (attributes != null) {
            final Project source = attributes.getSource();
            if (source != null && source.getHttpUrl() != null) {
                if (source.getUrl() == null) {
                    source.setUrl(source.getHttpUrl());
                }
                if (source.getHomepage() == null) {
                    source.setHomepage(source.getHttpUrl().substring(0, source.getHttpUrl().lastIndexOf(".git")));
                }
            }
        }
    }

    public void execute() {
        if (project instanceof Job<?, ?>) {
            ACL.impersonate(ACL.SYSTEM, new Runnable() {
                public void run() {
                    GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob((Job<?, ?>) project);
                    if (trigger != null) {
                        trigger.onPost(mergeRequestHook);
                    }
                }
            });
        }
        if (project instanceof SCMSourceOwner) {
            ACL.impersonate(ACL.SYSTEM, new SCMSourceOwnerNotifier());
            throw HttpResponses.ok();
        }
        throw HttpResponses.ok();
    }

    private class SCMSourceOwnerNotifier implements Runnable {
        private static final String NO_COMMIT = "0000000000000000000000000000000000000000";

        public void run() {
            for (SCMSource scmSource : ((SCMSourceOwner) project).getSCMSources()) {
                if (scmSource instanceof GitLabSCMSource) {
                    GitLabSCMSource gitLabSCMSource = (GitLabSCMSource) scmSource;
                    try {
                        if (new URIish(gitLabSCMSource.getRemote()).equals(new URIish(gitLabSCMSource.getRemote()))) {
                            notifyGitLabSource(gitLabSCMSource);
                            LOGGER.log(Level.FINE, "Notify scmSourceOwner {0} about changes for {1}",
                                       toArray(project.getName(), gitLabSCMSource.getRemote()));
                            ((SCMSourceOwner) project).onSCMSourceUpdated(scmSource);
                        }
                    } catch (URISyntaxException e) {
                        // nothing to do
                    }
                }
            }
        }

        private void notifyGitLabSource(GitLabSCMSource gitLabSource) {
            MergeRequestObjectAttributes objectAttributes = mergeRequestHook.getObjectAttributes();
            State state = objectAttributes.getState();
            if (state == State.closed || state == State.merged) {
                gitLabSource.removeMergeRequest(objectAttributes.getId());
            } else {
                GitLabApi client = gitLabSource.getClient();
                String author = null;
                if (client != null && objectAttributes.getAuthorId() != null) {
                    try {
                        author = client.getUser(objectAttributes.getAuthorId()).getName();
                    } catch (WebApplicationException | ProcessingException e) {
                        LOGGER.log(Level.SEVERE, "Failed to retrieve author for merge request", e);
                    }
                }
                gitLabSource.updateMergeRequest(objectAttributes.getIid(),
                                                objectAttributes.getSourceBranch(),
                                                objectAttributes.getTargetBranch(),
                                                objectAttributes.getLastCommit().getId(), objectAttributes.getUpdatedAt().getTime(),
                                                objectAttributes.getTitle(),
                                                author,
                                                convert(mergeRequestHook));
            }
        }
    }
}
