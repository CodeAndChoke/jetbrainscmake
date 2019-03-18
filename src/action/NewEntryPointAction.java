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
    private ExecutableState config;
    private Project project;

    /**
     * User pushed the hot key and action needed to be verified
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        project = event.getRequiredData(CommonDataKeys.PROJECT);

        config = ExecutableState.getInstance(project);

        targetedSourceFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);


        String nearestCmake = Objects.requireNonNull(targetedSourceFile).getParent().getPath() + CMAKE_FILE;
        File cmakeOnCurrentFolder = new File(nearestCmake);
        if(!cmakeOnCurrentFolder.exists()){
            nearestCmake = project.getBasePath() + CMAKE_FILE;
        }

        File appendingCmake = new File(nearestCmake);
        VirtualFile cmakeFile = LocalFileSystem.getInstance().findFileByIoFile(appendingCmake);
        if (cmakeFile == null) {
            Notifications.Bus.notify (
                    new Notification("new_entry_point_action", "Single File Execution Plugin", "Fail to access " + nearestCmake, NotificationType.ERROR)
            );
            return;
        }
        Document cmakelistDocument = FileDocumentManager.getInstance().getDocument(cmakeFile);

        String fileName = targetedSourceFile != null ? targetedSourceFile.getName() : null;  // source file name (but not include path)

        String exeName = buildExecutableName(config.getExecutableName());
        String relativeSourcePath = new File(Objects.requireNonNull(targetedSourceFile.getParent().getPath())).toURI().relativize(new File(targetedSourceFile.getPath()).toURI()).getPath();
        if(!cmakeOnCurrentFolder.exists()){
            relativeSourcePath = new File(Objects.requireNonNull(project.getBasePath())).toURI().relativize(new File(targetedSourceFile.getPath()).toURI()).getPath();
        }

        String regex = "^add_executable\\s*?\\(\\s*?" + exeName + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";

        Pattern pattern = Pattern.compile(regex);

        Scanner scanner = new Scanner(Objects.requireNonNull(cmakelistDocument).getText());
        int exeExistFlag = EXE_NOT_EXIST;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String existingSourceName = m.group(1);
                if (existingSourceName.contains(relativeSourcePath)) {
                    exeExistFlag = EXE_EXIST_SAME_SOURCE;
                } else {
                    exeExistFlag = EXE_EXIST_DIFFERENT_SOURCE;
                }
                break;
            }
        }
        scanner.close();

        switch(exeExistFlag) {
            case EXE_NOT_EXIST:
                insertAddExecutable(cmakelistDocument, exeName, relativeSourcePath);
                Notifications.Bus.notify (
                        new Notification("new_entry_point_action", "New Entry Point Plugin", "add_executable added for " + fileName + ".", NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_SAME_SOURCE:
                Notifications.Bus.notify (
                        new Notification("new_entry_point_action", "New Entry Point Plugin", "add_executable for this source already exists.", NotificationType.INFORMATION)
                );
                break;
            case EXE_EXIST_DIFFERENT_SOURCE:
                int okFlag;
                if (config.isNotShowOverwriteConfirmDialog()) {
                    okFlag = ExeOverwriteConfirmDialog.OK_FLAG_OK;
                } else {
                    okFlag = ExeOverwriteConfirmDialog.show(project);
                }

                if (okFlag == ExeOverwriteConfirmDialog.OK_FLAG_OK) {
                    updateAddExecutable(cmakelistDocument, exeName, relativeSourcePath);
                    Notifications.Bus.notify(
                            new Notification("new_entry_point_action", "New Entry Point Plugin", "add_executable overwritten", NotificationType.INFORMATION)
                    );
                }
                break;
            default:
                break;
        }

    }

    private void insertAddExecutable(final Document cmakelistDocument, final String exeName, final String relativeSourcePath) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            String updatedText = cmakelistDocument.getText();
            updatedText += "\n" + constructAddExecutable(exeName, relativeSourcePath);
            String runtimeDir = config.getRuntimeOutputDirectory();
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
        String runtimeDir = config.getRuntimeOutputDirectory();
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
     * @param fileName name of the new executable
     * @param sourceFilePath name of the file
     * @return the possible new executable
     */
    private String constructAddExecutable(String fileName, String sourceFilePath) {
        return "add_executable("+ fileName + " " + quotingSourcePath(sourceFilePath) +")";
    }

    private String constructSetTargetProperties(String executableName, String outputDirectory) {
        return "set_target_properties(" + executableName + " PROPERTIES RUNTIME_OUTPUT_DIRECTORY " + outputDirectory + ")";
    }

    private String buildExecutableName(String executableName) {
        return executableName.replace(ExecutableState.EXECUTABLE_NAME_FILENAME,
                targetedSourceFile.getNameWithoutExtension());
    }

    private String buildOutputDirectory() {
        String newOutPutDirectory = config.getRuntimeOutputDirectory();
        String sourceDirRelativePath = new File(Objects.requireNonNull(project.getBasePath())).toURI().relativize(
                new File(targetedSourceFile.getPath()).getParentFile().toURI()).getPath();

        newOutPutDirectory = newOutPutDirectory.replace(ExecutableState.PROJECT_DIR, "${PROJECT_SOURCE_DIR}");
        newOutPutDirectory = newOutPutDirectory.replace(ExecutableState.FILE_DIR, "${CMAKE_CURRENT_SOURCE_DIR}/" + sourceDirRelativePath);
        return newOutPutDirectory;
    }

    /**
     * Simply quote the source path of a file
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
