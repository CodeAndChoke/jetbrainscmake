package action;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

class ActionProperty {
    private Project project;
    private VirtualFile targetedSourceFile;
    private boolean cmakeOnCurrentFolderFound = true;
    private VirtualFile cmakeFile;
    private Document cmakeDocument;
    private String executable;
    private String relativeSourcePath;
    private String fileName;
    private int executableExists;

    public ActionProperty() {
    }

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public VirtualFile getTargetedSourceFile() {
        return targetedSourceFile;
    }

    public void setTargetedSourceFile(final VirtualFile targetedSourceFile) {
        this.targetedSourceFile = targetedSourceFile;
    }

    public boolean isCmakeOnCurrentFolderFound() {
        return cmakeOnCurrentFolderFound;
    }

    public void setCmakeOnCurrentFolderFound(final boolean cmakeOnCurrentFolderFound) {
        this.cmakeOnCurrentFolderFound = cmakeOnCurrentFolderFound;
    }

    public VirtualFile getCmakeFile() {
        return cmakeFile;
    }

    public void setCmakeFile(final VirtualFile cmakeFile) {
        this.cmakeFile = cmakeFile;
    }

    public Document getCmakeDocument() {
        return cmakeDocument;
    }

    public void setCmakeDocument(final Document cmakeDocument) {
        this.cmakeDocument = cmakeDocument;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(final String executable) {
        this.executable = executable;
    }

    public String getRelativeSourcePath() {
        return relativeSourcePath;
    }

    public void setRelativeSourcePath(final String relativeSourcePath) {
        this.relativeSourcePath = relativeSourcePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public int getExecutableExists() {
        return executableExists;
    }

    public void setExecutableExists(final int executableExists) {
        this.executableExists = executableExists;
    }
}
