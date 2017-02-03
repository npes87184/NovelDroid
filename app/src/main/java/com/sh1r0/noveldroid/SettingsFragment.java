package com.sh1r0.noveldroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingsFragment extends PreferenceFragmentCompat implements
	SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String KEY_ENCODING = "encoding";
	private static final String KEY_NAMING_RULE = "naming_rule";
	private static final String KEY_NAMING_RULE_PREVIEW = "naming_rule_preivew";
	private static final String KEY_DOWN_DIR = "down_dir";
	public static final String KEY_SDCARD_URI = "sdcard_uri";
	private static final String KEY_CHECK_UPDATE = "check_update";
	private static final String KEY_ABOUT = "about";
	private static final int FILE_CODE = 0;
	private static final int REQUEST_CODE_STORAGE_ACCESS = 1;

	private Preference encoding;
	private Preference namingRule;
	private Preference downDir;
	private Preference checkUpdate;
	private Preference about;
	private SharedPreferences prefs;
	private String versionName;

	private DownloadManager manager;
	private BroadcastReceiver receiver;
	private DownloadManager.Request request;
	private long downloadId;

	@Override
	public void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		addPreferencesFromResource(R.xml.settings);
		prefs = getPreferenceManager().getSharedPreferences();
		prefs.registerOnSharedPreferenceChangeListener(this);

		try {
			versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		encoding = findPreference(KEY_ENCODING);
		namingRule = findPreference(KEY_NAMING_RULE);
		namingRule.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				LayoutInflater factory = LayoutInflater.from(getActivity());
				final View dialog_view = factory.inflate(R.layout.naming_rule_dialog,null);
				final EditText editText;
				final TextView previewTextView;
				previewTextView = (TextView) dialog_view.findViewById(R.id.tv_preview_string);
				editText =  (EditText) dialog_view.findViewById(R.id.et_naming_rule);
				editText.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable s) {
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						String userDefinedName = s.toString();
						String previewString = toPreviewFileNameString(userDefinedName);
						previewTextView.setText(previewString);
					}
				});
				editText.setText(prefs.getString(KEY_NAMING_RULE, NovelUtils.DEFAULT_NAMING_RULE));
				AlertDialog.Builder editDialog = new AlertDialog.Builder(getActivity());
				editDialog.setTitle(getResources().getString(R.string.filename_rule_dialog_title));
				editDialog.setView(dialog_view);
				editDialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					// do something when the button is clicked
					public void onClick(DialogInterface arg0, int arg1) {
						String userDefinedName = editText.getText().toString();
						String previewString = toPreviewFileNameString(userDefinedName);
						if(isFilenameValid(previewString) && !previewString.equals("")) {
							prefs.edit().putString(KEY_NAMING_RULE_PREVIEW, previewString).commit();
							namingRule.getSharedPreferences().edit().putString(KEY_NAMING_RULE, userDefinedName).commit();
						} else {
							// illegal file name
							Toast.makeText(getActivity(), getString(R.string.illegalName), Toast.LENGTH_LONG).show();
						}
					}
				});
				editDialog.show();
				return true;
			}
		});


		downDir = findPreference(KEY_DOWN_DIR);
		downDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(getActivity(), FilePickerActivity.class);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
				i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefs.getString(KEY_DOWN_DIR, NovelUtils.APP_DIR));

				startActivityForResult(i, FILE_CODE);
				return true;
			}
		});

		checkUpdate = findPreference(KEY_CHECK_UPDATE);
		checkUpdate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				UpdateChecker updateChecker = new UpdateChecker(versionName);
				updateChecker.execute("sh1r0/NovelDroid");
				return true;
			}
		});

		about = findPreference(KEY_ABOUT);
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				final FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(android.R.id.content, AboutFragment.newInstance(versionName));
				ft.addToBackStack(null);
				ft.commit();
				return true;
			}
		});

		manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
					DownloadManager.Query query = new DownloadManager.Query();
					query.setFilterById(downloadId);
					Cursor c = manager.query(query);
					if (c.moveToFirst()) {
						int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
							String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setDataAndType(Uri.parse(uriString), "application/vnd.android.package-archive");
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(i);
						}
					}
				}
			}
		};
	}

	private boolean isFilenameValid(String fileName) {
		File f = new File(fileName);
		try {
			return f.getCanonicalFile().getName().equals(fileName);
		} catch (IOException e) {
			return false;
		}
	}

	private String toPreviewFileNameString(String format) {
		String novelName = getString(R.string.bookname);
		String authorName = getString(R.string.author);
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String strDate = sdFormat.format(date);

		SimpleDateFormat yyyyFormat = new SimpleDateFormat("yyyy");
		String yyyyDate = yyyyFormat.format(date);

		SimpleDateFormat mmFormat = new SimpleDateFormat("MM");
		String mmDate = mmFormat.format(date);

		SimpleDateFormat ddFormat = new SimpleDateFormat("dd");
		String ddDate = ddFormat.format(date);

		// /n = bookname, /a = author, /t = time, /y = year, /m = month and /d = day
		String filename = format.replace("/n", novelName);
		filename = filename.replace("/a", authorName);
		filename = filename.replace("/t", strDate);
		filename = filename.replace("/y", yyyyDate);
		filename = filename.replace("/m", mmDate);
		filename = filename.replace("/d", ddDate);

		return filename;
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {

	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().setTitle(R.string.settings);
		encoding.setSummary(prefs.getString(KEY_ENCODING, "UTF-8"));
		namingRule.setSummary(prefs.getString(KEY_NAMING_RULE_PREVIEW, toPreviewFileNameString(NovelUtils.DEFAULT_NAMING_RULE)));
		downDir.setSummary(prefs.getString(KEY_DOWN_DIR, NovelUtils.APP_DIR));
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	@Override
	public void onPause() {
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		getActivity().unregisterReceiver(this.receiver);
		super.onPause();
	}

	@Override
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Uri sdCardUri;

			try {
				sdCardUri = Uri.parse(prefs.getString(KEY_SDCARD_URI, null));
			} catch (Exception e) {
				sdCardUri = null;
			}
			File testFile = new File(uri.getPath());
			FileUtils fileUtils = new FileUtils(this.getContext());

			if (fileUtils.isOnExtSdCard(testFile)) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !fileUtils.isWritableNormalOrSaf(testFile, sdCardUri)) {
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.oops)
							.setMessage(R.string.oops_sdcard_detail)
							.setCancelable(false)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// trigger storage access framework.
									Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
									startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
								}
							})
							.show();
				} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !FileUtils.isWritableNormal(testFile)) {
					// The android version is kitkat and can not write to it.
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.oops)
							.setMessage(R.string.oops_illegalOutput_detail)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// do nothing.
								}
							})
							.show();
					return;
				}
			}
			downDir.getSharedPreferences().edit().putString(KEY_DOWN_DIR, uri.getPath() + "/").commit();
		}

		if (requestCode == REQUEST_CODE_STORAGE_ACCESS) {
			Uri treeUri = null;
			if (resultCode == Activity.RESULT_OK) {
				// Get Uri from Storage Access Framework.
				treeUri = data.getData();

				// Persist URI in shared preference so that you can use it later.
				prefs.edit().putString(KEY_SDCARD_URI, treeUri.toString()).commit();

				// Persist access permissions.
				getActivity().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
						Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		switch (key) {
			case KEY_ENCODING:
				encoding.setSummary(sharedPreferences.getString(KEY_ENCODING, "UTF-8"));
				break;
			case KEY_NAMING_RULE:
				namingRule.setSummary(prefs.getString(KEY_NAMING_RULE_PREVIEW, toPreviewFileNameString(NovelUtils.DEFAULT_NAMING_RULE)));
				break;
			case KEY_DOWN_DIR:
				downDir.setSummary(sharedPreferences.getString(KEY_DOWN_DIR, NovelUtils.APP_DIR));
				break;
		}
	}

	private class UpdateChecker extends AsyncTask<String, Integer, Pair<String, URL>> {
		private ProgressDialog progressDialog;
		private String currentVersion;
		private File apkPath;

		public UpdateChecker(String versionName) {
			super();
			this.currentVersion = versionName;
		}

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(getActivity(),
					getResources().getString(R.string.check_update), getResources().getString(R.string.checking));
		}

		@Override
		protected Pair<String, URL> doInBackground(String... repos) {
			ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(
					Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
			if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
				return null;
			}

			String repo = repos[0];
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(String.format("https://api.github.com/repos/%s/releases", repo));
			HttpResponse response;
			try {
				response = client.execute(get);
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf8"), 4096);

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				is.close();

				JSONArray releases = new JSONArray(sb.toString());
				JSONObject latestRelease = releases.getJSONObject(0);
				String latestVersion = latestRelease.getString("tag_name");
				URL downloadURL = new URL(latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"));

				File tempDir = new File(NovelUtils.TEMP_DIR);
				if (!tempDir.exists()) {
					tempDir.mkdirs();
				}
				this.apkPath = new File(tempDir, "noveldroid.apk");

				return new Pair<>(latestVersion, downloadURL);
			} catch (IOException | JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(final Pair<String, URL> versionPair) {
			progressDialog.dismiss();

			if (versionPair == null) {
				Toast.makeText(getActivity(), R.string.check_update_fail_tooltip, Toast.LENGTH_LONG).show();
				return;
			}

			AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
			dialog.setCancelable(false);

			if (NovelUtils.versionCompare(currentVersion, versionPair.first) >= 0) {
				dialog.setMessage(R.string.current_is_latest);
				dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
			} else {
				String msg = getString(R.string.latest_version) + versionPair.first + "\n"
						+ getString(R.string.current_version) + currentVersion;
				dialog.setTitle(R.string.found_update).setMessage(msg);
				dialog.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						request = new DownloadManager.Request(Uri.parse(versionPair.second.toString()));
						request.setDestinationUri(Uri.fromFile(apkPath));
						request.setMimeType("application/vnd.android.package-archive");
						downloadId = manager.enqueue(request);
					}
				});
			}
			dialog.show();
		}
	}
}
