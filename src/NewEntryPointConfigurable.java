import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * This ProjectConfigurable class appears on Settings dialog,
 * to let user to configure this plugin's behavior.
 */
class NewEntryPointConfigurable implements SearchableConfigurable {

    private NewEntryPointConfigurableGUI gui;

    private final Project project;

    public NewEntryPointConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Single File Execution Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "preference.NewEntryPointConfigurable";
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.NewEntryPointConfigurable";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        gui = new NewEntryPointConfigurableGUI();
        gui.createUI(project);
        return gui.getRootPanel();
    }

    @Override
    public boolean isModified() {
        return gui.isModified();
    }

    @Override
    public void apply() {
        gui.apply();
    }

    @Override
    public void reset() {
        gui.reset();
    }

    @Override
    public void disposeUIResources() {
        gui = null;
    }
}
