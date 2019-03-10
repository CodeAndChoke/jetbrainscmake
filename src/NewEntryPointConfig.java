import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Singleton configures how the plugin works
 */
@State(name="NewEntryPointConfig", storages = {@Storage("NewEntryPointConfig.xml")})
public class NewEntryPointConfig implements PersistentStateComponent<NewEntryPointConfig> {
    static final String EXECUTABLE_NAME_FILENAME = "%FILENAME%";
    private static final String DEFAULT_EXECUTABLE_NAME = EXECUTABLE_NAME_FILENAME;
    private String executableName = DEFAULT_EXECUTABLE_NAME;
    static final String PROJECT_DIR = "%PROJECT_DIR%";
    static final String FILE_DIR = "%FILE_DIR%";
    private String runtimeOutputDirectory = "";
    private static final boolean DEFAULT_NOT_SHOW_OVERWRITE_CONFIRM_DIALOG = false;
    boolean notShowOverwriteConfirmDialog = DEFAULT_NOT_SHOW_OVERWRITE_CONFIRM_DIALOG;

    private NewEntryPointConfig() { }

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
    public NewEntryPointConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NewEntryPointConfig newEntryPointConfig) {
        XmlSerializerUtil.copyBean(newEntryPointConfig, this);
    }

    @Nullable
    static NewEntryPointConfig getInstance(Project project) {
        return ServiceManager.getService(project, NewEntryPointConfig.class);
    }
}
