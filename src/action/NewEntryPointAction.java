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
import gui.ExeOverwriteConfirmDialog;
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

    private VirtualFile targetedSourceFile;
    private ExecutableState executableState;
    private Project project;
    private VirtualFile cmakeFile;
    private boolean cmakeOnCurrentFolderFound = true;

    /**
     * User pushed the hot key and action needed to be verified
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String nearestCmake = processEvent(event);
        if (nearestCmake == null) {
            return;
        }

        Document cmakeDocument = FileDocumentManager.getInstance().getDocument(cmakeFile);
        String fileName;
        if (targetedSourceFile == null) {
            fileName = null;
        } else {
            fileName = targetedSourceFile.getName();
        }

        String executable = buildExecutableName(executableState.getExecutableName());

        String relativeSourcePath;

        if (!cmakeOnCurrentFolderFound) {
            relativeSourcePath = targetedSourceFile.getNameWithoutExtension();
        } else {
            relativeSourcePath = getRelativeSourcePath();
        }

        int executableExists = processCMakeFile(Objects.requireNonNull(cmakeDocument).getText(), executable, relativeSourcePath);
        finishEvent(executableExists, cmakeDocument, executable, relativeSourcePath, fileName);
    }

    /**
     * After every is verfied, this method will execute the calculated result
     * @param executableExists flag if the executable's name already exists
     * @param cmakeDocument the found cmake document
     * @param executable name of the created executable
     * @param relativeSourcePath relative path to the chose source file
     * @param fileName soure file's name
     */
    private void finishEvent(int executableExists, Document cmakeDocument, String executable, String relativeSourcePath, String fileName){
        switch (executableExists) {
            case EXE_NOT_EXIST:
                insertAddExecutable(cmakeDocument, executable, relativeSourcePath);
                Notifications.Bus.notify(
                        new Notification("new_entry_point_action",
                                "New Entry Point Plugin",
                                "add_executable added for " + fileName + ".",
                                NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_SAME_SOURCE:
                Notifications.Bus.notify(
                        new Notification("new_entry_point_action",
                                "New Entry Point Plugin",
                                "add_executable for this source already exists.",
                                NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_DIFFERENT_SOURCE:
                int okFlag;
                if (executableState.isNoOverWriteConfirmDialog()) {
                    okFlag = ExeOverwriteConfirmDialog.OK_FLAG_OK;
                } else {
                    okFlag = ExeOverwriteConfirmDialog.show(project);
                }

                if (okFlag == ExeOverwriteConfirmDialog.OK_FLAG_OK) {
                    updateAddExecutable(cmakeDocument, executable, relativeSourcePath);
                    Notifications.Bus.notify(
                            new Notification("new_entry_point_action",
                                    "New Entry Point Plugin",
                                    "add_executable overwritten",
                                    NotificationType.INFORMATION)
                    );
                }
                break;
            default:
                break;
        }
    }

    /**
     * Process the text of the input cmake file and find out if the executable name already exists
     * @param CMakeText text of the found cmake file
     * @param executable executable's name
     * @param relativeSourcePath relative path to the source file
     * @return flag if the the same executable was found
     */
    private int processCMakeFile(@NotNull String CMakeText, String executable, String relativeSourcePath){
        String regex = "^add_executable\\s*?\\(\\s*?" + executable + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        Scanner scanner = new Scanner(CMakeText);
        int executableExists = EXE_NOT_EXIST;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String existingSourceName = m.group(1);
                if (existingSourceName.contains(relativeSourcePath)) {
                    executableExists = EXE_EXIST_SAME_SOURCE;
                } else {
                    executableExists = EXE_EXIST_DIFFERENT_SOURCE;
                }
                break;
            }
        }
        scanner.close();
        return executableExists;
    }

    /**
     * @return the path from the project's root to the targeted source file
     */
    private String getRelativeSourcePath() {
        return new File(Objects.requireNonNull(targetedSourceFile.getParent().getPath())).toURI().relativize(new File(targetedSourceFile.getPath()).toURI()).getPath();
    }

    /**
     * Process the incoming event and return the name of the nearest cmake file
     *
     * @param event incoming event
     * @return name of the nearest cmake file
     */
    private String processEvent(AnActionEvent event) {
        project = event.getRequiredData(CommonDataKeys.PROJECT);
        executableState = ExecutableState.getInstance(project);
        targetedSourceFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        String nearestCmake = Objects.requireNonNull(targetedSourceFile).getParent().getPath() + CMAKE_FILE;

        File cmakeOnCurrentFolder = new File(nearestCmake);
        if (!cmakeOnCurrentFolder.exists()) {
            nearestCmake = project.getBasePath() + CMAKE_FILE;
            cmakeOnCurrentFolderFound = false;
        } else {
            cmakeOnCurrentFolderFound = true;
        }

        File appendingCmake = new File(nearestCmake);
        cmakeFile = LocalFileSystem.getInstance().findFileByIoFile(appendingCmake);
        if (cmakeFile == null) {
            Notifications.Bus.notify(new Notification(
                    "new_entry_point_action",
                    "Single File Execution Plugin",
                    "Fail to access " + nearestCmake,
                    NotificationType.ERROR));
            return null;
        }
        return nearestCmake;
    }

    private void insertAddExecutable(final Document cmakelistDocument, final String exeName, final String relativeSourcePath) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            String updatedText = cmakelistDocument.getText();
            updatedText += "\n" + constructAddExecutable(exeName, relativeSourcePath);
            String runtimeDir = executableState.getRuntimeOutputDirectory();
            if (runtimeDir != null && !runtimeDir.equals("")) {
                String outputDir = quoteString(buildOutputDirectory());
                updatedText += "\n" + constructSetTargetProperties(exeName, outputDir);
            }
            cmakelistDocument.setText(updatedText);
        });
    }

    private void updateAddExecutable(final Document cmakelistDocument,
                                     final String exeName,
                                     final String relativeSourcePath) {
        String runtimeDir = executableState.getRuntimeOutputDirectory();
        StringBuilder updatedDocument = new StringBuilder();

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
                line = m.replaceFirst(constructAddExecutable(exeName, relativeSourcePath));
                if (runtimeDir != null && !runtimeDir.equals("")) {
                    String outputDir = quoteString(buildOutputDirectory());
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
     * @param fileName       name of the new executable
     * @param sourceFilePath name of the file
     * @return the possible new executable
     */
    private String constructAddExecutable(String fileName, String sourceFilePath) {
        return "add_executable(" + fileName + " " + quotingSourcePath(sourceFilePath) + ")";
    }

    private String constructSetTargetProperties(String executableName, String outputDirectory) {
        return "set_target_properties(" + executableName + " PROPERTIES RUNTIME_OUTPUT_DIRECTORY " + outputDirectory + ")";
    }

    /**
     * Build the name of the new executable with help of the chose source file.
     * <p>
     * The extension of the source file will be simply removed
     *
     * @param sourceFile name of the source file
     * @return the new executable's name
     */
    private String buildExecutableName(String sourceFile) {
        return sourceFile.replace(ExecutableState.EXECUTABLE_NAME_FILENAME,
                targetedSourceFile.getNameWithoutExtension());
    }

    private String buildOutputDirectory() {
        String newOutPutDirectory = executableState.getRuntimeOutputDirectory();
        String sourceDirRelativePath = new File(Objects.requireNonNull(project.getBasePath())).toURI().relativize(
                new File(targetedSourceFile.getPath()).getParentFile().toURI()).getPath();

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
