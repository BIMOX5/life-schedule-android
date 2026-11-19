package lt.lifeschedule.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(LsWidgetPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
