package edu.unlv.cs.evol.repatch.matrix.logicCells;

import edu.unlv.cs.evol.repatch.refactoringObjects.typeObjects.MethodSignatureObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.typeObjects.ParameterObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.ChangeParameterTypeObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.InlineMethodObject;
import edu.unlv.cs.evol.repatch.refactoringObjects.RefactoringObject;

public class ChangeParameterTypeInlineMethodCell {

    public static void checkCombination(RefactoringObject methodObject, RefactoringObject parameterObject) {
        ChangeParameterTypeObject parameter = (ChangeParameterTypeObject) parameterObject;
        InlineMethodObject method = (InlineMethodObject) methodObject;

        String originalParameterClass = parameter.getOriginalClass();
        String originalMethodClass = method.getOriginalClassName();
        String destinationParameterClass = parameter.getDestinationClass();
        String destinationMethodClass = method.getDestinationClassName();

        MethodSignatureObject originalMethodSignature = method.getOriginalMethodSignature();
        MethodSignatureObject originalParameterMethodSignature = parameter.getOriginalMethod();
        MethodSignatureObject destinationParameterMethodSignature = parameter.getDestinationMethod();
        MethodSignatureObject destinationMethodSignature = method.getDestinationMethodSignature();

        ParameterObject destinationParameter = parameter.getDestinationParameter();

        // If the source method's parameter type is changed
        if(destinationMethodClass.equals(originalParameterClass)
                && originalMethodSignature.equalsSignature(originalParameterMethodSignature)) {
            int location = destinationParameterMethodSignature.getParameterLocation(destinationParameter);
            originalMethodSignature.updateParameterAtLocation(location, destinationParameter);
            ((InlineMethodObject) methodObject).setOriginalMethodSignature(originalMethodSignature);

        }

        // If the target method's parameter type is changed
        else if(originalMethodClass.equals(destinationParameterClass)
                && destinationMethodSignature.equalsSignature(originalParameterMethodSignature)) {
            int location = destinationParameterMethodSignature.getParameterLocation(destinationParameter);
            destinationMethodSignature.updateParameterAtLocation(location, destinationParameter);
            ((InlineMethodObject) methodObject).setDestinationMethodSignature(destinationMethodSignature);
        }

    }
}
