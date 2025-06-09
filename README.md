# Refactoring-Aware Patch Integration Across Structurally Divergent Java Forks

While most forks on platforms like GitHub are short-lived and used for social collaboration, a smaller but impactful subset evolve into long-lived forks, referred to here as variants, that maintain independent development trajectories. Integrating bug-fix patches across such divergent variants poses challenges due to structural drift, including refactorings that rename, relocate, or reorganize code elements and obscure semantic correspondence. This paper presents an empirical study of patch integration failures in **14 divergent pair** of variants and introduces ``RePatch``, a refactoring-aware integration system for Java repositories. ``RePatch`` extends the RefMerge framework, originally designed for symmetric merges, by supporting asymmetric patch transfer. ``RePatch`` inverts refactorings in both the source and target to realign the patch context, applies the patch, and replays the transformations to preserve the intent of the variant. In our evaluation of **478 bug-fix pull requests**, Git ``cherry-pick`` fails in **64.4%** of cases due to structural misalignments, while ``RePatch`` successfully integrates **52.8%** of the previously failing patches. These results highlight the limitations of syntax-based tools and the need for semantic reasoning in variant-aware patch propagation.


## System Requirements
* Operating System: Linux
* RAM: >= 16 GB
* Free Storage: >= 15 GB
* Processor: CPU 1.18 GHz or greater

* Git
* Java 11
* IntelliJ (Community Addition) Version 2020.1.2
* MySQL Database >= 8.0

## Directory Structure and Description
```bash
.
├── .gitignore                              # files to ignore in version control
├── COPYING                                 # licensing info (typically GNU-style)
├── LICENSE                                 # project license 
├── README.md                               # project overview and instructions
├── analysis/                               # data analysis and notebooks
│   └── notebook.ipynb                      
├── build.gradle                            # Gradle build configuration
├── database/                               # local database dump and assets
│   └── localhost.sql.zip                   
├── database.properties                     # DB connection settings
├── github-oauth.properties                 # configuration for GitHub API access
├── gradle/                                 # Gradle wrapper files
│   └── wrapper/
├── gradle.properties                       # Gradle build settings
├── gradlew                                 # Unix script to run Gradle wrapper
├── lib/                                    # external libraries or JARS
├── settings.gradle                         # project and module settings
└── src/                                    # source code
    ├── main/                               # application source code
    └── test/                               # test source code
```

## Setting up
To setup and test ``RePatch`` tool on your local computer, following the steps below:

### Get the code and other assets:
The easiest way is using the git clone command:
```
git clone https://github.com/replication-pack/PatchTrack.git
```
Open the project in Intellij and wait for it to index and build the files. 

### Configure RefactoringMiner

Use `Git Clone https://github.com/tsantalis/RefactoringMiner.git` to clone RefactoringMiner. Then build RefactoringMiner with `./gradlew distzip`. It will be under build/distributions.

You will need to add RefactoringMiner to your local maven repository to use it in the build.gradle. You can use `mvn install:install-file -Dfile=<path-to-file>` to add it to your local maven repository. You can verify that it's been installed by checking the path `/home/username/.m2/repository/org/refactoringminer`.


### Edit configuration
Edit the configuration tasks to have `:runIde -Pmode=integration -PdataPath=path -PevaluationProject=project`, where path is the path to the cloned test projects and project is the test project (target variant). Make sure to create the `path` directory.

Edit the configuration tasks in the IntelliJ IDE under `Run | Edit Configurations` (more information can be found [here](https://www.jetbrains.com/help/idea/run-debug-configuration.html#create-permanent)) to have `:runIde` and include set `-Pmode=` to `integration`.
Then, set `-PevaluationProject=` to the project (target variant) that you want to evaluate on. For example,
it would look like `-PevaluationProject=kafka` if you want to run integration on `linkedin/kafka`.

### Running the Experiment
**NB: Running the entire experiment takes more than 10 hour to complete. For this reason, provide a sample source -> target variant and 10 patches (pull requests), alongside the full dataset, to facilate quick testing of the tool/experiment. Both the test and full projects are located in: `src/main/resources` directory**. 

Follow the steps below to run the experiment:

1. Create a GitHub token and add it to `github-oauth.properties`. 
   
2. Add the corresponding integration project to the configuration in the IntelliJ IDE under `Run | Edit Configurations`. For example, `-PevaluationProject=kafka`. 

3. RePatch will automatically clone the target variant and add the remote source variant. Once this is done stop the running project and open the integration project with the IntelliJ IDEA in a new window. 

4. Wait for IntelliJ to build the cloned project, then close it.

5. Now re-run the `RePatch` by clicking the `Run` button in the IntelliJ IDE.

6. Wait for the integration pipeline to finish processing that project.

The data from the integration pipeline will be stored in the database, `refactoring_aware_integration`. `RePatch` will create the database if it does not already exist. Finally, use the scripts in `analysis` directory to get tables and plots from the data.

## Analysis and Reproducibility
We provide SQL scripts, `CSV` files and notebook to support reproduciblity of the results reported in the paper. This can be found in the `analysis` directory. If you want to regenerate the CSV files, setup and populate the database with the data provide in `database` directory

### RQ1

### RQ2

### RQ3


