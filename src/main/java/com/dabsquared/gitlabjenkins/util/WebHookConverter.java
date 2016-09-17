package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

/**
 * @author Robin Müller
 */
public class WebHookConverter {

    public static CauseData convert(PushHook hook) {
        String branch = hook.getRef() == null ? null : hook.getRef().replaceFirst("^refs/heads/", "");
        return causeData()
            .withActionType(CauseData.ActionType.PUSH)
            .withSourceProjectId(hook.getProjectId())
            .withTargetProjectId(hook.getProjectId())
            .withBranch(branch)
            .withSourceBranch(branch)
            .withUserName(hook.getUserName())
            .withUserEmail(hook.getUserEmail())
            .withSourceRepoHomepage(hook.getRepository().getHomepage())
            .withSourceRepoName(hook.getRepository().getName())
            .withSourceNamespace(hook.getProject().getNamespace())
            .withSourceRepoUrl(hook.getRepository().getUrl())
            .withSourceRepoSshUrl(hook.getRepository().getGitSshUrl())
            .withSourceRepoHttpUrl(hook.getRepository().getGitHttpUrl())
            .withMergeRequestTitle("")
            .withMergeRequestDescription("")
            .withMergeRequestId(null)
            .withMergeRequestIid(null)
            .withTargetBranch(branch)
            .withTargetRepoName("")
            .withTargetNamespace("")
            .withTargetRepoSshUrl("")
            .withTargetRepoHttpUrl("")
            .withTriggeredByUser(hook.getUserName())
            .withBefore(hook.getBefore())
            .withAfter(hook.getAfter())
            .withLastCommit(hook.getAfter())
            .withTargetProjectUrl(hook.getProject().getWebUrl())
            .build();
    }

    public static CauseData convert(MergeRequestHook hook) {
        return causeData()
            .withActionType(CauseData.ActionType.MERGE)
            .withSourceProjectId(hook.getObjectAttributes().getSourceProjectId())
            .withTargetProjectId(hook.getObjectAttributes().getTargetProjectId())
            .withBranch(hook.getObjectAttributes().getSourceBranch())
            .withSourceBranch(hook.getObjectAttributes().getSourceBranch())
            .withUserName(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
            .withUserEmail(hook.getObjectAttributes().getLastCommit().getAuthor().getEmail())
            .withSourceRepoHomepage(hook.getObjectAttributes().getSource().getHomepage())
            .withSourceRepoName(hook.getObjectAttributes().getSource().getName())
            .withSourceNamespace(hook.getObjectAttributes().getSource().getNamespace())
            .withSourceRepoUrl(hook.getObjectAttributes().getSource().getUrl())
            .withSourceRepoSshUrl(hook.getObjectAttributes().getSource().getSshUrl())
            .withSourceRepoHttpUrl(hook.getObjectAttributes().getSource().getHttpUrl())
            .withMergeRequestTitle(hook.getObjectAttributes().getTitle())
            .withMergeRequestDescription(hook.getObjectAttributes().getDescription())
            .withMergeRequestId(hook.getObjectAttributes().getId())
            .withMergeRequestIid(hook.getObjectAttributes().getIid())
            .withTargetBranch(hook.getObjectAttributes().getTargetBranch())
            .withTargetRepoName(hook.getObjectAttributes().getTarget().getName())
            .withTargetNamespace(hook.getObjectAttributes().getTarget().getNamespace())
            .withTargetRepoSshUrl(hook.getObjectAttributes().getTarget().getSshUrl())
            .withTargetRepoHttpUrl(hook.getObjectAttributes().getTarget().getHttpUrl())
            .withTriggeredByUser(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
            .withLastCommit(hook.getObjectAttributes().getLastCommit().getId())
            .withTargetProjectUrl(hook.getProject().getWebUrl())
            .build();
    }
}
