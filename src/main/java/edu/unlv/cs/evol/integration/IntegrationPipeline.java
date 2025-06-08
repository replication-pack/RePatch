package edu.unlv.cs.evol.integration;


import com.intellij.openapi.application.ApplicationStarter;
import edu.unlv.cs.evol.integration.database.DatabaseUtils;
import org.javalite.activejdbc.Base;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IntegrationPipeline implements ApplicationStarter {

    @Override
    public String getCommandName() {
        return "integration";
    }

    @Override
    public void main(@NotNull List<String> args) {
        try {
            String mode = args.get(1);

            DatabaseUtils.createDatabase(true);
            String path = System.getProperty("user.home") +"/" + args.get(2);
            String projectName = args.get(3);
            startIntegration(path, projectName);

        } catch(Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }


    /*
     * Start integration of divergent pairs.
     * start integration
     * change db frame refactoring_aware_integration
     */
    private void startIntegration(String path, String evaluationProject) {
        try {
            Base.open("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/refactoring_aware_integration?serverTimezone=UTC",
                    "root", "student");
            RePatchEvaluation evaluation = new RePatchEvaluation();
            evaluation.runComparison(path, evaluationProject);
            Base.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }




}