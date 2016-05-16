package com.sam.warrior;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;





public class MainActivity extends Activity implements OnClickListener {

	private AdView adView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create the adView.
		adView = new AdView(this);
		adView.setAdUnitId("a152f5ba4e45dae");
		adView.setAdSize(AdSize.BANNER);

		// Lookup your LinearLayout assuming it's been given
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.main);

		// Add the adView to it.
		layout.addView(adView);

		// Initiate a generic request.
		AdRequest adRequest = new AdRequest.Builder().build();

		// Load the adView with the ad request.
		adView.loadAd(adRequest);

		View NewGameButton = findViewById(R.id.new_game);
		NewGameButton.setOnClickListener(this);

		View Instructions = findViewById(R.id.instructions);
		Instructions.setOnClickListener(this);

		View About = findViewById(R.id.about);
		About.setOnClickListener(this);

		View Exit = findViewById(R.id.exit);
		Exit.setOnClickListener(this);
		
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
			break;

		case R.id.new_game:
			Intent i2 = new Intent(this, NewGame.class);
			startActivity(i2);
			break;

		case R.id.instructions:
			Intent i3 = new Intent(this, Instructions.class);
			startActivity(i3);
			break;

		case R.id.exit:
			Intent i4 = new Intent(this, Settings.class);
			startActivity(i4);

			break;

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, Settings.class);
			startActivity(i);

		}

		return true;
	}
}