import gearth.extensions.ExtensionInfo;
import gearth.extensions.ThemedExtensionFormCreator;

import java.net.URL;

public class StackDropperLauncher extends ThemedExtensionFormCreator {

    @Override
    protected String getTitle() {
        return "Stack Dropper " + StackDropper.class.getAnnotation(ExtensionInfo.class).Version();
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("fxml/stackdropper.fxml");
    }

    public static void main(String[] args) {
        runExtensionForm(args, StackDropperLauncher.class);
    }
}
