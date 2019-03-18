package config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Singleton configuration of how the plugin works
 *
 * After an event took place, the class will extract the information of the chose
 * source file in order to confirm if the event could take place.
 *
 * This class implements the PersistentStateComponent, which can be used for saving both application-level values
 * and project-level values' state between restarts of the IDE
 */
@State(name="config.ExecutableState", storages = {@Storage("config.ExecutableState.xml")})
@Getter
@Setter
public class ExecutableState implements PersistentStateComponent<ExecutableState> {
    public static final String EXECUTABLE_NAME_FILENAME = "%FILENAME%";
    public static final String PROJECT_DIR = "%PROJECT_DIR%";
    public static final String FILE_DIR = "%FILE_DIR%";

    private String runtimeOutputDirectory = "";
    private boolean notShowOverwriteConfirmDialog = false;
    private String executableName = EXECUTABLE_NAME_FILENAME;

    /**
     * Empty private constructor
     */
    private ExecutableState() { }

    @Nullable
    @Override
    public ExecutableState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ExecutableState executableState) {
        XmlSerializerUtil.copyBean(executableState, this);
    }

    @Nullable
    public static ExecutableState getInstance(Project project) {
        return ServiceManager.getService(project, ExecutableState.class);
    }
}
