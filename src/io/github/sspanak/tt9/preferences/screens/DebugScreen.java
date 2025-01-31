package io.github.sspanak.tt9.preferences.screens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.sspanak.tt9.Logger;
import io.github.sspanak.tt9.R;
import io.github.sspanak.tt9.preferences.PreferencesActivity;
import io.github.sspanak.tt9.ui.UI;

public class DebugScreen extends BaseScreenFragment {
	private final static String LOG_TAG = "DebugScreen";
	private final static String DEBUG_LOGS_SWITCH = "pref_enable_debug_logs";
	private final static String SYSTEM_LOGS_SWITCH = "pref_enable_system_logs";
	private final static String LOGS_CONTAINER = "debug_logs_container";

	public DebugScreen() { init(); }
	public DebugScreen(PreferencesActivity activity) { init(activity); }

	@Override protected int getTitle() { return R.string.pref_category_debug_options; }
	@Override protected int getXml() { return R.xml.prefs_screen_debug; }

	@Override
	protected void onCreate() {
		initLogMessagesSwitch();
		initSystemLogsSwitch();
		enableLogsCopy();

		SwitchPreferenceCompat systemLogs = findPreference(SYSTEM_LOGS_SWITCH);
		boolean includeSystemLogs = systemLogs != null && systemLogs.isChecked();
		printLogs(includeSystemLogs);
	}

	private void initLogMessagesSwitch() {
		SwitchPreferenceCompat msgSwitch = findPreference(DEBUG_LOGS_SWITCH);
		if (msgSwitch == null) {
			Logger.w(LOG_TAG, "Debug logs switch not found.");
			return;
		}

		msgSwitch.setChecked(Logger.isDebugLevel());
		msgSwitch.setOnPreferenceChangeListener((Preference p, Object newValue) -> {
			Logger.enableDebugLevel((boolean) newValue);
			return true;
		});
	}

	private void initSystemLogsSwitch() {
		SwitchPreferenceCompat systemLogs = findPreference(SYSTEM_LOGS_SWITCH);
		if (systemLogs == null) {
			Logger.w(LOG_TAG, "System logs switch not found.");
			return;
		}

		systemLogs.setOnPreferenceChangeListener((p, newValue) -> {
			printLogs((boolean) newValue);
			return true;
		});
	}

	private void printLogs(boolean includeSystemLogs) {
		Preference logsContainer = findPreference(LOGS_CONTAINER);
		if (logsContainer == null) {
			Logger.w(LOG_TAG, "Logs container not found. Cannot print logs");
			return;
		}

		StringBuilder log = new StringBuilder();
		try {
				Process process = Runtime.getRuntime().exec("logcat -d -v threadtime io.github.sspanak.tt9:D");
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = bufferedReader.readLine()) != null) {
					if (includeSystemLogs || line.contains(Logger.TAG_PREFIX)) {
						log.append(line).append("\n\n");
					}
				}
		}
		catch (IOException e) {
			log.append("Error getting the logs. ").append(e.getMessage());
		}

		if (log.toString().isEmpty()) {
			log.append("No Logs");
		}

		logsContainer.setSummary(log.toString());
	}

	private void enableLogsCopy() {
		if (activity == null) {
			Logger.w(LOG_TAG, "Activity is missing. Copying the logs will not be possible.");
			return;
		}

		Preference logsContainer = findPreference(LOGS_CONTAINER);
		if (logsContainer == null) {
			Logger.w(LOG_TAG, "Logs container not found. Copying the logs will not be possible.");
			return;
		}

		ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
		logsContainer.setOnPreferenceClickListener((Preference p) -> {
			clipboard.setPrimaryClip(ClipData.newPlainText("TT9 debug log", p.getSummary()));
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
				UI.toast(activity, "Logs copied.");
			}
			return true;
		});
	}
}
