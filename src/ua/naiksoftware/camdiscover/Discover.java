package ua.naiksoftware.camdiscover;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Discover {
	
	private Activity activity;
	private ViewGroup layoutForm, layoutResults;

	private EditText etFirstIP;
	private EditText etSecondIP;
	private EditText etRangeFromIP;
	private EditText etRangeToIP;
	private EditText etTimeout;
	private EditText etMaxThreads;
	private EditText etPorts;
	private CheckBox chBoxAnyServers;
	private ListView listView;
	private ArrayAdapter<String> listAdapter;
	private ArrayList<String> list;
	private Button btnDiscover;
	private ProgressBar progressBar;

	private boolean running;
	private boolean anyServers;

	private int connectTimeout = 250;
	private String[] ports = new String[] { "80", "8080" };
	private final List<String> searchHeaders = new ArrayList<String>() {
		{
			add("DNVRS-Webs");
			add("DVRDVS-Webs");
			add("Hikvision-Webs");
			add("App-webs");
			add("Netwave IP Camera");
			add("Webcam");
			add("WebCam");
			add("webcam");
		}
	};
	private int threads = 25;
	private String urlPrefix;
	private int[] range = { 0, 255 };
	private ExecutorService pool;
	private int requestsInThread = 32;
	private final AtomicInteger mainCounter = new AtomicInteger(0);
	private int requests;
	private int progress;

	public Discover(Activity a) {
		list = new ArrayList<String>();
		setActivity(a);
	}

	public void setActivity(Activity a) {
		this.activity = a;
		if (a != null)
			initialize();
	}

	/**
	 * Initializes the class.
	 */
	private void initialize() {
		layoutForm = (ViewGroup) activity.findViewById(R.id.layoutForm);
		layoutResults = (ViewGroup) activity.findViewById(R.id.layoutResults);
		if (running) layoutForm.setVisibility(View.GONE);
		else layoutResults.setVisibility(View.GONE);

		etFirstIP = (EditText) activity.findViewById(R.id.firstIpElem);
		etSecondIP = (EditText) activity.findViewById(R.id.secondIpElem);
		etRangeFromIP = (EditText) activity.findViewById(R.id.fromRange);
		etRangeToIP = (EditText) activity.findViewById(R.id.toRange);
		etTimeout = (EditText) activity.findViewById(R.id.timeout);
		etMaxThreads = (EditText) activity.findViewById(R.id.maxThreads);
		etPorts = (EditText) activity.findViewById(R.id.ports);
		chBoxAnyServers = (CheckBox) activity.findViewById(R.id.anyServers);
		progressBar = (ProgressBar) activity.findViewById(R.id.progressBar);

		listAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, list);
		listView = (ListView) activity.findViewById(R.id.listView);
		listView.setAdapter(listAdapter);

		btnDiscover = (Button) activity.findViewById(R.id.buttonDiscover);
		btnDiscover.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!running) {
					btnDiscover.setText("Force stop");
					backupData();
					start();
				} else {
					pool.shutdownNow();
					btnDiscover.setText("Discover");
				}
				running = !running;
				if (running) {
					layoutForm.setVisibility(View.GONE);
					layoutResults.setVisibility(View.VISIBLE);
				} else {
					layoutForm.setVisibility(View.VISIBLE);
					layoutResults.setVisibility(View.GONE);
				}
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String url_str = list.get(position);
				openBrowser(url_str);
			}
		});
		chBoxAnyServers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						anyServers = isChecked;
					}
				});
		((Button) activity.findViewById(R.id.viewIpRanges)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openBrowser("http://ipgeobase.ru/cgi-bin/AdvSearch.cgi ");
			}
		});
		backupData();
		restoreData();
	}

	public void backupData() {
		String s1 = etFirstIP.getText().toString();
		if (s1.isEmpty()) {
			s1 = "192";
		}
		String s2 = etSecondIP.getText().toString();
		if (s2.isEmpty()) {
			s2 = "168";
		}
		urlPrefix = s1 + "." + s2 + ".";
		s1 = etRangeFromIP.getText().toString();
		if (s1.isEmpty()) {
			s1 = "0";
		}
		s2 = etRangeToIP.getText().toString();
		if (s2.isEmpty()) {
			s2 = "3";
		}
		range[0] = Integer.parseInt(s1);
		range[1] = Integer.parseInt(s2);
		s1 = etMaxThreads.getText().toString();
		if (!s1.isEmpty()) {
			threads = Integer.parseInt(s1);
		}
		s1 = etTimeout.getText().toString();
		if (!s1.isEmpty()) {
			connectTimeout = Integer.parseInt(s1);
		}
		s1 = etPorts.getText().toString();
		if (!s1.isEmpty()) {
			ports = s1.split("\\s+");
		}
		progress = progressBar.getProgress();
	}

	public void restoreData() {
		String[] prefs = urlPrefix.split("\\.");
		etFirstIP.setText(prefs[0]);
		etSecondIP.setText(prefs[1]);
		etRangeFromIP.setText(String.valueOf(range[0]));
		etRangeToIP.setText(String.valueOf(range[1]));
		StringBuilder sb = new StringBuilder();
		for (String p : ports) {
			sb.append(p).append(" ");
		}
		String s = sb.toString();
		etPorts.setText(s.substring(0, s.length() - 1));
		etTimeout.setText(String.valueOf(connectTimeout));
		etMaxThreads.setText(String.valueOf(threads));
		progressBar.setProgress(progress);
		chBoxAnyServers.setChecked(anyServers);
	}

	private void start() {
		listAdapter.clear();
		listAdapter.notifyDataSetChanged();
		mainCounter.set(0);
		requests = ((range[1] - range[0] + 1) * 256);
		progressBar.setMax(requests);
		progressBar.setProgress(0);
		pool = Executors.newFixedThreadPool(threads);
		for (int i = range[0]; i <= range[1]; i++) {
			for (int j = 0; j < 256; j++) {
				if (j % requestsInThread == 0) {
					pool.submit(new DiscoverTask(i, j));
				}
			}
		}
		pool.shutdown();
	}

	private class DiscoverTask implements Runnable {

		private int third, fromFifth;

		public DiscoverTask(int third, int fromFifth) {
			this.third = third;
			this.fromFifth = fromFifth;
		}

		public void run() {
			int num = 0;
			String server;
			for (int i = 0; i <= requestsInThread && fromFifth + i < 256; i++) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				String address = urlPrefix + third + "." + (fromFifth + i);
				String httpAddr = "http://" + address;
				for (int p = 0; p < ports.length; p++) {
					int port = Integer.parseInt(ports[p]);
					if (port == 80) {
						HttpURLConnection connect = null;
						try {
							URL url = new URL(httpAddr);
							connect = (HttpURLConnection) url.openConnection();
							connect.setReadTimeout(connectTimeout);
							connect.setConnectTimeout(connectTimeout);
							connect.connect();
							server = connect.getHeaderField("Server");
							Log.e("CamDiscover", "Url " + url.getHost() + " OK");
							if (server != null) {
								Log.e("CamDiscover", "Server " + server);
								if (anyServers) {
									addLink(httpAddr + " " + server);
								} else if (searchHeaders.contains(server)) {
									addLink(httpAddr + " " + server);
								}
							} else {
								server = connect
										.getHeaderField("WWW-Authenticate");
								if (server != null) {
									if (anyServers) {
										addLink(httpAddr + " WWW-Authenticate");
									} else if (searchHeaders.contains(server)) {
										addLink(httpAddr
												+ " WWW-Authenticate  webcam");
									}
								} else if (anyServers) {
									addLink(httpAddr + " undefined");
								}
							}
						} catch (Exception e) {
							// e.printStackTrace();
						} finally {
							connect.disconnect();
						}
					} else { // Other port, scan via socket
						try {
							InetSocketAddress addr = new InetSocketAddress(
									address, port);
							Socket sock = new Socket();
							sock.connect(addr, connectTimeout);
							addLink(address + ":" + port + readFromSocket(sock));
							sock.close();
						} catch (Exception e) {
							// e.printStackTrace();
						}
					}
				}
				num = mainCounter.incrementAndGet();
				progressBar.setProgress(num);
				if (num == requests) {
					running = false;
					if (activity != null) {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								btnDiscover.setText("Discover");// end
							}
						});
					}
				}
			}
		}

	}

	private static String readFromSocket(Socket sock) throws Exception {
		InputStream in = sock.getInputStream();
		StringBuilder result = new StringBuilder(" ");
		int timeout = 0;
		int a;
		while (true) {
			a = in.available();
			if (a > 0) {
				int r = in.read();
				if (r == -1) {
					break;
				}
				result.append((char) r);
				timeout = 0;
			} else {
				Thread.sleep(10);
				a = in.available();
				if (a == 0) {
					timeout++;
				}
				if (timeout > 40) {
					break; // timeout
				}
			}
		}
		return result.toString();
	}

	private void addLink(final String url) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				listAdapter.add(url);
				listAdapter.notifyDataSetChanged();
			}
		});
	}

	private void openBrowser(final String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url.substring(0, url.indexOf(' '))));
		try {
			activity.startActivity(intent);
		} catch (ActivityNotFoundException ex) {
			Toast.makeText(activity, "Browser not found on this device", Toast.LENGTH_LONG).show();
		}
	}

	public boolean running() {
		return running;
	}
}
