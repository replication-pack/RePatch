package edu.unlv.cs.evol.repatch;

import edu.unlv.cs.evol.repatch.invertOperations.InvertRefactorings;
import edu.unlv.cs.evol.repatch.utils.GitUtils;
import edu.unlv.cs.evol.repatch.matrix.Matrix;
import edu.unlv.cs.evol.repatch.replayOperations.*;
import edu.unlv.cs.evol.repatch.invertOperations.*;
import edu.unlv.cs.evol.repatch.replayOperations.ReplayRefactorings;
import edu.unlv.cs.evol.repatch.utils.RefactoringObjectUtils;
import edu.unlv.cs.evol.repatch.refactoringObjects.RefactoringObject;
import edu.unlv.cs.evol.repatch.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


public class RePatch extends AnAction {

    Git git;
    Project project;



    @Override
    public void update(@NotNull AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        GitRepositoryManager repoManager = GitRepositoryManager.getInstance(project);
        List<GitRepository> repos = repoManager.getRepositories();
        GitRepository repo = repos.get(0);

        String leftCommit = System.getenv("LEFT_COMMIT");
        String rightCommit = System.getenv("RIGHT_COMMIT");
        String baseCommit = System.getenv("BASE_COMMIT");

        List<Refactoring> detectedRefactorings = new ArrayList<>();
        rePatch(rightCommit, leftCommit, baseCommit, project, repo, detectedRefactorings);

    }

    /*
     * Gets the directory of the project that's being merged, then it calls the function that performs the merge.
     */
    public ArrayList<Pair<RefactoringObject, RefactoringObject>> rePatch(String rightCommit, String leftCommit, String baseCommit,
                                                                          Project project, GitRepository repo,
                                                                          List<Refactoring> detectedRefactorings) {
        this.project = project;
        File dir = new File(Objects.requireNonNull(project.getBasePath()));
        try {
            git = Git.open(dir);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return doMerge(rightCommit, leftCommit, baseCommit, repo, detectedRefactorings);

    }

    /*
     * This method gets the refactorings that are between the base commit and the left and right commits. It uses the
     * matrix to determine if any of the refactorings are conflicting or have ordering dependencies.
     * Then it checks out the base commit, saving it in a temporary directory. It checks out the right commit, undoes
     * the refactorings, and saves the content into a respective temporary directory. It does the same thing for the
     * left commit, but it uses the current directory instead of saving it to a new one. After it's undone all the
     * refactorings, the merge function is called and it replays the refactorings.
     *
     * We modify this to take in custom baseCommit (parent of the remote commit you want to cherry-pick
     *  in order to mimic cherry pick
     */
    private ArrayList<Pair<RefactoringObject, RefactoringObject>> doMerge(String rightCommit, String leftCommit, String baseCommit,
                                                                          GitRepository repo,
                                                                          List<Refactoring> detectedRefactorings){
        long time = System.currentTimeMillis();
        GitUtils gitUtils = new GitUtils(repo, project);
        //String baseCommit = gitUtils.getBaseCommit(leftCommit, rightCommit); // we pass this directly in the method
        System.out.println("Detecting refactorings");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<ArrayList<RefactoringObject>> rightRefsAtomic = new AtomicReference<>(new ArrayList<>());
        AtomicReference<ArrayList<RefactoringObject>> leftRefsAtomic = new AtomicReference<>(new ArrayList<>());
        Future futureRefMiner = executor.submit(() -> {
            rightRefsAtomic.set(detectAndSimplifyRefactorings(rightCommit, baseCommit, detectedRefactorings));
            leftRefsAtomic.set(detectAndSimplifyRefactorings(leftCommit, baseCommit, detectedRefactorings));
        });
        try {
            futureRefMiner.get(11, TimeUnit.MINUTES);


        } catch (TimeoutException e) {
            System.out.println("RePatch Timed Out");
            return null;
        }
        catch (InterruptedException | ExecutionException e) {
            System.out.println("There was an error detecting refactorings");
            e.printStackTrace();
            return null;
        }

        ArrayList<RefactoringObject> rightRefs = rightRefsAtomic.get();
        ArrayList<RefactoringObject> leftRefs = leftRefsAtomic.get();

        long time2 = System.currentTimeMillis();
        // If it timed out
        if((time - time2) > 900000) {
            System.out.println("RePatch Timed Out");
            return null;
        }


        gitUtils.checkout(rightCommit);
        // Update the PSI classes after the commit
        Utils.reparsePsiFiles(project);
        Utils.dumbServiceHandler(project);
        System.out.println("Inverting right refactorings");
        int failedRefactorings = InvertRefactorings.invertRefactorings(rightRefs, project);
        Utils.reparsePsiFiles(project);
        Utils.dumbServiceHandler(project);
        String rightUndoCommit = gitUtils.addAndCommit();
        gitUtils.checkout(leftCommit);
        // Update the PSI classes after the commit
        Utils.reparsePsiFiles(project);
        Utils.dumbServiceHandler(project);
        System.out.println("Inverting left refactorings");
        failedRefactorings += InvertRefactorings.invertRefactorings(leftRefs, project);

        gitUtils.addAndCommit();

        String message = failedRefactorings + " refactorings were not inverted for " + leftCommit + " and " + rightCommit;
        Utils.log(project.getName(), message);

        // boolean isConflicting = gitUtils.merge(rightUndoCommit);
        boolean isConflicting = gitUtils.cherryPick(rightUndoCommit);

        Utils.refreshVFS();
        Utils.reparsePsiFiles(project);
        Utils.dumbServiceHandler(project);

        // Check if any of the refactorings are conflicting or have ordering dependencies
        System.out.println("Detecting refactoring conflicts");
        Matrix matrix = new Matrix(project);

        Pair<ArrayList<Pair<RefactoringObject, RefactoringObject>>, ArrayList<RefactoringObject>> pair = matrix.detectConflicts(leftRefs, rightRefs);

        time2 = System.currentTimeMillis();
        // Timeout if it's been 15 minutes
        if((time - time2) > 900000) {
            System.out.println("RePatch Timed Out");
            return null;
        }

        ArrayList<RefactoringObject> refactorings = pair.getRight();
        if(isConflicting) {
            List<String> conflictingFilePaths = gitUtils.getConflictingFilePaths();
            for(String conflictingFilePath : conflictingFilePaths) {
                Utils utils = new Utils(project);
                utils.removeRefactoringsInConflictingFile(conflictingFilePath, refactorings);

            }
        }

        // Combine the lists so we can perform all the refactorings on the merged project
        // Replay all of the refactorings
        System.out.println("Replaying refactorings");
        ReplayRefactorings.replayRefactorings(pair.getRight(), project);

        return pair.getLeft();

    }

    /*
     * Use RefMiner to detect refactorings in commits between the base commit and the parent commit. Compare each newly
     * detected refactoring against previously detected refactorings to check for transitivity or if the refactorings can
     * be simplified.
     */
    public ArrayList<RefactoringObject> detectAndSimplifyRefactorings(String commit, String base, List<Refactoring> detectedRefactorings) {
        ArrayList<RefactoringObject> simplifiedRefactorings = new ArrayList<>();
        Matrix matrix = new Matrix(project);
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        try {
            miner.detectBetweenCommits(git.getRepository(), base, commit,
                new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
                        // Add each refactoring to refResult
                        for(Refactoring refactoring : refactorings) {
                            // Create the refactoring object so we can compare and update
                            detectedRefactorings.add(refactoring);
                            RefactoringObject refactoringObject = RefactoringObjectUtils.createRefactoringObject(refactoring);
                            // If the refactoring type is not presently supported, skip it
                            if(refactoringObject == null) {
                                continue;
                            }
                            // simplify refactorings and check if factoring is transitive
                            matrix.simplifyAndInsertRefactorings(refactoringObject, simplifiedRefactorings);
                        }
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return simplifiedRefactorings;
    }

}