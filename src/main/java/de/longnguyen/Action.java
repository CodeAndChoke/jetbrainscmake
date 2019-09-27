package de.longnguyen;

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
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;


public class Action extends AnAction {
    private static final String CMAKE_FILE = "/CMakeLists.txt";

    /**
     * User pushed the hot key and action needed to be verified
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        VirtualFile targetedSourceFile = (event.getData(PlatformDataKeys.VIRTUAL_FILE));

        Project project = event.getProject();
        if (targetedSourceFile != null && project != null) {
            try {
                VirtualFile cmakeFile = getCmakeFile(targetedSourceFile, project);
                Document cmakeDocument = FileDocumentManager.getInstance().getDocument(cmakeFile);
                if (cmakeDocument != null) {
                    String sourceFilePath =
                            new File(Objects.requireNonNull(project.getBasePath()))
                                    .toURI()
                                    .relativize(new File(targetedSourceFile.getPath())
                                            .toURI()
                                    ).getPath();

                    processCmakeFile(cmakeDocument, sourceFilePath);
                }
            } catch (Exception e) {
                Notifications.Bus.notify(new Notification(
                        "new_executable_action",
                        "New Executable Plugin",
                        "An error happened. Fail to access the nearest CMakelists.txt",
                        NotificationType.ERROR));
            }
        }
    }

    private VirtualFile getCmakeFile(VirtualFile targetedSourceFile, Project project) {
        String nearestCmake = Objects.requireNonNull(targetedSourceFile).getParent().getPath() + CMAKE_FILE;
        VirtualFile cmakeFile = LocalFileSystem.getInstance().findFileByIoFile(new File(nearestCmake));
        if (cmakeFile == null) {
            nearestCmake = project.getBasePath() + CMAKE_FILE;
            cmakeFile = LocalFileSystem.getInstance().findFileByIoFile(new File(nearestCmake));
            if (cmakeFile == null) {
                throw new IllegalArgumentException();
            }
        }
        return cmakeFile;
    }

    private void processCmakeFile(Document cmakeDocument, String sourceFilePath){
        ApplicationManager.getApplication().runWriteAction(() -> {
            String currentText = cmakeDocument.getText();

            String executable = FilenameUtils.removeExtension(sourceFilePath)
                    .replace("/", "_")
                    .replace("\\", "_");

            String newLine = "add_executable(" + executable + " " + sourceFilePath + ")";

            if (!currentText.contains(newLine)) {
                cmakeDocument.setText(currentText + "\n" + newLine);
                Notifications.Bus.notify(
                        new Notification("new_executable_action",
                                "New Entry Point Plugin",
                                "add_executable added for " + sourceFilePath + ".",
                                NotificationType.INFORMATION)
                );
            } else {
                Notifications.Bus.notify(
                        new Notification("new_executable_action",
                                "New Entry Point Plugin",
                                "Executable already existed.",
                                NotificationType.INFORMATION)
                );
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        final Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        anActionEvent.getPresentation().setVisible((project != null && editor != null));
    }
}
