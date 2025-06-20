package edu.unlv.cs.evol.integration.database;

import edu.unlv.cs.evol.integration.data.ConflictingFileData;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("conflicting_file")
public class ConflictingFile extends Model {

    public ConflictingFile() {}

    public ConflictingFile(MergeResult mergeResult, ConflictingFileData conflictingFileData) {
        set("merge_tool", mergeResult.getMergeTool(), "path", conflictingFileData.getFilePath(),
                "total_conflicts", conflictingFileData.getConflictingBlocks(),
                "total_conflicting_loc", conflictingFileData.getConflictingLOC(),
                "merge_result_id", mergeResult.getId(),
                "merge_commit_id", mergeResult.getMergeCommitId(),
                "project_id", mergeResult.getProjectId(),
                "patch_id", mergeResult.getPatchId());
    }

    public String getPath() {
        return getString("path");
    }

    public int getMergeResultId() {
        return getInteger("merge_result_id");
    }

    public int getMergeCommitId() {
        return getInteger("merge_commit_id");
    }

    public int getProjectId() {
        return getInteger("project_id");
    }

    public int getPatchId() {
        return getInteger("patch_id");
    }
}
