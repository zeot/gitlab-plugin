package com.dabsquared.gitlabjenkins.scm;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Robin Müller
 */
public class GitLabSCMSource extends AbstractGitSCMSource {

    private final static Logger LOGGER = Logger.getLogger(GitLabSCMSource.class.getName());

    private final String remote;
    private final String projectId;
    private final String projectUrl;
    private final String credentialsId;
    private final String includes;
    private final String excludes;
    private final String gitLabConnection;
    private final Object semaphore = new Object();

    private transient Map<String, BranchInfo> branches = new HashMap<>();
    private transient Map<Integer, BranchInfo> mergeRequests = new HashMap<>();
    private transient boolean manualRun = true;

    @DataBoundConstructor
    public GitLabSCMSource(String id,
                           String remote,
                           String credentialsId,
                           String includes,
                           String excludes,
                           String gitLabConnection) throws ProjectIdUtil.ProjectIdResolutionException {
        super(id);
        this.remote = remote;
        Project project = getProject(remote, gitLabConnection);
        this.projectId = project.getId().toString();
        this.projectUrl = project.getWebUrl();
        this.credentialsId = credentialsId;
        this.includes = includes;
        this.excludes = excludes;
        this.gitLabConnection = gitLabConnection;
    }

    @Override
    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public String getRemote() {
        return remote;
    }

    @Override
    public String getIncludes() {
        return includes;
    }

    @Override
    public String getExcludes() {
        return excludes;
    }

    public String getGitLabConnection() {
        return gitLabConnection;
    }

    public GitLabApi getClient() {
        return getClient(gitLabConnection);
    }

    public void updateBranch(String branchName, String commitId, long lastModified, CauseData causeData) {
        synchronized (semaphore) {
            if (!branches.isEmpty()) {
                try {
                    if (isExcluded(branchName)) {
                        return;
                    }
                    if (doesNotMetBranchCriteria(branchName, lastModified, new LogTaskListener(LOGGER, Level.FINE))) {
                        return;
                    }
                    SCMHead head = new GitLabSCMHead(branchName);
                    branches.put(branchName, new BranchInfo(head, new SCMRevisionImpl(head, commitId), causeData));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to add branch", e);
                }
            }
            manualRun = false;
        }
    }

    public void removeBranch(String branchName) {
        synchronized (semaphore) {
            branches.remove(branchName);
            manualRun = false;
        }
    }

    public void updateMergeRequest(Integer iid, String sourceBranch, String targetBranch, String commitId, long lastModified, String title,
                                   String author, CauseData causeData) {
        synchronized (semaphore) {
            if (!mergeRequests.isEmpty()) {
                try {
                    if (isExcluded(targetBranch)) {
                        return;
                    }
                    if (doesNotMetBranchCriteria(sourceBranch, lastModified, new LogTaskListener(LOGGER, Level.FINE))) {
                        return;
                    }
                    SCMHead head = new GitLabMergeRequestSCMHead(iid.toString(), new URL(projectUrl + "/" + iid), title, author);
                    mergeRequests.put(iid, new BranchInfo(head, new SCMRevisionImpl(head, commitId), causeData));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to add merge request", e);
                }
            }
            manualRun = false;
        }
    }

    public void removeMergeRequest(Integer id) {
        synchronized (semaphore) {
            mergeRequests.remove(id);
        }
    }

    protected GitLabSCMSource readResolve() {
        branches = new HashMap<>();
        mergeRequests = new HashMap<>();
        return this;
    }

    @Override
    protected List<RefSpec> getRefSpecs() {
        return Arrays.asList(new RefSpec("+refs/heads/*:refs/remotes/" + getRemoteName() + "/*"),
                             new RefSpec("+refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*"));
    }

    @Override
    protected SCMRevision retrieve(@NonNull SCMHead head, @NonNull TaskListener listener) throws IOException, InterruptedException {
        SCMRevision revision = super.retrieve(head, listener);
        synchronized (semaphore) {
            if (revision instanceof SCMRevisionImpl && head instanceof GitLabMergeRequestSCMHead && mergeRequests.containsKey(((GitLabMergeRequestSCMHead) head).getIid())) {
                BranchInfo branchInfo = mergeRequests.get(((GitLabMergeRequestSCMHead) head).getIid());
                if (revision.equals(branchInfo.getRevision())) {
                    return new GitLabSCMRevision(head, ((SCMRevisionImpl) revision).getHash(), branchInfo.getCauseData(), gitLabConnection);
                }
            }
        }
        return revision;
    }

    @Override
    protected void retrieve(@NonNull SCMHeadObserver observer, @NonNull TaskListener listener) throws IOException, InterruptedException {
        final GitLabApi client = getClient();
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
            return;
        }
        synchronized (semaphore) {
            if (manualRun) {
                branches.clear();
                mergeRequests.clear();
            }
            manualRun = true;
            try {
                listener.getLogger().println("Getting remote branches...");
                if (branches.isEmpty()) {
                    for (final Branch branch : client.getBranches(projectId)) {
                        final String branchName = branch.getName();
                        if (isExcluded(branchName)) {
                            continue;
                        }
                        long lastModified = branch.getCommit().getAuthoredDate().getTime();
                        if (doesNotMetBranchCriteria(branch.getName(), lastModified, listener)) {
                            continue;
                        }
                        SCMHead head = new GitLabSCMHead(branchName);
                        SCMRevisionImpl revision = new SCMRevisionImpl(head, branch.getCommit().getId());
                        branches.put(branchName, new BranchInfo(head, revision, null));
                        observer.observe(head, revision);
                    }
                } else {
                    for (BranchInfo branchInfo : branches.values()) {
                        observer.observe(branchInfo.getHead(), branchInfo.getRevision());
                    }
                }
                listener.getLogger().println("Getting merge requests...");
                if (mergeRequests.isEmpty()) {
                    for (final MergeRequest mergeRequest : getOpenMergeRequests(client, projectId)) {
                        if (isExcluded(mergeRequest.getTargetBranch())) {
                            continue;
                        }
                        Branch branch = client.getBranch(mergeRequest.getSourceProjectId().toString(), mergeRequest.getSourceBranch());
                        if (doesNotMetBranchCriteria(branch.getName(), branch.getCommit().getAuthoredDate().getTime(), listener)) {
                            continue;
                        }
                        SCMHead head = new GitLabMergeRequestSCMHead(mergeRequest.getIid().toString(),
                                                                     new URL(projectUrl + "/" + mergeRequest.getIid()),
                                                                     mergeRequest.getTitle(),
                                                                     mergeRequest.getAuthor().getName());
                        SCMRevisionImpl revision = new SCMRevisionImpl(head, branch.getCommit().getId());
                        mergeRequests.put(mergeRequest.getIid(), new BranchInfo(head, revision, null));
                        observer.observe(head, revision);
                    }
                } else {
                    for (BranchInfo branchInfo : mergeRequests.values()) {
                        observer.observe(branchInfo.getHead(), branchInfo.getRevision());
                    }
                }
            } catch (NotFoundException e) {
                listener.getLogger().println("Project not found on GitLab: " + gitLabConnection);
            } catch (WebApplicationException | ProcessingException e) {
                listener.getLogger().println("Failure while retrieving the branches: " + e);
            }
        }
    }

    private boolean doesNotMetBranchCriteria(String branchName, long lastModified, @NonNull TaskListener listener) throws IOException {
        SCMSourceCriteria branchCriteria = getCriteria();
        if (branchCriteria != null) {
            SCMSourceCriteria.Probe probe = getProbe(branchName, lastModified);
            if (branchCriteria.isHead(probe, listener)) {
                listener.getLogger().println("Met criteria");
            } else {
                listener.getLogger().println("Does not meet criteria");
                return true;
            }
        }
        return false;
    }

    private SCMSourceCriteria.Probe getProbe(final String branchName, final long lastModified) {
        return new SCMSourceCriteria.Probe() {
            @Override
            public String name() {
                return branchName;
            }

            @Override
            public long lastModified() {
                return lastModified;
            }

            @Override
            public boolean exists(@NonNull String path) throws IOException {
                try {
                    getClient().getFile(projectId, path, branchName);
                    return true;
                } catch (NotFoundException e) {
                    return false;
                } catch (WebApplicationException | ProcessingException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    private List<MergeRequest> getOpenMergeRequests(GitLabApi client, String projectId) {
        List<MergeRequest> result = new ArrayList<>();
        Integer page = 1;
        do {
            List<MergeRequest> mergeRequests = client.getMergeRequests(projectId, State.opened, page, 100);
            result.addAll(mergeRequests);
            page = mergeRequests.isEmpty() ? null : page + 1;
        } while (page != null);
        return result;
    }

    private static GitLabApi getClient(String gitLabConnection) {
        if (StringUtils.isNotEmpty(gitLabConnection)) {
            GitLabConnectionConfig connectionConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            return connectionConfig != null ? connectionConfig.getClient(gitLabConnection) : null;
        }
        return null;
    }

    private static Project getProject(String remote, String gitLabConnection) throws ProjectIdUtil.ProjectIdResolutionException {
        GitLabApi client = getClient(gitLabConnection);
        if (client == null) {
            LOGGER.log(Level.SEVERE, "No GitLab connection configured");
            throw new RuntimeException("No GitLab connection configured");
        }
        try {
            return client.getProject(ProjectIdUtil.retrieveProjectId(remote));
        } catch (WebApplicationException | ProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve projectId for remote: " + remote, e);
            throw e;
        }
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.GitLabSCMSource_DisplayName();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                                                     @QueryParameter String remote) {
            if (context == null || !context.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM,
                                   Jenkins.getActiveInstance(),
                                   StandardCredentials.class,
                                   URIRequirementBuilder.fromUri(remote).build(),
                                   GitClient.CREDENTIALS_MATCHER);
        }

        public ListBoxModel doFillGitLabConnectionItems() {
            ListBoxModel options = new ListBoxModel();
            GitLabConnectionConfig descriptor = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            for (GitLabConnection connection : descriptor.getConnections()) {
                options.add(connection.getName(), connection.getName());
            }
            return options;
        }

        public FormValidation doCheckRemote(@QueryParameter String remote, @QueryParameter String gitLabConnection) {
            if (StringUtils.isEmpty(remote)) {
                return FormValidation.error(Messages.remote_required());
            }
            try {
                getProject(remote, gitLabConnection);
            } catch (NotFoundException e) {
                return FormValidation.error(Messages.remote_noProjectFound());
            } catch (Exception e) {
                return FormValidation.error(Messages.remote_resolveProjectException(e.getMessage()));
            }
            return FormValidation.ok();
        }
    }

    private static class BranchInfo {

        private final SCMHead head;
        private final SCMRevision revision;
        private final CauseData causeData;

        private BranchInfo(SCMHead head, SCMRevision revision, CauseData causeData) {
            this.head = head;
            this.revision = revision;
            this.causeData = causeData;
        }

        public SCMHead getHead() {
            return head;
        }

        public SCMRevision getRevision() {
            return revision;
        }

        public CauseData getCauseData() {
            return causeData;
        }
    }
}
