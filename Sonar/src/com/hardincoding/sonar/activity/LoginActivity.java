package com.hardincoding.sonar.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hardincoding.sonar.R;
import com.hardincoding.sonar.subsonic.service.SubsonicMusicService;
import com.hardincoding.sonar.util.ConnectionErrorDialog;
import com.hardincoding.sonar.util.ModalBackgroundTask;
import com.hardincoding.sonar.util.ProgressListener;
import com.hardincoding.sonar.util.ToastProgressListener;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_USERNAME = "com.hardincoding.sonar.extra.USERNAME";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mServer;
	private String mUsername;
	private String mPassword;

	// UI references.
	private EditText mServerView;
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		// Set up the login form.
		mServerView = (EditText) findViewById(R.id.server);
		
		mUsername = getIntent().getStringExtra(EXTRA_USERNAME);
		mUsernameView = (EditText) findViewById(R.id.username);
		mUsernameView.setText(mUsername);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL || id == EditorInfo.IME_ACTION_DONE) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mServerView.setError(null);
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mServer = mServerView.getText().toString();
		mUsername = mUsernameView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid username.
		if (TextUtils.isEmpty(mUsername)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		}

		// Check for a valid server address.
		if (TextUtils.isEmpty(mServer)) {
			mServerView.setError(getString(R.string.error_field_required));
			focusView = mServerView;
			cancel = true;
		} else if (!mServer.contains("http://") && !mServer.contains("https://")) {
			mServerView.setError(getString(R.string.error_invalid_server));
			focusView = mServerView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask(this, false);
			mAuthTask.execute();
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends ModalBackgroundTask<Boolean> {
		
		public UserLoginTask(Activity activity, boolean finishActivityOnCancel) {
			super(activity, finishActivityOnCancel);
		}
		
		@Override
        protected Boolean doInBackground() throws Throwable {
			ProgressListener progressToast = new ToastProgressListener(LoginActivity.this);
			SubsonicMusicService.INSTANCE.setServerAddress(mServer);
			SubsonicMusicService.INSTANCE.setCredentials(mUsername, mPassword);
			SubsonicMusicService.INSTANCE.ping(LoginActivity.this, progressToast);
			return SubsonicMusicService.INSTANCE.isLicenseValid(LoginActivity.this, progressToast);
		}

		@Override
		protected void done(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
				// TODO Store credentials
				finish();
			}
		}

		@Override
		protected void cancel() {
			mAuthTask = null;
			showProgress(false);
		}

        @Override
        protected void error(Throwable error) {
        	mAuthTask = null;
            
        	String msg = getErrorMessage(error);
        	if (msg.equals("Wrong username or password.")) {
        		String txt_error = getString(R.string.error_incorrect_username_password);
        		mUsernameView.setError(txt_error);
        		mPasswordView.setError(txt_error);
        		mUsernameView.requestFocus();
        	} else {
        		if (msg.equals("A network error occurred. Please check the server address or try again later.")) {
        			mServerView.setError(getString(R.string.error_invalid_server));
        		}
        		new ConnectionErrorDialog(LoginActivity.this, msg, false);
        	}
        	
    		showProgress(false);
        }
	}
}
