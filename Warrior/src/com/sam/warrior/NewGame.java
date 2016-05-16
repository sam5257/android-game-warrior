package com.sam.warrior;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class NewGame extends Activity {
	private RenderView renderView;
	private volatile boolean running = false;
	public boolean sound;
	public boolean gameOver = false;
	private boolean flag = true;
	private WakeLock wakeLock;
	private String globalHighestScore = "N.A";
	private String globalHighestScorer = "Not Connected";
	private String name;
	private SharedPreferences prefs;
	private AudioManager am;
	private int firstlaunch;
	private HttpClient client;
	private int score = 0;
	private Context context = this;
	private boolean internetAvailable = false;
	public boolean isScoredGlobalHighest = false;
	private boolean touch = false;
	private AdView adView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		firstlaunch = prefs.getInt("l", 0);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);
		renderView = new RenderView(this);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		// Create the adView.
		adView = new AdView(this);
		adView.setAdUnitId("a152f5ba4e45dae");
		adView.setAdSize(AdSize.BANNER);
		// Initiate a generic request.
		AdRequest adRequest = new AdRequest.Builder().build();

		// Load the adView with the ad request.
		adView.loadAd(adRequest);

		adView.setLayoutParams(lp);
		RelativeLayout layout = new RelativeLayout(this);

		layout.addView(renderView);
		// Add the adView to it.
		layout.addView(adView);

		setContentView(layout);
	}

	public static DefaultHttpClient getThreadSafeClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);

		return client;
	}

	public boolean getConnectivityStatus(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}

		}
		return false;
	}

	protected void onResume() {
		super.onResume();
		renderView.resume();

		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		sound = Settings.getMusic(this);
		if (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT
				|| am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)
			sound = false;

		internetAvailable = getConnectivityStatus(context);
		if (internetAvailable) {
			new LoadHighestScore().execute("");
			globalHighestScorer = "Loading...";
		}
	}

	protected void onPause() {
		super.onPause();
		renderView.pause();
		try {
			this.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	@Override
	public void onBackPressed() {

		this.onPause();
		showDialog(0);

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case 0:
			return new AlertDialog.Builder(this)
					.setTitle("Are you sure you want to quit?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									finish();
								}
							})

					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									onResume();
								}
							})
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									touch = true;
									onResume();

								}
							}).create();

		case 1:
			return new AlertDialog.Builder(this)
					.setTitle("TILT to MOVE. TOUCH to FIRE")
					.setPositiveButton("Got it !!",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Editor editor = prefs.edit();
									editor.putInt("l", 1);
									editor.commit();
									firstlaunch = 1;
									onResume();
								}
							})

					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									onResume();

								}
							}).create();

		case 2:
			// get prompts.xml view
			LayoutInflater li = LayoutInflater.from(this);
			View promptsView = li.inflate(R.layout.promptview, null);

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					context);

			// set prompts.xml to alertdialog builder
			alertDialogBuilder.setView(promptsView);

			final EditText userInput = (EditText) promptsView
					.findViewById(R.id.editTextDialogUserInput);

			// set dialog message
			alertDialogBuilder.setOnCancelListener(
					new DialogInterface.OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {

							isScoredGlobalHighest = false;
							onResume();

						}
					}).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}

					});

			// create alert dialog
			final AlertDialog dialog = alertDialogBuilder.create();
			dialog.show();
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
					new View.OnClickListener() {
						private boolean callPost;

						@Override
						public void onClick(View v) {

							callPost = true;

							name = userInput.getText().toString().trim();
							if (name.length() == 0) {

								Toast.makeText(getApplicationContext(),
										"Please enter name", Toast.LENGTH_SHORT)
										.show();

								callPost = false;

							}
							if (name.length() > 15) {

								Toast.makeText(getApplicationContext(),
										"Only 15 characters are allowed",
										Toast.LENGTH_SHORT).show();

								callPost = false;

							}

							for (String str : bannedWords) {
								if (name.contains(str)) {

									Toast.makeText(getApplicationContext(),
											"Invalid name", Toast.LENGTH_SHORT)
											.show();

									callPost = false;
								}

							}

							if (callPost == true) {

								dialog.dismiss();
								isScoredGlobalHighest = false;
								new HighestScore().execute(userInput.getText()
										.toString());

							}

						}
					});
			return dialog;

		}
		return null;
	}

	class RenderView extends SurfaceView implements Runnable, OnTouchListener,
			SensorEventListener {
		private Thread renderThread;
		private SurfaceHolder holder;
		private Paint paint;
		private Paint mPaint;
		private AssetManager assetManager;
		private InputStream inputStream;
		private Bitmap imgTank;
		private Bitmap imgTank1;
		private Bitmap imgBullet;
		private Bitmap imgEnemy;
		private int frameBufferWidth;
		private int frameBufferHeight;
		private int width;
		private int height;
		private float scaleX;
		private float scaleY;
		private Rect dst = new Rect();
		private int x1 = 250, y1 = 950;
		private int x2 = 900, y2 = 950;
		private int x3 = 100, y3;
		private boolean fire = false;
		private int x;
		private int y;
		private int w;
		private int h;
		private Bitmap frameBuffer;
		private Canvas canvas;
		private Canvas mCanvas;
		private Bitmap enemy;
		private Bitmap enemy1;
		private Bitmap enemy2;
		private Bitmap explosion[] = new Bitmap[12];
		private Bitmap happy;
		private Bitmap sad;
		private boolean taEnCol = false;
		private boolean buEnCol = true;
		private boolean start;
		private int i;
		private int vel = 8;

		private int k;
		private SensorManager manager;
		private SoundPool soundPool;
		private AssetFileDescriptor descriptor;
		private Typeface font;
		private int sound1;
		private int sound2;
		private int sound3;
		private Sensor accelerometer;
		private Rect rect1;
		private Rect rect2;
		private Rect rect3;
		private Rect rect4;
		private MotionEvent event;

		public int highestScore;
		public int whichEnemy;

		private boolean highest = false;
		private PowerManager powerManager;
		private int numStars;
		private Point[] stars;
		private int delay = 0;
		private int which;

		@SuppressWarnings("deprecation")
		public RenderView(Context context) {
			super(context);

			holder = getHolder();

			this.setOnTouchListener(this);

			assetManager = getAssets();

			inputStream = null;
			frameBufferWidth = 800;
			frameBufferHeight = 1200;
			width = getWindowManager().getDefaultDisplay().getWidth();
			height = getWindowManager().getDefaultDisplay().getHeight();
			scaleX = (float) frameBufferWidth / width;
			scaleY = (float) frameBufferHeight / height;
			soundPool = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);

			try {
				inputStream = assetManager.open("latestTank.png");
				imgTank = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("bullet.png");
				imgBullet = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("Enemy.png");
				enemy = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("Enemy1.png");
				enemy1 = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("Enemy2.png");
				enemy2 = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("sad.png");
				sad = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("happy.png");
				happy = BitmapFactory.decodeStream(inputStream);

				inputStream = assetManager.open("tank.png");
				imgTank1 = BitmapFactory.decodeStream(inputStream);

			} catch (IOException e) {
				e.printStackTrace();
				e.toString();
			}

			for (int i = 0; i < 12; i++) {
				try {
					inputStream = assetManager.open("e" + i + ".png");
				} catch (IOException e) {
					e.printStackTrace();
				}
				explosion[i] = BitmapFactory.decodeStream(inputStream);

			}

			try {
				descriptor = assetManager.openFd("shot.ogg");
				sound1 = soundPool.load(descriptor, 1);

				descriptor = assetManager.openFd("explosion.ogg");
				sound2 = soundPool.load(descriptor, 1);

				descriptor = assetManager.openFd("explosionLong.ogg");
				sound3 = soundPool.load(descriptor, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}

			dst.set(0, 0, width, height);

			font = Typeface.defaultFromStyle(0);
			mPaint = new Paint();
			paint = new Paint();
			paint.setTypeface(font);
			paint.setColor(Color.WHITE);
			paint.setTextSize(50);

			powerManager = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
					"my lock");
			wakeLock.acquire();

			manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

			if (manager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0)
				accelerometer = manager
						.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);

			manager.registerListener(this, accelerometer,
					SensorManager.SENSOR_DELAY_GAME);

			frameBuffer = Bitmap.createBitmap(frameBufferWidth,
					frameBufferHeight, Config.RGB_565);

			canvas = new Canvas(frameBuffer);
			w = canvas.getWidth();
			h = canvas.getHeight();

			rect1 = new Rect();
			rect2 = new Rect();
			rect3 = new Rect();
			rect4 = new Rect();

			// assetManager.close();

			numStars = frameBufferWidth * frameBufferHeight / 3000;
			stars = new Point[numStars];
			for (i = 0; i < numStars; i++) {
				// random XY Point
				stars[i] = new Point((int) (Math.random() * frameBufferWidth),
						(int) (Math.random() * frameBufferHeight));
			}

			highestScore = prefs.getInt("key1", 0);

		}

		public void pause() {

			running = false;
			while (true) {
				try {
					renderThread.join();
					break;
				} catch (InterruptedException e) {

				}

			}

		}

		public void resume() {

			running = true;
			renderThread = new Thread(this);
			renderThread.start();
		}

		@Override
		public void run() {

			while (running) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!holder.getSurface().isValid())
					continue;

				paint();

			}
		}

		public void paint() {

			// getInput();

			rect1.set(x1, y1 + 100, 300, 200);
			rect2.set(x2, y2, 50, 150);
			rect3.set(x3, y3, 75, 150);
			rect4.set(x1 + 100, y1, 100, 200);

			collision(rect1, rect2, rect3, rect4);

			canvas.drawColor(Color.BLACK);

			for (int a = 0; a < numStars; a++) {

				canvas.drawPoint(stars[a].x, stars[a].y, paint);

			}

			if (buEnCol == false) {
				if (fire == true) {
					if (y2 == 950)
						if (sound)
							soundPool.play(sound1, 1.0f, 1.0f, 0, 0, 1);
					canvas.drawBitmap(imgBullet, x2, y2 -= 50, null);
					if (y2 < 0) {
						y2 = 950;
						x2 = 900;
						fire = false;

					}
				}

				if (y3 == 0) {
					x3 = (int) (Math.random() * 500 + 150);
					which = (int) (Math.random() * 10) % 3;
				}
				if (taEnCol == false) {
					if (which == 0)
						canvas.drawBitmap(enemy, x3, y3 += vel, null);
					if (which == 1)
						canvas.drawBitmap(enemy1, x3, y3 += vel, null);
					if (which == 2)
						canvas.drawBitmap(enemy2, x3, y3 += vel, null);
				}
				if (y3 > 1200) {
					y3 = 0;

				}
			} else {
				if (i == 0) {
					if (sound)
						soundPool.play(sound2, 1.0f, 1.0f, 0, 0, 1);
					vel = vel + 2;
					score += 10;
				}
				if (i < 12) {
					canvas.drawBitmap(explosion[i], x3, y2 - 50, null);
					i++;

				} else {
					i = 0;
					y2 = 950;
					x2 = 900;
					y3 = 0;
					start = false;
					buEnCol = false;
					fire = false;
				}

			}

			if (taEnCol) {

				if (k == 0) {
					if (sound)
						soundPool.play(sound3, 1.0f, 1.0f, 0, 0, 1);
					x2 = 1300;
				}
				if (k < 12) {
					canvas.drawBitmap(explosion[k], x1 + 100, y1 - 10, null);
					canvas.drawBitmap(explosion[k], x1 + 10, y1, null);
					canvas.drawBitmap(explosion[k], x1 + 200, y1, null);
					k++;
				} else {

					canvas.drawText("YOUR SCORE:" + score, 235, 300, paint);
					canvas.drawText("HIGHEST SCORE: " + highestScore, 200, 400,
							paint);
//					canvas.drawText("GLOBAL HIGHEST: " + globalHighestScore,
//							190, 500, paint);
//					if (isScoredGlobalHighest) {
//						canvas.drawText("YOUR SCORE IS GLOBAL HIGHEST", 20,
//								600, paint);
//					} else {
//						canvas.drawText("BY: " + globalHighestScorer, 200, 600,
//								paint);
//					}
//					if (isScoredGlobalHighest) {
//						canvas.drawText("TOUCH TO SUBMIT YOUR SCORE", 30, 700,
//								paint);
//					} else {
						canvas.drawText("TOUCH TO RESTART", 200, 700, paint);

//					}
					x1 = 1300;
					if (score > highestScore && highest == false) {
						highestScore = score;
						Editor editor = prefs.edit();
						editor.putInt("key1", score);
						editor.commit();
						highest = true;

					}
					if (globalHighestScore.equals("N.A") == false) {
						if (internetAvailable == true
								&& score > Integer.parseInt(globalHighestScore
										.trim())) {
							isScoredGlobalHighest = true;

						}
					}

					if (!highest)
						canvas.drawBitmap(sad, 270, 800, null);
					else
						canvas.drawBitmap(happy, 270, 800, null);

					gameOver = true;
					if (gameOver && touch) {
						x1 = 250;
						y1 = 950;
						x2 = 1300;
						y2 = 950;
						x3 = 400;
						y3 = 0;
						fire = false;
						taEnCol = false;
						buEnCol = false;
						start = false;
						i = 0;
						vel = 8;
						score = 0;
						k = 0;
						touch = false;
						gameOver = false;
						highest = false;
					}
				}
			}

			else {
				/*
				 * if(fire) { canvas.drawBitmap(imgTank1, x1, 260, null); } else
				 * {
				 */
				canvas.drawText("SCORE:" + score, 300, 1200, paint);
				canvas.drawBitmap(imgTank, x1, 950, null);
				// }

			}

			mCanvas = holder.lockCanvas();
			mCanvas.drawBitmap(frameBuffer, null, dst, null);
			holder.unlockCanvasAndPost(mCanvas);

		}

		@SuppressWarnings("deprecation")
		public void getInput() {

			if (isScoredGlobalHighest) {
				onPause();
				showDialog(2);
				return;
			}

			switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				if (taEnCol) {
					start = true;
					break;
				}

			case MotionEvent.ACTION_UP:
				x = (int) (event.getX() * scaleX);
				y = (int) (event.getY() * scaleY);

				fire = true;
				if (y2 == 950)
					x2 = x1 + 125;

			}
			if (firstlaunch == 0) {
				onPause();
				showDialog(1);
			}

		}

		public void collision(Rect r1, Rect r2, Rect r3, Rect r4) {
			if (r3.left < r2.left + r2.right && r3.left + r3.right > r2.left
					&& r3.top < r2.top + r2.bottom
					&& r3.top + r3.bottom > r2.top)
				buEnCol = true;
			if (r3.left < r1.left + r1.right && r3.left + r3.right > r1.left
					&& r3.top < r1.top + r1.bottom
					&& r3.top + r3.bottom > r1.top)
				taEnCol = true;
			if (r3.left < r4.left + r4.right && r3.left + r3.right > r4.left
					&& r3.top < r4.top + r4.bottom
					&& r3.top + r3.bottom > r4.top)
				taEnCol = true;

		}

		@Override
		public boolean onTouch(View renderView, MotionEvent event) {
			this.event = event;

			if (gameOver && !isScoredGlobalHighest) {
				touch = true;
				return false;
			}

			getInput();
			return true;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.values[0] > 1) {
				if (x1 > 0)
					x1 -= 10;
			}

			if (event.values[0] < -1) {
				if (x1 < w - 300)
					x1 += 10;

			}

		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		wakeLock.release();
	}

	public class LoadHighestScore extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			StringBuilder url = new StringBuilder(
					"https://global-highest-score.appspot.com/_ah/api/highestscoreendpoint/v1/highestscore/1");
			HttpGet get = new HttpGet(url.toString());
			HttpResponse response = null;
			client = getThreadSafeClient();
			HttpEntity entity;
			try {
				response = client.execute(get);

				int status = response.getStatusLine().getStatusCode();
				if (status == 200) {
					entity = response.getEntity();
					String data = EntityUtils.toString(entity);
					JSONObject obj = new JSONObject(data);
					globalHighestScorer = obj.getString("name");
					globalHighestScore = obj.getString("score");

				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {

				client.getConnectionManager().shutdown();
			}

			return null;
		}

	}

	public class HighestScore extends AsyncTask<String, Integer, String> {

		ProgressDialog myPd_ring = null;

		@Override
		protected String doInBackground(String... userInput) {

			StringBuilder url = new StringBuilder(
					"https://global-highest-score.appspot.com/_ah/api/highestscoreendpoint/v1/highestscore");
			String jsonString = "{\"id\":1,\"name\":\"" + name
					+ "\",\"score\":\"" + score + "\"}";
			client = getThreadSafeClient();
			HttpResponse response = null;
			try {
				HttpPut httpPut = new HttpPut(url.toString());
				httpPut.addHeader("Accept", "application/json");

				httpPut.addHeader("Content-Type", "application/json");
				StringEntity entity = new StringEntity(jsonString, "UTF-8");

				entity.setContentType("application/json");
				httpPut.setEntity(entity);
				response = client.execute(httpPut);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 200) {

					globalHighestScorer = name;
					globalHighestScore = String.valueOf(score);
				}

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {

				client.getConnectionManager().shutdown();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (myPd_ring != null) {
				myPd_ring.dismiss();

			}
			onResume();

		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			myPd_ring = ProgressDialog.show(NewGame.this, "Please wait",
					"Posting your score..", true);
			myPd_ring.setCancelable(true);

		}

	}
	protected String[] bannedWords = { "Not Connected", "null", "null", "null",
			"null", "null", "null" };

}

