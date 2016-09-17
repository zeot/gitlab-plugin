package com.dabsquared.gitlabjenkins.scm;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Robin Müller
 */
public class GitLabMergeRequestSCMHead extends GitLabSCMHead {

    private final GitLabMergeRequestAction gitLabMergeRequestAction;
    private final Integer iid;

    public GitLabMergeRequestSCMHead(String iid, URL url, String title, String author) {
        super("merge-requests/" + iid);
        this.iid = Integer.valueOf(iid);
        gitLabMergeRequestAction = new GitLabMergeRequestAction(iid, url, title, author);
    }

    public Integer getIid() {
        return iid;
    }

    @NonNull
    @Override
    public List<? extends Action> getAllActions() {
        List<Action> actions = new LinkedList<>(super.getAllActions());
        actions.add(gitLabMergeRequestAction);
        return actions;
    }
}
