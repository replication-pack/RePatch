package edu.unlv.cs.evol.repatch.invertOperations;

import edu.unlv.cs.evol.repatch.utils.Utils;
import edu.unlv.cs.evol.repatch.refactoringObjects.RefactoringObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.RenamePackageObject;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveDirectoryWithClassesProcessor;
import com.intellij.refactoring.rename.RenamePsiPackageProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;

public class InvertRenamePackage {
    private final Project project;

    public InvertRenamePackage(Project project) {
        this.project = project;
    }

    public void invertRenamePackage(RefactoringObject ref) {
        RenamePackageObject renamePackageObject = (RenamePackageObject) ref;
        String originalPackageName = renamePackageObject.getOriginalName();
        String renamedPackageName = renamePackageObject.getDestinationName();

        PsiPackage refactoredPsiPackage = JavaPsiFacade.getInstance(project).findPackage(renamedPackageName);
        // If we couldn't find the refactored PSI package, do not try to invert
        if(refactoredPsiPackage == null) {
            return;
        }

        // Have not found a way to add the source root with just the psi package so using this as a workaround
        // until we find a way
        // 1. Get a class in the package, so we can add the source root
        PsiClass psiClass = refactoredPsiPackage.getClasses()[0];
        // 2. Get the classes qualified name
        String className = psiClass.getQualifiedName();
        // 3. Get the file path of the containing PSI file
        String filePath = psiClass.getContainingFile().getName();
        Utils utils = new Utils(project);
        utils.addSourceRoot(filePath, className);


        RefactoringFactory factory = JavaRefactoringFactory.getInstance(project);
        RenameRefactoring renameRefactoring = factory.createRename(refactoredPsiPackage, originalPackageName, true, true);
        UsageInfo[] refactoringUsages = renameRefactoring.findUsages();
        if(refactoringUsages.length > 0) {
            renameRefactoring.doRefactoring(refactoringUsages);
        }
        else {

            MoveDirectoryWithClassesProcessor processor =
                    RenamePsiPackageProcessor.createRenameMoveProcessor(originalPackageName, refactoredPsiPackage, false, false);
            WriteCommandAction.runWriteCommandAction(project, processor);
        }
        // Check if there is a usage view. if so, close the usage view and do not perform the refactoring.
        // Need to check if there is a workaround or solution for this
        UsageViewManager viewManager = UsageViewManager.getInstance(project);
        UsageView usageView = viewManager.getSelectedUsageView();
        if(usageView != null) {
            usageView.close();
        }

    }
}
