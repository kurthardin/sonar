package com.hardincoding.sonar.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.domain.Station;
import com.hardincoding.sonar.subsonic.service.CachedMusicService;

public class CreateStationActivity extends Activity {
	
	private String mArtist;
	private EditText mArtistView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_station);
		
		mArtistView = (EditText) findViewById(R.id.station_artist);
		mArtistView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.create_station || id == EditorInfo.IME_NULL || id == EditorInfo.IME_ACTION_DONE) {
							createStation();
							return true;
						}
						return false;
					}
				});
		
		findViewById(R.id.create_station_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						createStation();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void createStation() {
		mArtist = mArtistView.getText().toString();
		
		if (mArtist.isEmpty()) {
			mArtistView.setError(getString(R.string.error_field_required));
			mArtistView.requestFocus();
			return;
		}
		
		CachedMusicService.INSTANCE.getStations(this).add(new Station(mArtist));
		CachedMusicService.INSTANCE.writeStations(this);
		finish();
	}

}
