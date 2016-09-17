package com.dabsquared.gitlabjenkins.scm;

import jenkins.scm.api.actions.ChangeRequestAction;

import java.net.URL;

/**
 * @author Robin Müller
 */
public class GitLabMergeRequestAction extends ChangeRequestAction {

    private final String id;
    private final URL url;
    private final String title;
    private final String author;

    public GitLabMergeRequestAction(String id, URL url, String title, String author) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.author = author;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getAuthor() {
        return author;
    }
}
