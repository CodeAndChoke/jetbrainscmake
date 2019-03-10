import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@State(
        name="SingleFileExecutionConfig",
        storages = {
                @Storage("SingleFileExecutionConfig.xml")}
)
public class SingleFileExecutionConfig implements PersistentStateComponent<SingleFileExecutionConfig> {
    static final String EXECUTABLE_NAME_FILENAME = "%FILENAME%";
    private static final String DEFAULT_EXECUTABLE_NAME = EXECUTABLE_NAME_FILENAME;
    private String executableName = DEFAULT_EXECUTABLE_NAME;
    static final String PROJECT_DIR = "%PROJECT_DIR%";
    static final String FILE_DIR = "%FILE_DIR%";
    private String runtimeOutputDirectory = "";
    private static final boolean DEFAULT_NOT_SHOW_OVERWRITE_CONFIRM_DIALOG = false;
    boolean notShowOverwriteConfirmDialog = DEFAULT_NOT_SHOW_OVERWRITE_CONFIRM_DIALOG;

    private SingleFileExecutionConfig() { }

    String getExecutableName() {
        return Objects.requireNonNull(executableName);
    }

    void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    String getRuntimeOutputDirectory() {
        return runtimeOutputDirectory;
    }

    void setRuntimeOutputDirectory(String runtimeOutputDirectory) {
        this.runtimeOutputDirectory = runtimeOutputDirectory;
    }

    @Nullable
    @Override
    public SingleFileExecutionConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SingleFileExecutionConfig singleFileExecutionConfig) {
        XmlSerializerUtil.copyBean(singleFileExecutionConfig, this);
    }

    @Nullable
    static SingleFileExecutionConfig getInstance(Project project) {
        return ServiceManager.getService(project, SingleFileExecutionConfig.class);
    }
}
