package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.File;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Robin Müller
 */
@Path("/api/v3")
@Produces(MediaType.APPLICATION_JSON)
public interface GitLabApi {

    @POST
    @Path("/projects")
    Project createProject(@QueryParam("name") String projectName);

    @POST
    @Path("/projects/{projectId}/merge_requests")
    void createMergeRequest(
        @PathParam("projectId") Integer projectId,
        @QueryParam("source_branch") String sourceBranch,
        @QueryParam("target_branch") String targetBranch,
        @QueryParam("title") String title);

    @GET
    @Path("/projects/{projectName}")
    Project getProject(@PathParam("projectName") String projectName);

    @PUT
    @Path("/projects/{projectId}")
    Project updateProject(@PathParam("projectId") String projectId,
                          @QueryParam("name") String name,
                          @QueryParam("path") String path);

    @DELETE
    @Path("/projects/{projectId}")
    void deleteProject(@PathParam("projectId") String projectId);

    @POST
    @Path("/projects/{projectId}/hooks")
    void addProjectHook(@PathParam("projectId") String projectId,
                        @QueryParam("url") String url,
                        @QueryParam("push_events") Boolean pushEvents,
                        @QueryParam("merge_requests_events") Boolean mergeRequestEvents,
                        @QueryParam("note_events") Boolean noteEvents);

    @POST
    @Path("/projects/{projectId}/statuses/{sha}")
    void changeBuildStatus(@PathParam("projectId") String projectId,
                           @PathParam("sha") String sha,
                           @QueryParam("state") BuildState state,
                           @QueryParam("ref") String ref,
                           @QueryParam("context") String context,
                           @QueryParam("target_url") String targetUrl,
                           @QueryParam("description") String description);

    @POST
    @Path("/projects/{projectId}/statuses/{sha}")
    void changeBuildStatus(@PathParam("projectId") Integer projectId,
                           @PathParam("sha") String sha,
                           @QueryParam("state") BuildState state,
                           @QueryParam("ref") String ref,
                           @QueryParam("context") String context,
                           @QueryParam("target_url") String targetUrl,
                           @QueryParam("description") String description);

    @GET
    @Path("/projects/{projectId}/repository/commits/{sha}")
    void getCommit(@PathParam("projectId") String projectId, @PathParam("sha") String sha);


    @PUT
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/merge")
    void acceptMergeRequest(@PathParam("projectId") Integer projectId,
                            @PathParam("mergeRequestId") Integer mergeRequestId,
                            @QueryParam("merge_commit_message") String mergeCommitMessage,
                            @QueryParam("should_remove_source_branch") boolean shouldRemoveSourceBranch);

    @POST
    @Path("/projects/{projectId}/merge_requests/{mergeRequestId}/notes")
    void createMergeRequestNote(@PathParam("projectId") Integer projectId,
                                @PathParam("mergeRequestId") Integer mergeRequestId,
                                @QueryParam("body") String body);

    @GET
    @Path("/projects/{projectId}/merge_requests")
    List<MergeRequest> getMergeRequests(@PathParam("projectId") String projectId,
                                        @QueryParam("state") State state,
                                        @QueryParam("page") int page,
                                        @QueryParam("per_page") int perPage);

    @GET
    @Path("/projects/{projectId}/repository/branches")
    List<Branch> getBranches(@PathParam("projectId") String projectId);

    @GET
    @Path("/projects/{projectId}/repository/branches/{branch}")
    Branch getBranch(@PathParam("projectId") String projectId,
                     @PathParam("branch") String branch);

    @HEAD
    @Path("/user")
    void headCurrentUser();

    @GET
    @Path("/user")
    User getCurrentUser();

    @GET
    @Path("/users/{userId}")
    User getUser(@PathParam("userId") int userId);

    @POST
    @Path("/users")
    User addUser(@QueryParam("email") String email,
                 @QueryParam("username") String username,
                 @QueryParam("name") String name,
                 @QueryParam("password") String password);

    @PUT
    @Path("/users/{userId}")
    User updateUser(@PathParam("userId") String userId,
                    @QueryParam("email") String email,
                    @QueryParam("username") String username,
                    @QueryParam("name") String name,
                    @QueryParam("password") String password);

    @GET
    @Path("/projects/{projectId}/repository/files")
    File getFile(@PathParam("projectId") String projectId,
                 @QueryParam("file_path") String filePath,
                 @QueryParam("ref") String ref);
}
