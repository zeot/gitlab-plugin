package com.dabsquared.gitlabjenkins.gitlab.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class File {

    private String fileName;
    private String filePath;
    private Long size;
    private String encoding;
    private String content;
    private String ref;
    private String blobId;
    private String commitId;
    private String lastCommitId;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getBlobId() {
        return blobId;
    }

    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getLastCommitId() {
        return lastCommitId;
    }

    public void setLastCommitId(String lastCommitId) {
        this.lastCommitId = lastCommitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        File file = (File) o;
        return new EqualsBuilder()
            .append(fileName, file.fileName)
            .append(filePath, file.filePath)
            .append(size, file.size)
            .append(encoding, file.encoding)
            .append(content, file.content)
            .append(ref, file.ref)
            .append(blobId, file.blobId)
            .append(commitId, file.commitId)
            .append(lastCommitId, file.lastCommitId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(fileName)
            .append(filePath)
            .append(size)
            .append(encoding)
            .append(content)
            .append(ref)
            .append(blobId)
            .append(commitId)
            .append(lastCommitId)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("fileName", fileName)
            .append("filePath", filePath)
            .append("size", size)
            .append("encoding", encoding)
            .append("content", content)
            .append("ref", ref)
            .append("blobId", blobId)
            .append("commitId", commitId)
            .append("lastCommitId", lastCommitId)
            .toString();
    }
}
