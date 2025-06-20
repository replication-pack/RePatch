package edu.unlv.cs.evol.repatch.invertOperations;

import edu.unlv.cs.evol.repatch.refactoringObjects.RenameParameterObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.typeObjects.MethodSignatureObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.typeObjects.ParameterObject;
import edu.unlv.cs.evol.repatch.utils.Utils;
import edu.unlv.cs.evol.repatch.refactoringObjects.RefactoringObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;

public class InvertRenameParameter {
    Project project;

    public InvertRenameParameter(Project project) {
        this.project = project;
    }

    public void invertRenameParameter(RefactoringObject refactoringObject) {
        RenameParameterObject parameterObject = (RenameParameterObject) refactoringObject;

        ParameterObject originalParameterObject = parameterObject.getOriginalParameterObject();
        ParameterObject refactoredParameterObject = parameterObject.getRefactoredParameterObject();

        String originalParameterName = originalParameterObject.getName();
        String originalClassName = parameterObject.getOriginalClassName();
        String originalFilePath = parameterObject.getOriginalFilePath();
        MethodSignatureObject originalMethodSignature = parameterObject.getOriginalMethodSignature();

        Utils utils = new Utils(project);
        utils.addSourceRoot(originalFilePath, originalClassName);
        // Get PSI class so we can find PSI method
        PsiClass psiClass = utils.getPsiClassFromClassAndFileNames(originalClassName, originalFilePath);
        // If we cannot find the PSI class, do not try to invert the refactoring
        if (psiClass == null) {
            return;
        }
        VirtualFile vFile = psiClass.getContainingFile().getVirtualFile();
        // Get PSI method so we can find PSI parameter
        PsiMethod psiMethod = Utils.getPsiMethod(psiClass, originalMethodSignature);
        if (psiMethod == null) {
            return;
        }

        PsiParameter psiParameter = Utils.getPsiParameter(psiMethod, refactoredParameterObject);

        if(psiParameter == null) {
            return;
        }

        RefactoringFactory factory = JavaRefactoringFactory.getInstance(project);
        RenameRefactoring renameRefactoring = factory.createRename(psiParameter, originalParameterName, true, true);
        UsageInfo[] refactoringUsages = renameRefactoring.findUsages();
        renameRefactoring.doRefactoring(refactoringUsages);

        UsageViewManager viewManager = UsageViewManager.getInstance(project);
        UsageView usageView = viewManager.getSelectedUsageView();
        if(usageView != null) {
            usageView.close();
        }



        // Update the virtual file that contains the refactoring
        vFile.refresh(false, true);


    }

}
