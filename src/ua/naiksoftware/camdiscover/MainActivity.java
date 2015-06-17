package ua.naiksoftware.camdiscover;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	private Discover discover;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		discover = new Discover(this);
	}

	@Override
	public void onBackPressed() {
		if (discover == null || !discover.running()) {
			super.onBackPressed();
		}
	}
}
