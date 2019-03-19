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
    private static final String EXECUTABLE_NAME_FILENAME = "%FILENAME%";
    private VirtualFile targetedSourceFile;
    private boolean cmakeOnCurrentFolderFound = true;
    private Document cmakeDocument;
    private String executable;
    private String relativeSourcePath;
    private String fileName;
    private int executableExists;

    /**
     * User pushed the hot key and action needed to be verified
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // First step: Process the action event and find out if the project has any CMake file
        if (!processEvent(event)) {
            return;
        }

        // Second step: Extract the file name of the targeted source file
        if (targetedSourceFile == null) {
            fileName = null;
        } else {
            fileName = targetedSourceFile.getName();
        }

        // Third step: Build the name of the new executable
        executable = buildExecutableName();

        // Fourth step: Get the relative source path of the found cmake file
        if (!cmakeOnCurrentFolderFound) {
            relativeSourcePath = targetedSourceFile.getNameWithoutExtension();
        } else {
            relativeSourcePath = getRelativeSourcePath();
        }


        // Fifth step: Process the content of the cmake file
        processCMakeFile();

        finishEvent();
    }

    /**
     * First step: Process the incoming event and return the name of the nearest cmake file.
     * <p>
     * If none CMake file exists, a null will be returned
     *
     * @param event incoming event
     * @return name of the nearest cmake file
     */
    private boolean processEvent(AnActionEvent event) {
        final Project project = (event.getRequiredData(CommonDataKeys.PROJECT));
        targetedSourceFile = (event.getData(PlatformDataKeys.VIRTUAL_FILE));
        String nearestCmake = Objects.requireNonNull(targetedSourceFile).
                getParent().getPath() + CMAKE_FILE;

        File cmakeOnCurrentFolder = new File(nearestCmake);
        if (!cmakeOnCurrentFolder.exists()) {
            nearestCmake = project.getBasePath() + CMAKE_FILE;
            cmakeOnCurrentFolderFound = false;
        } else {
            cmakeOnCurrentFolderFound = true;
        }

        final VirtualFile cmakeFile = LocalFileSystem.getInstance().findFileByIoFile(new File(nearestCmake));
        if (cmakeFile == null) {
            Notifications.Bus.notify(new Notification(
                    "new_executable_action",
                    "New Executable Plugin",
                    "An error happened. Fail to access the nearest CMakelists.txt",
                    NotificationType.ERROR));
            return false;
        }
        cmakeDocument = FileDocumentManager.getInstance().getDocument(cmakeFile);
        return true;
    }

    /**
     * Third step: Build the name of the new executable with help of the chose source file.
     * <p>
     * The extension of the source file will be simply removed
     *
     * @return the new executable's name
     */
    private String buildExecutableName() {
        return targetedSourceFile.getName().replace(EXECUTABLE_NAME_FILENAME, targetedSourceFile.getNameWithoutExtension());
    }

    /**
     * Fourth step: Get the relative source path of the found cmake file
     *
     * @return the path from the project's root to the targeted source file
     */
    private String getRelativeSourcePath() {
        return new File(Objects.requireNonNull(targetedSourceFile.getParent().getPath())).toURI().relativize(new File(targetedSourceFile.getPath()).toURI()).getPath();
    }

    /**
     * Fifth step: Process the content of the cmake file
     * <p>
     * Process the text of the input cmake file and find out if the executable name already exists
     */
    private void processCMakeFile() {
        String regex = "^add_executable\\s*?\\(\\s*?" + executable + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        String CMakeText = cmakeDocument.getText();
        Scanner scanner = new Scanner(CMakeText);
        executableExists = EXE_NOT_EXIST;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String existingSourceName = matcher.group(1);
                if (existingSourceName.contains(relativeSourcePath)) {
                    executableExists = EXE_EXIST_SAME_SOURCE;
                } else {
                    executableExists = EXE_EXIST_DIFFERENT_SOURCE;
                }
                break;
            }
        }
        scanner.close();
    }

    /**
     * Sixth step: Insert the executable
     */
    private void insertAddExecutable() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            String updatedText = cmakeDocument.getText();
            updatedText += "\n" + "add_executable(" + fileName + " " +
                    quotingSourcePath(relativeSourcePath) + ")";
            cmakeDocument.setText(updatedText);
        });
    }

    /**
     * After every is verfied, this method will execute the calculated result
     */
    private void finishEvent() {
        switch (executableExists) {
            case EXE_NOT_EXIST:
                insertAddExecutable();
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
                updateAddExecutable();
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

    private void updateAddExecutable() {
        StringBuilder updatedDocument = new StringBuilder();

        String regex = "^add_executable\\s*?\\(\\s*?" + executable + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);

        String regex2 = "^set_target_properties\\s*?\\(\\s*?" + executable + "\\s+(((\\S+)\\s+)*\\S+)\\s*\\)";
        Pattern pattern2 = Pattern.compile(regex2);

        Scanner scanner = new Scanner(cmakeDocument.getText());

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = pattern.matcher(line);
            Matcher m2 = pattern2.matcher(line);
            if (m2.find()) {
                continue;
            }
            if (m.find()) {
                line = m.replaceFirst(constructAddExecutable());
            }
            updatedDocument.append(line).append('\n');
        }
        scanner.close();
        final String updatedText = updatedDocument.toString();
        ApplicationManager.getApplication().runWriteAction(() -> cmakeDocument.setText(updatedText));
    }

    /**
     * Create a cmake valid command to add a new executable
     *
     * @return the possible new executable
     */
    private String constructAddExecutable() {
        return "add_executable(" + fileName + " " +
                quotingSourcePath(relativeSourcePath) + ")";
    }

    /**
     * Simply quote the source path of a file
     *
     * @param path path of the source code
     * @return a quoted source path
     */
    private static String quotingSourcePath(String path) {
        String quotedPath = path;
        if (path.contains(" ") || path.contains("(") || path.contains(")")) {
            quotedPath = '"' + quotedPath + '"';
        }
        return quotedPath;
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);

        anActionEvent.getPresentation().setVisible((project != null && editor != null));
    }
}
