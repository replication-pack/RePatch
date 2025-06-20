package edu.unlv.cs.evol.repatch.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRevisionNumber;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.reset.GitResetMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GitUtils {

    private final Project project;
    private final GitRepository repo;

    public GitUtils(GitRepository repository, Project proj) {
        repo = repository;
        project = proj;
    }

    /*
     * Perform the git checkout with the IntelliJ API.
     */
    public void checkout(String commit) {
        GitThread thread = new GitThread(repo, commit);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Refresh the virtual file system after the commit
        Utils.refreshVFS();
    }

    /*
     * Perform git add -A and git commit
     */
    public String addAndCommit() {
        add();
        return commit();
    }

    public void add() {
        GitAdd thread = new GitAdd(project, repo);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String commit() {
        DoGitCommit gitCommit = new DoGitCommit(repo, project);

        Thread thread = new Thread(gitCommit);
        thread.start();
        try {
            thread.join();
            return gitCommit.getCommit();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean merge(String rightCommit) {
        AtomicReference<GitCommandResult> gitCommandResult = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            GitLineHandler lineHandler = new GitLineHandler(project, repo.getRoot(), GitCommand.MERGE);
            lineHandler.addParameters(rightCommit, "--no-commit");
            gitCommandResult.set(Git.getInstance().runCommand(lineHandler));
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String gitMergeResult = gitCommandResult.get().toString();
        if(gitMergeResult.contains("conflict")) {
            return hasJavaConflict(gitMergeResult);
        }
        return false;
    }

    public boolean cherryPick(String rightCommit) {
        System.out.println("-> Try cherry picking after undoing refactoring.......");
        AtomicReference<GitCommandResult> gitCommandResult = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            GitLineHandler lineHandler = new GitLineHandler(project, repo.getRoot(), GitCommand.CHERRY_PICK);
            lineHandler.addParameters(rightCommit, "--no-commit");
            gitCommandResult.set(Git.getInstance().runCommand(lineHandler));
        });
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String gitCherryPickResult = gitCommandResult.get().toString();

        if (gitCherryPickResult.contains("conflict") || gitCherryPickResult.contains("CONFLICT")) {
            return hasJavaConflict(gitCherryPickResult);
        }
        if (gitCherryPickResult.contains("error") || gitCherryPickResult.contains("fatal")) {
            return true;
        }

        return false;
    }


    private boolean hasJavaConflict(String gitResult) {
        for (String line : gitResult.split(",")) {
            if (!line.contains("CONFLICT")) {
                continue;
            }
            if (line.contains(".java")) {
                return true;
            }
        }
        return false;
    }

    /*
     * Get the base commit of the merge scenario.
     */
    public String getBaseCommit(String left, String right) {
        VirtualFile root = repo.getRoot();
        class BaseThread extends Thread {
            private final Project project;
            private final VirtualFile root;
            private final String leftCommit;
            private final String rightCommit;
            private String baseCommit;

            BaseThread(Project project, VirtualFile root, String leftCommit, String rightCommit) {
                this.project = project;
                this.root = root;
                this.leftCommit = leftCommit;
                this.rightCommit = rightCommit;
            }
            @Override
            public void run() {
                GitRevisionNumber num = null;
                try {
                    num = GitHistoryUtils.getMergeBase(project, root, leftCommit, rightCommit);
                } catch (VcsException e) {
                    System.out.println("Project: " + project + " LeftCommit: " + leftCommit + " RightCommit: " + rightCommit);
                    e.printStackTrace();
                }
                if(num == null) {
                    this.baseCommit = null;
                }
                else {
                    this.baseCommit = num.getShortRev();
                }
            }

            public String getBaseCommit() {
                return baseCommit;
            }
        }

        BaseThread thread = new BaseThread(project, root, left, right);
        thread.start();
        try {
            thread.join();
            return thread.getBaseCommit();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String diff(String dir, String path1, String path2) {
        StringBuilder builder = new StringBuilder();
        try {
            String commands = "git diff --ignore-cr-at-eol --ignore-all-space --ignore-blank-lines --ignore-space-change " +
                    "--no-index -U0 " + path1 + " " + path2;
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(commands, null, new File(dir));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String s;
            while ((s = stdInput.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
            }
            while ((s = stdError.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();

    }

    public List<String> getConflictingFilePaths() {
        AtomicReference<GitCommandResult> gitCommandResult = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            GitLineHandler lineHandler = new GitLineHandler(project, repo.getRoot(), GitCommand.DIFF);
            lineHandler.addParameters("--name-only", "--diff-filter=U");
            gitCommandResult.set(Git.getInstance().runCommand(lineHandler));
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return gitCommandResult.get().getOutput();

    }



}

class DoGitCommit implements Runnable {
    private final Project project;
    private final GitRepository repo;
    private String commit;

    public DoGitCommit(GitRepository repo, Project project) {
        this.project = project;
        this.repo = repo;
    }

    @Override
    public void run() {
        GitLineHandler lineHandler = new GitLineHandler(project, repo.getRoot(), GitCommand.COMMIT);
//        System.out.println("Project path" + project.getBasePath());
        // Add message to commit to clearly show it's RefMerge step
        lineHandler.addParameters("-m", "RefMerge");
        GitCommandResult result = Git.getInstance().runCommand(lineHandler);
        String res = result.getOutput().get(0);
        // get the commit hash from the output message
        String commit;
        if(res.contains("]")) {
            commit = res.substring(res.indexOf("HEAD") + 1, res.indexOf("]") - 1);
        }
        else {
            commit = res.substring(res.lastIndexOf(" ") + 1, res.length()-1);
        }
        this.commit = commit.substring(commit.lastIndexOf(" ") + 1);
    }

    public String getCommit() {
        return commit;
    }
}

class GitAdd extends Thread {
    final GitRepository repo;
    final Project project;

    public GitAdd(Project project, GitRepository repo) {
        this.project = project;
        this.repo = repo;
    }

    @Override
    public void run() {
        GitLineHandler lineHandler = new GitLineHandler(project, repo.getRoot(), GitCommand.ADD);
        lineHandler.addParameters("-A");
        Git.getInstance().runCommand(lineHandler);
    }

}

class GitThread extends Thread {
    final GitRepository repo;
    final String commit;

    public GitThread(GitRepository repo, String commit) {
        this.repo = repo;
        this.commit = commit;
    }

    @Override
    public void run()
    {
        Git.getInstance().reset(repo, GitResetMode.HARD, "HEAD");
        Git.getInstance().checkout(repo, commit, null, true, false, false);
    }
}
