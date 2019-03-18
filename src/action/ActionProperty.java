package action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import config.ExecutableState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.intellij.openapi.editor.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class ActionProperty {
    private Project project;
    private VirtualFile targetedSourceFile;
    private boolean cmakeOnCurrentFolderFound = true;
    private VirtualFile cmakeFile;
    private Document cmakeDocument;
    private String executable;
    private String relativeSourcePath;
    private String fileName;
    private ExecutableState executableState;
    private int executableExists;
}
