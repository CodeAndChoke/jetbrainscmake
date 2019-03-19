package action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import config.ExecutableState;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Execute the actionPerformed when user intends to use the plugin
 */
class NewEntryPointAction extends AnAction {
    private static final int EXE_NOT_EXIST = 0;
    private static final int EXE_EXIST_SAME_SOURCE = 1;
    private static final int EXE_EXIST_DIFFERENT_SOURCE = 2;
    private static final String CMAKE_FILE = "/CMakeLists.txt";

    /**
     * User pushed the hot key and action needed to be verified
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        // First step: Process the action event and find out if the project has any CMake file
        ActionProperty actionProperty = processEvent(event);
        if (actionProperty == null) {
            return;
        }


        actionProperty.setCmakeDocument(FileDocumentManager.getInstance().getDocument(actionProperty.getCmakeFile()));
        String fileName;
        if (actionProperty.getTargetedSourceFile() == null) {
            fileName = null;
        } else {
            fileName = actionProperty.getTargetedSourceFile().getName();
        }
        actionProperty.setFileName(fileName);

        String executable = buildExecutableName(actionProperty);

        actionProperty.setExecutable(executable);

        String relativeSourcePath;

        if (!actionProperty.isCmakeOnCurrentFolderFound()) {
            relativeSourcePath = actionProperty.getTargetedSourceFile().getNameWithoutExtension();
        } else {
            relativeSourcePath = getRelativeSourcePath(actionProperty);
        }

        actionProperty.setRelativeSourcePath(relativeSourcePath);

        processCMakeFile(actionProperty);


        finishEvent(actionProperty);
    }

    /**
     * After every is verfied, this method will execute the calculated result
     */
    private void finishEvent(ActionProperty actionProperty) {
        int executableExists = actionProperty.getExecutableExists();
        String fileName = actionProperty.getFileName();
        switch (executableExists) {
            case EXE_NOT_EXIST:
                insertAddExecutable(actionProperty);
                Notifications.Bus.notify(
                        new Notification("new_executable_action",
                                "New Entry Point Plugin",
                                "add_executable added for " + fileName + ".",
                                NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_SAME_SOURCE:
                Notifications.Bus.notify(
                        new Notification("new_executable_action",
                                "New Entry Point Plugin",
                                "add_executable for this source already exists.",
                                NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_DIFFERENT_SOURCE:
                updateAddExecutable(actionProperty);
                Notifications.Bus.notify(
                        new Notification("new_executable_action",
                                "New Entry Point Plugin",
                                "add_executable overwritten",
                                NotificationType.INFORMATION)
                );
                break;
            default:
                break;
        }
    }

    /**
     * Process the text of the input cmake file and find out if the executable name already exists
     */
    private void processCMakeFile(ActionProperty actionProperty) {
        String executable = actionProperty.getExecutable();
        String regex = "^add_executable\\s*?\\(\\s*?" + executable + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        String CMakeText = actionProperty.getCmakeDocument().getText();
        Scanner scanner = new Scanner(CMakeText);
        int executableExists = EXE_NOT_EXIST;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String existingSourceName = m.group(1);
                if (existingSourceName.contains(actionProperty.getRelativeSourcePath())) {
                    executableExists = EXE_EXIST_SAME_SOURCE;
                } else {
                    executableExists = EXE_EXIST_DIFFERENT_SOURCE;
                }
                break;
            }
        }
        scanner.close();
        actionProperty.setExecutableExists(executableExists);
    }

    /**
     * @return the path from the project's root to the targeted source file
     */
    private static String getRelativeSourcePath(ActionProperty actionProperty) {
        VirtualFile targetedSourceFile = actionProperty.getTargetedSourceFile();
        return new File(Objects.requireNonNull(targetedSourceFile.getParent().getPath())).toURI().relativize(new File(targetedSourceFile.getPath()).toURI()).getPath();
    }

    /**
     * Process the incoming event and return the name of the nearest cmake file.
     *
     * @param event incoming event
     * @return name of the nearest cmake file
     */
    private ActionProperty processEvent(AnActionEvent event) {
        ActionProperty actionProperty = new ActionProperty();
        actionProperty.setProject(event.getRequiredData(CommonDataKeys.PROJECT));
        actionProperty.setExecutableState(ExecutableState.getInstance(actionProperty.getProject()));
        actionProperty.setTargetedSourceFile(event.getData(PlatformDataKeys.VIRTUAL_FILE));
        String nearestCmake = Objects.requireNonNull(actionProperty.getTargetedSourceFile()).getParent().getPath() + CMAKE_FILE;

        File cmakeOnCurrentFolder = new File(nearestCmake);
        if (!cmakeOnCurrentFolder.exists()) {
            nearestCmake = actionProperty.getProject().getBasePath() + CMAKE_FILE;
            actionProperty.setCmakeOnCurrentFolderFound(false);
        } else {
            actionProperty.setCmakeOnCurrentFolderFound(true);
        }

        File appendingCmake = new File(nearestCmake);
        actionProperty.setCmakeFile(LocalFileSystem.getInstance().findFileByIoFile(appendingCmake));
        if (actionProperty.getCmakeFile() == null) {
            Notifications.Bus.notify(new Notification(
                    "new_executable_action",
                    "New Executable Plugin",
                    "Fail to access " + nearestCmake,
                    NotificationType.ERROR));
            return null;
        }
        return actionProperty;
    }

    private void insertAddExecutable(ActionProperty actionProperty) {
        Document cmakelistDocument = actionProperty.getCmakeDocument();
        String exeName = actionProperty.getExecutable();
        ApplicationManager.getApplication().runWriteAction(() -> {
            String updatedText = cmakelistDocument.getText();
            updatedText += "\n" + constructAddExecutable(actionProperty);
            String runtimeDir = "";
            if (runtimeDir != null && !runtimeDir.equals("")) {
                String outputDir = quoteString(buildOutputDirectory(actionProperty));
                updatedText += "\n" + constructSetTargetProperties(exeName, outputDir);
            }
            cmakelistDocument.setText(updatedText);
        });
    }

    private void updateAddExecutable(ActionProperty actionProperty) {
        String runtimeDir = "";
        StringBuilder updatedDocument = new StringBuilder();
        String exeName = actionProperty.getExecutable();
        Document cmakelistDocument = actionProperty.getCmakeDocument();

        String regex = "^add_executable\\s*?\\(\\s*?" + exeName + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);

        String regex2 = "^set_target_properties\\s*?\\(\\s*?" + exeName + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern2 = Pattern.compile(regex2);

        Scanner scanner = new Scanner(cmakelistDocument.getText());

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = pattern.matcher(line);
            Matcher m2 = pattern2.matcher(line);
            if (m2.find()) {
                continue;
            }
            if (m.find()) {
                line = m.replaceFirst(constructAddExecutable(actionProperty));
                if (runtimeDir != null && !runtimeDir.equals("")) {
                    String outputDir = quoteString(buildOutputDirectory(actionProperty));
                    line += "\n" + constructSetTargetProperties(exeName, outputDir);
                }
            }
            updatedDocument.append(line).append('\n');
        }
        scanner.close();
        final String updatedText = updatedDocument.toString();
        ApplicationManager.getApplication().runWriteAction(() -> cmakelistDocument.setText(updatedText));
    }

    /**
     * Create a cmake valid command to add a new executable
     *
     * @return the possible new executable
     */
    private String constructAddExecutable(ActionProperty actionProperty) {
        return "add_executable(" + actionProperty.getFileName() + " " +
                quotingSourcePath(actionProperty.getRelativeSourcePath()) + ")";
    }

    private String constructSetTargetProperties(String executableName, String outputDirectory) {
        return "set_target_properties(" + executableName + " PROPERTIES RUNTIME_OUTPUT_DIRECTORY " + outputDirectory + ")";
    }

    /**
     * Build the name of the new executable with help of the chose source file.
     * <p>
     * The extension of the source file will be simply removed
     * #
     *
     * @return the new executable's name
     */
    private String buildExecutableName(ActionProperty actionProperty) {
        return actionProperty.getTargetedSourceFile().getName().replace(ExecutableState.EXECUTABLE_NAME_FILENAME,
                actionProperty.getTargetedSourceFile().getNameWithoutExtension());
    }

    private String buildOutputDirectory(ActionProperty actionProperty) {
        String newOutPutDirectory = "";
        String sourceDirRelativePath = new File(Objects.requireNonNull(actionProperty.getProject().getBasePath())).toURI().relativize(
                new File(actionProperty.getTargetedSourceFile().getPath()).getParentFile().toURI()).getPath();

        newOutPutDirectory = newOutPutDirectory.replace(ExecutableState.PROJECT_DIR, "${PROJECT_SOURCE_DIR}");
        newOutPutDirectory = newOutPutDirectory.replace(ExecutableState.FILE_DIR, "${CMAKE_CURRENT_SOURCE_DIR}/" + sourceDirRelativePath);
        return newOutPutDirectory;
    }

    /**
     * Simply quote the source path of a file
     *
     * @param path path of the source code
     * @return a quoted source path
     */
    private String quotingSourcePath(String path) {
        String quotedPath = path;
        if (path.contains(" ") || path.contains("(") || path.contains(")")) {
            quotedPath = '"' + quotedPath + '"';
        }
        return quotedPath;
    }

    private String quoteString(String str) {
        return '"' + str + '"';
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);

        anActionEvent.getPresentation().setVisible((project != null && editor != null));
    }
}
